package org.atomserver;

import com.sun.jersey.api.container.ContainerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;

import org.springframework.util.StringUtils;

public class Server {

    static List<Long> strings = new ArrayList<Long>();

    public static void main(String[] args) {
        synchronized (strings) {
            strings.add(System.currentTimeMillis());
        }
        String list = StringUtils.collectionToCommaDelimitedString(strings);
        System.out.println("list = " + list);
    }

    public static void runAtomServer() {
        try {
            HttpServer httpServer =
                    HttpServer.create(new InetSocketAddress("localhost", 8000), 0);

            httpServer.setExecutor(Executors.newCachedThreadPool());

            httpServer.createContext(
                    "/",
                    ContainerFactory.createContainer(
                            HttpHandler.class,
                            new PackagesResourceConfig("org.atomserver.rest")));

            httpServer.start();

            System.out.println("press return to stop");
            System.in.read();

            httpServer.stop(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
