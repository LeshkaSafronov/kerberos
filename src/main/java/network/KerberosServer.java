package network;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import encryptor.Encryptor;
import encryptor.Utils;
import javafx.util.Pair;
import org.apache.commons.text.RandomStringGenerator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.time.Instant;

import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;

public class KerberosServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 10);

        server.createContext("/auth", new AuthHandler());

        server.setExecutor(null);
        server.start();
    }

    static class AuthHandler implements HttpHandler {

        private byte[] getEncryptedTGTResponse(String clientId) throws SQLException, IOException {
            RandomStringGenerator generator = new RandomStringGenerator.Builder()
                    .withinRange('0', 'z')
                    .filteredBy(LETTERS, DIGITS)
                    .build();

            JSONObject jsonTGTObject = new JSONObject();
            jsonTGTObject.put("client_id", clientId);
            jsonTGTObject.put("tgt_host", Settings.TGT_HOST);
            jsonTGTObject.put("timestamp", Instant.now().getEpochSecond());
            jsonTGTObject.put("ticket_estimate", Settings.TICKET_ESTIMATE_SECONDS);
            jsonTGTObject.put("client_tgs_key", generator.generate(7));

            Encryptor encryptor = new Encryptor(Utils.stringToLong(Settings.AS_TGS_KEY));

            ByteArrayInputStream byteStream = new ByteArrayInputStream(jsonTGTObject.toJSONString().getBytes());
            ByteArrayOutputStream encodedTGTOutputStream = new ByteArrayOutputStream();

            while (true) {
                byte[] buffer = new byte[8];
                int status = byteStream.read(buffer);
                if (status == -1) {
                    break;
                }

                long encodedChunk = encryptor.encrypt(Utils.bytesToLong(buffer));
                encodedTGTOutputStream.write(Utils.longToBytes(encodedChunk));
            }

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("tgt_object", encodedTGTOutputStream.toByteArray());
            return jsonObject.toJSONString().getBytes();

        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try(InputStreamReader inputStreamReader = new InputStreamReader(exchange.getRequestBody());
                OutputStream outputStream = exchange.getResponseBody();
                Connection connection = DriverManager.getConnection(Settings.DB_HOST,
                                                                    Settings.DB_USERNAME,
                                                                    Settings.DB_PASSWORD);
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "select client_id, client_key from users where client_id = ?"
                )) {

                byte[] responseBytes;
                int statusCode;

                JSONParser jsonParser = new JSONParser();
                JSONObject jsonObject = (JSONObject) jsonParser.parse(inputStreamReader);

                String clientId = (String) jsonObject.get("client_id");
                preparedStatement.setString(1, clientId);

                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    responseBytes = getEncryptedTGTResponse(clientId);// jsonObject.toJSONString().getBytes();
                    statusCode = 200;
                } else {
                    JSONObject errorJson = new JSONObject();
                    errorJson.put("message", String.format("Client with supplied id %s does not exists", clientId));
                    responseBytes = errorJson.toJSONString().getBytes();
                    statusCode = 400;
                }

                Headers headers = exchange.getResponseHeaders();
                headers.add("Content-Type", "application/json");

                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                outputStream.write(responseBytes);

            } catch (ParseException | SQLException e) {
                try(OutputStream outputStream = exchange.getResponseBody()) {
                    Headers headers = exchange.getResponseHeaders();
                    headers.add("Content-Type", "application/plain");

                    byte[] responseBytes = "Internal Server Error".getBytes();
                    exchange.sendResponseHeaders(500, responseBytes.length);
                    outputStream.write(responseBytes);
                }
                e.printStackTrace();
            }
        }
    }
}
