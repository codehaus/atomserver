package org.atomserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.CountDownLatch;

public class AtomServer {
    public static final String APP_CONTEXT = "/";
    public static final int PORT = 8000;

    private Server server;

    public void start() {
        server = new Server(PORT);
        // TODO: src/main/webapp is no good outside dev env.
        server.addHandler(new WebAppContext("src/main/webapp", APP_CONTEXT));
        try {
            server.start();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }


    public static AtomServer create() {
        return new AtomServer();
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
}
