package encryptor;

import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        while (true) {
            try (Socket socket = serverSocket.accept();
                 PrintWriter printWriter = new PrintWriter(socket.getOutputStream())) {


                int buffer_size = 8, read_bytes;
                byte[] buffer = new byte[buffer_size];
                try(DataInputStream reader = new DataInputStream(socket.getInputStream())) {

                    while (true) {
                        read_bytes = reader.read(buffer);
                        System.out.println(read_bytes);
                        if (read_bytes == -1) {
                            break;
                        }
                    }
                }

                try (DataInputStream reader = new DataInputStream(socket.getInputStream())) {
                    read_bytes = reader.read(buffer);
                    System.out.println(read_bytes);
                    if (read_bytes == -1) {
                        break;
                    }
                }



                // System.out.println(Arrays.toString(buffer));
            }
        }
    }
}
