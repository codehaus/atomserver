package org.atomserver;

import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.spring.container.SpringComponentProviderFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import static java.lang.String.format;
import java.net.*;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;

@Component(AtomServer.BEAN_NAME)
@ManagedResource(objectName = "bean:name=AtomServer")
public class AtomServer implements ApplicationContextAware {
    public static final String BEAN_NAME = "org.atomserver.AtomServer";
    public static final String APP_CONTEXT = "/app";
    public static final int PORT = 8000;
    public static final String HOSTNAME = computeHostname();
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
        return (AtomServer) new ClassPathXmlApplicationContext(
                "/org/atomserver/applicationContext.xml").getBean(BEAN_NAME);
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
