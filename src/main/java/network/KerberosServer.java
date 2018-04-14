package network;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import static encryptor.Utils.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;
import java.time.Instant;
import java.util.Arrays;

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

        private void writeError(HttpExchange exchange, String message, int statusCode) throws IOException {
            try(OutputStream outputStream = exchange.getResponseBody()) {
                Headers headers = exchange.getResponseHeaders();
                headers.add("Content-Type", "application/plain");

                byte[] responseBytes = Base64.encodeBase64(message.getBytes());
                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                outputStream.write(responseBytes);
            }
        }

        private JSONObject generateTGTObject(String clientId, String clientTGSKey) {
            JSONObject jsonTGTObject = new JSONObject();
            jsonTGTObject.put("client_id", clientId);
            jsonTGTObject.put("tgs_host", Settings.TGS_HOST);
            jsonTGTObject.put("timestamp", Instant.now().getEpochSecond());
            jsonTGTObject.put("ticket_estimate", Settings.TICKET_ESTIMATE_SECONDS);
            jsonTGTObject.put("client_tgs_key", clientTGSKey);
            return jsonTGTObject;
        }

        private byte[] getEncryptedTGTJsonString(String clientId, String clientKey) throws IOException {
            RandomStringGenerator generator = new RandomStringGenerator.Builder()
                    .withinRange('0', 'z')
                    .filteredBy(LETTERS, DIGITS)
                    .build();

            String clientTGSKey = generator.generate(7);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("tgt_object",
                    Base64.encodeBase64String(
                            encryptJson(
                                    generateTGTObject(clientId, clientTGSKey),
                                    stringToLong(Settings.AS_TGS_KEY)
                            )
                    )
            );
            jsonObject.put("client_tgs_key", clientTGSKey);
            return encryptJson(jsonObject, stringToLong(clientKey));
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equals("GET")) {
                writeError(exchange, "Method GET not allowed", 400);
                return;
            }

            try(InputStreamReader inputStreamReader = new InputStreamReader(exchange.getRequestBody());
                OutputStream outputStream = exchange.getResponseBody();
                Connection connection = DriverManager.getConnection(Settings.DB_HOST,
                                                                    Settings.DB_USERNAME,
                                                                    Settings.DB_PASSWORD);
                PreparedStatement preparedStatement = connection.prepareStatement(
                        "select client_id, client_key from users where client_id = ?"
                )) {

                byte[] requestData = Base64.decodeBase64(IOUtils.toByteArray(inputStreamReader));
                byte[] responseBytes;
                int statusCode;

                JSONParser jsonParser = new JSONParser();
                JSONObject jsonObject = (JSONObject) jsonParser.parse(new String(requestData));

                String clientId = (String) jsonObject.get("client_id");
                preparedStatement.setString(1, clientId);

                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    String clientKey = resultSet.getString(2);
                    responseBytes = getEncryptedTGTJsonString(clientId, clientKey);
                    statusCode = 200;
                } else {
                    JSONObject errorJson = new JSONObject();
                    errorJson.put("message", String.format("Client with supplied id %s does not exists", clientId));
                    responseBytes = errorJson.toJSONString().getBytes();
                    statusCode = 400;
                }

                System.out.println(Arrays.toString(responseBytes));
                byte[] responseB64Data = Base64.encodeBase64(responseBytes);
                System.out.println(Arrays.toString(responseB64Data));


                Headers headers = exchange.getResponseHeaders();
                headers.add("Content-Type", "text/plain");

                exchange.sendResponseHeaders(statusCode, responseB64Data.length);
                outputStream.write(responseB64Data);
            } catch (ParseException | SQLException e) {
                writeError(exchange, "Internal Server Error", 500);
                e.printStackTrace();
            }
        }
    }
}
