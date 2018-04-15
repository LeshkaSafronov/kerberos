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

import static encryptor.Utils.*;
import static network.Utils.writeError;

public class SSServerHandler implements HttpHandler {
    private String clientSSKey;

    private JSONObject generateSSResponseObject(JSONObject authBlock) throws IOException {
        int timestamp = Integer.valueOf(authBlock.get("timestamp").toString());

        JSONObject ssResponseObject = new JSONObject();
        ssResponseObject.put("timestamp", timestamp + 1);
        return ssResponseObject;
    }

    private boolean checkRequest(JSONObject tgsObject, JSONObject authBlock) {
        int tgtTimestamp = Integer.valueOf(tgsObject.get("timestamp").toString());
        int authTimestamp = Integer.valueOf(authBlock.get("timestamp").toString());
        int ticketEstimate = Integer.valueOf(tgsObject.get("ticket_estimate").toString());

        return (tgsObject.get("client_id").toString().equals(authBlock.get("client_id").toString()) &&
                authTimestamp - tgtTimestamp <= ticketEstimate);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("GET")) {
            writeError(exchange, "Method GET not allowed", 400);
            return;
        }

        try(InputStreamReader inputStreamReader = new InputStreamReader(exchange.getRequestBody());
            OutputStream outputStream = exchange.getResponseBody()) {

            byte[] requestData = Base64.decodeBase64(IOUtils.toByteArray(inputStreamReader));
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(new String(requestData));

            JSONObject tgsObject = decryptJson(
                    Base64.decodeBase64(jsonObject.get("tgs_object").toString()),
                    stringToLong(Settings.TGS_SS_KEY)
            );

            clientSSKey = tgsObject.get("client_ss_key").toString();

            JSONObject authBlock = decryptJson(
                    Base64.decodeBase64(jsonObject.get("auth_block").toString()),
                    stringToLong(clientSSKey)
            );

            if (!checkRequest(tgsObject, authBlock)) {
                throw new HttpException("Permission denied");
            }

            System.out.println("Private SS Key --> " + clientSSKey);

            JSONObject ssResponseObject = generateSSResponseObject(authBlock);
            byte[] responseB64Data = Base64.encodeBase64(
                    encryptJson(
                            ssResponseObject,
                            stringToLong(clientSSKey)
                    )
            );

            Headers headers = exchange.getResponseHeaders();
            headers.add("Content-Type", "text/plain");

            exchange.sendResponseHeaders(200, responseB64Data.length);
            outputStream.write(responseB64Data);
        } catch (ParseException e) {
            writeError(exchange, "Internal Server Error", 500);
            e.printStackTrace();
        } catch (HttpException e) {
            writeError(exchange, e.toString(), 400);
            e.printStackTrace();
        }
    }
}
