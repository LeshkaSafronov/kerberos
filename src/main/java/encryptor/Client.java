package encryptor;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    public static void main(String[]args) throws IOException {

        Socket socket = new Socket("localhost", 8080);
        try(PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("Hello");
        }

        try(PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println("Alexey");
        }
    }
}
