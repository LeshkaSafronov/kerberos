package network;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;

import static encryptor.Utils.*;
import static network.Utils.writeError;

public class TGSServerHandler implements HttpHandler {
    private JSONObject generateTGSObject(String clientId, String clientSSKey) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("client_id", clientId);
        jsonObject.put("ss_host", Settings.SS_HOST);
        jsonObject.put("timestamp", Instant.now().getEpochSecond());
        jsonObject.put("client_ss_key", clientSSKey);
        return jsonObject;
    }

    private JSONObject generateTGSResponseObject(JSONObject tgtObject) throws IOException {
        String clientId = tgtObject.get("client_id").toString();
        String clientSSKey = GENERATOR.generate(7);

        JSONObject tgsResponseObject = new JSONObject();
        tgsResponseObject.put(
                "tgs_object",
                Base64.encodeBase64String(
                        encryptJson(
                                generateTGSObject(clientId, clientSSKey),
                                stringToLong(Settings.TGS_SS_KEY)
                        )
                )
        );
        tgsResponseObject.put("cliest_ss_key", clientSSKey);
        return tgsResponseObject;
    }

    private boolean checkRequest(JSONObject tgtObject, JSONObject authBlock) {
        int tgtTimestamp = Integer.valueOf(tgtObject.get("timestamp").toString());
        int authTimestamp = Integer.valueOf(authBlock.get("timestamp").toString());
        int ticketEstimate = Integer.valueOf(tgtObject.get("ticket_estimate").toString());

        return (tgtObject.get("client_id").toString().equals(authBlock.get("client_id").toString()) &&
                authTimestamp - tgtTimestamp <= ticketEstimate);
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
                    Settings.DB_PASSWORD)) {

            byte[] requestData = Base64.decodeBase64(IOUtils.toByteArray(inputStreamReader));

            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(new String(requestData));

            JSONObject tgtObject = decryptJson(
                    Base64.decodeBase64(jsonObject.get("tgt_object").toString()),
                    stringToLong(Settings.AS_TGS_KEY)
            );

            String clientTGSKey = tgtObject.get("client_tgs_key").toString();
            JSONObject authBlock = decryptJson(
                    Base64.decodeBase64(jsonObject.get("auth_block").toString()),
                    stringToLong(clientTGSKey)
            );

            if (!checkRequest(tgtObject, authBlock)) {
                throw new HttpException("Permission denied");
            }

            JSONObject tgsResponseObject = generateTGSResponseObject(tgtObject);
            byte[] responseB64Data = Base64.encodeBase64(
                    encryptJson(
                            tgsResponseObject,
                            stringToLong(clientTGSKey)
                    )
            );

            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", "text/plain");

            exchange.sendResponseHeaders(200, responseB64Data.length);
            outputStream.write(responseB64Data);
        } catch (ParseException | SQLException e) {
            writeError(exchange, "Internal Server Error", 500);
            e.printStackTrace();
        } catch (HttpException e) {
            writeError(exchange, e.toString(), 400);
            e.printStackTrace();
        }
    }
}
