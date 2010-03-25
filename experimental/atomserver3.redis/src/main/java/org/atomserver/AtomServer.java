package org.atomserver;

import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.spring.container.SpringComponentProviderFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.atomserver.app.ServiceStateSynchronizationFilter;
import org.atomserver.core.RedisSubstrate;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

@Component(AtomServer.BEAN_NAME)
@ManagedResource(objectName = "bean:name=AtomServer")
public class AtomServer implements ApplicationContextAware {
    public static final String BEAN_NAME = "org.atomserver.AtomServer";
    public static final String APP_CONTEXT = "/app";
    public static final int PORT = 8000;
    public static final String HOSTNAME = computeHostname();
    private static final Pattern CONFIG_URI_PATTERN =
//            Pattern.compile("atomserver:([a-zA-Z_]\\w*(\\.[a-zA-Z_]\\w*))(.*)");
            Pattern.compile("atomserver:([a-zA-Z_]\\w*(?:\\.[a-zA-Z_]\\w*)*)((?:;[a-zA-Z_]\\w*=[^;]*)*)");

    private static String computeHostname() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            hostname = "localhost";
        }
        return System.getProperty("org.atomserver.HOSTNAME", hostname);
    }


    private HttpServer httpServer;
    private ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void start() {
        ResourceConfig resourceConfig = new PackagesResourceConfig("org.atomserver.app");
        resourceConfig.setPropertiesAndFeatures(
                Collections.<String, Object>singletonMap(
                        ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS,
                        ServiceStateSynchronizationFilter.class.getName()));
        SpringComponentProviderFactory factory =
                new SpringComponentProviderFactory(
                        resourceConfig,
                        (ConfigurableApplicationContext) applicationContext);
        int port = PORT;
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new RuntimeException(format("error opening HTTP server on port %d", port));
        }
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.createContext(
                APP_CONTEXT,
                ContainerFactory.createContainer(HttpHandler.class, resourceConfig, factory));

        httpServer.start();
    }

    public void stop() {
        httpServer.stop(2);
    }


    public static AtomServer create() {

        Matcher matcher = CONFIG_URI_PATTERN.matcher(System.getProperty("org.atomserver.core.Substrate"));
        if (!matcher.matches()) {
            throw new IllegalStateException("illegal config URI!");
        }
        String className = matcher.group(1);
        String[] parts = matcher.group(2).split(";");


//        String[] parts = configUri.getSchemeSpecificPart().split("[;]");
//        String className = parts[0];
        Map<String, String> propMap = new HashMap<String, String>();
        for (int i = 1; i < parts.length; i++) {
            String[] nameAndValue = parts[i].split("=", 2);
            if (nameAndValue.length  > 1) {
                try {
                    String name = URLDecoder.decode(nameAndValue[0], "UTF-8");
                    String value = URLDecoder.decode(nameAndValue[1], "UTF-8");
                    propMap.put(name, value);
                    System.out.println("BKJ::name = " + name);
                    System.out.println("BKJ::value = " + value);
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalStateException(e); // TODO: handle
                }
            }
        }
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e); // TODO: handle
        }


        GenericApplicationContext context = new GenericApplicationContext();
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(context);
        xmlReader.loadBeanDefinitions(
                new ClassPathResource("/org/atomserver/applicationContext.xml"));
        context.registerBeanDefinition("substrate",
                new RootBeanDefinition(clazz, new MutablePropertyValues(propMap)));
        context.refresh();


//        ClassPathXmlApplicationContext context =
//                new ClassPathXmlApplicationContext(
//                        new String[]{"/org/atomserver/applicationContext.xml"}, false);


        return (AtomServer) context.getBean(BEAN_NAME);
    }

    public static void main(String[] args) {
        try {

            final AtomServer server = AtomServer.create();
            server.start();

            final CountDownLatch latch = new CountDownLatch(1);

            // TODO: re-do this
            // This is basically a low-rent way to "stop" the server.  I'm trying to figure out
            // what is the best way to STOP a "server" process cleanly - I'm thinking that the
            // answer is JMX, which is what I will move to next, but that takes a little more
            // work than this, so I'm keeping it simple until I come up with a definitely better
            // solution
            HttpServer manage = HttpServer.create(new InetSocketAddress(8042), 0);
            manage.createContext(
                    "/",
                    new HttpHandler() {
                        public void handle(HttpExchange httpExchange) throws IOException {
                            httpExchange.getResponseHeaders().set("Content-Type", "text/plain");
                            httpExchange.sendResponseHeaders(200, 0L);
                            httpExchange.getResponseBody().write("stopping...".getBytes());
                            httpExchange.getResponseBody().flush();
                            httpExchange.close();
                            latch.countDown();
                        }
                    });

            manage.start();
            latch.await();
            System.out.println("stopping AtomServer...");
            server.stop();
            System.out.println("stopping management thread...");
            manage.stop(2);

            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static URI getRootUri() throws URISyntaxException {
        return URI.create(String.format("http://%s:%s", HOSTNAME, PORT));
    }

    public static URI getRootAppUri() {
        return URI.create(String.format("http://%s:%s%s", HOSTNAME, PORT, APP_CONTEXT));
    }
}
