package network;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.OutputStream;

public class Utils {
    public static void writeError(HttpExchange exchange, String message, int statusCode) throws IOException {
        try(OutputStream outputStream = exchange.getResponseBody()) {
            System.out.println("writeError!!!");
            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", "application/plain");

            byte[] responseBytes = Base64.encodeBase64(message.getBytes());
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            outputStream.write(responseBytes);
        }
    }
}
