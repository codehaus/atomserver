package org.atomserver;

import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.spring.container.SpringComponentProviderFactory;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.File;
import static java.lang.String.format;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

@Component(AtomServer.BEAN_NAME)
public class AtomServer implements ApplicationContextAware {
    public static final String BEAN_NAME = "org.atomserver.AtomServer";

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
        int port = 8000;
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new RuntimeException(format("error opening HTTP server on port %d", port));
        }
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.createContext(
                "/", ContainerFactory.createContainer(HttpHandler.class, resourceConfig, factory));

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
            
            AtomServer server = AtomServer.create();
            server.start();

            // TODO: just a stupid way to keep the server open for now...  need to replace with real start/stop
            File lockfile = new File("/tmp/lockfile");
            lockfile.delete();
            while (!lockfile.exists()) {
                System.out.println("waiting for lockfile...");
                Thread.sleep(15000);
            }

            server.stop();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
