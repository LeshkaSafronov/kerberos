package network;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class KerberosServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 10);

        server.createContext("/auth", new AuthServerHandler());
        server.createContext("/tgs", new TGSServerHandler());

        server.setExecutor(null);
        server.start();
    }
}
