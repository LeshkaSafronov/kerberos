package network;

import encryptor.Utils;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;

import static encryptor.Utils.*;

public class HttpClient {
    private static CloseableHttpClient client = HttpClientBuilder.create().build();
    private static String clientId = "zhenya";
    private static String clientKey = "1234567";

    private String clientTGSKey;
    private String clientSSKey;
    private JSONObject authSecondBlock;

    private HttpResponse sendRequest(String url, JSONObject jsonObject) throws IOException {
        HttpPost request = new HttpPost(url);

        BasicHttpEntity httpEntity = new BasicHttpEntity();
        httpEntity.setContent(new ByteArrayInputStream(Base64.encodeBase64(jsonObject.toJSONString().getBytes())));

        request.setEntity(httpEntity);
        return client.execute(request);
    }

    private HttpResponse sendRequestToAuthServer() throws IOException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("client_id", clientId);
        return sendRequest(String.format("http://%s", Settings.AS_HOST), jsonObject);
    }

    private JSONObject getResponseJson(HttpResponse response, long key) throws IOException, ParseException  {
        int responseAuthStatus = response.getStatusLine().getStatusCode();
        byte[] encryptedData = Base64.decodeBase64(readData(response));

        if (responseAuthStatus != 200) {
            throw new HttpResponseException(responseAuthStatus, new String(encryptedData));
        }

        return decryptJson(encryptedData, key);
    }


    private JSONObject generateAuthBlockJson() {
        JSONObject authBlockJson = new JSONObject();
        authBlockJson.put("client_id", clientId);
        authBlockJson.put("timestamp", Instant.now().getEpochSecond());
        return authBlockJson;
    }

    private HttpResponse sendRequestToTGSServer(JSONObject jsonAuthObject) throws IOException {
        clientTGSKey = jsonAuthObject.get("client_tgs_key").toString();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("auth_block",
                            Base64.encodeBase64String(
                                    encryptJson(
                                        generateAuthBlockJson(),
                                        Utils.stringToLong(clientTGSKey)
                                    )
                            )
        );
        jsonObject.put("tgt_object", jsonAuthObject.get("tgt_object"));
        jsonObject.put("ss_host", Settings.SS_HOST);
        return sendRequest(String.format("http://%s", Settings.TGS_HOST), jsonObject);
    }

    private HttpResponse sendRequestToSSServer(JSONObject jsonTGSObject) throws IOException {
        clientSSKey = jsonTGSObject.get("client_ss_key").toString();
        authSecondBlock = generateAuthBlockJson();

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("auth_block",
                Base64.encodeBase64String(
                        encryptJson(
                                authSecondBlock,
                                Utils.stringToLong(clientSSKey)
                        )
                )
        );
        jsonObject.put("tgs_object", jsonTGSObject.get("tgs_object"));
        return sendRequest(String.format("http://%s", Settings.SS_HOST), jsonObject);
    }

    private boolean checkSSServer(JSONObject jsonSSObject) {
        int timestampAuthSecondBlock = Integer.valueOf(authSecondBlock.get("timestamp").toString());
        int timestampSS = Integer.valueOf(jsonSSObject.get("timestamp").toString());
        return timestampSS - timestampAuthSecondBlock == 1;
    }


    public void start() throws IOException, ParseException, HttpException {
        System.out.println("Sending request to Auth Server");
        HttpResponse responseAuthServer = sendRequestToAuthServer();
        JSONObject jsonAuthObject = getResponseJson(responseAuthServer, stringToLong(clientKey));
        System.out.println("Response from AuthServer: " + jsonAuthObject.toJSONString());
        System.out.println();

        System.out.println("Sending request to TGS Server");
        HttpResponse responseTGSServer = sendRequestToTGSServer(jsonAuthObject);
        JSONObject jsonTGSObject = getResponseJson(responseTGSServer, stringToLong(clientTGSKey));
        System.out.println("Response from TGS Server: " + jsonTGSObject.toJSONString());
        System.out.println();

        System.out.println("Sending request to SS Server");
        HttpResponse responseSSServer = sendRequestToSSServer(jsonTGSObject);
        JSONObject jsonSSObject = getResponseJson(responseSSServer, stringToLong(clientSSKey));
        System.out.println("Response from SS Server: " + jsonTGSObject.toJSONString());
        System.out.println();

        if (!checkSSServer(jsonSSObject)) {
            throw new HttpException("Invalid SS server");
        }

        System.out.println("Private SS Key --> " + clientSSKey);
    }

    public static void main(String[] args) throws IOException, ParseException, HttpException {
        HttpClient httpClient = new HttpClient();
        httpClient.start();
    }
}