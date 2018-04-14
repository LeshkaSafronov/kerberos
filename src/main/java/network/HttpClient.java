package network;

import com.sun.xml.internal.rngom.parse.host.Base;
import encryptor.Utils;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;

import static encryptor.Utils.*;

public class HttpClient {
    private static CloseableHttpClient client = HttpClientBuilder.create().build();
    private static String clientId = "zhenya";
    private static String clientKey = "1234567";

    private HttpResponse sendRequest(String url, JSONObject jsonObject) throws IOException {
        HttpPost request = new HttpPost(url);

        BasicHttpEntity httpEntity = new BasicHttpEntity();
        httpEntity.setContent(new ByteArrayInputStream(Base64.encodeBase64(jsonObject.toJSONString().getBytes())));

        request.setEntity(httpEntity);
        return client.execute(request);
    }

    private HttpResponse sendRequestToAuthServer() throws IOException {
        HttpPost request = new HttpPost(String.format("http://%s", Settings.AS_HOST));
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("client_id", clientId);
        return sendRequest(String.format("http://%s", Settings.AS_HOST), jsonObject);
    }


    private JSONObject generateAuthBlockJson() {
        JSONObject authBlockJson = new JSONObject();
        authBlockJson.put("client_id", clientId);
        authBlockJson.put("timestamp", Instant.now().getEpochSecond());
        return authBlockJson;
    }

    private HttpResponse sendRequestToTGSServer(JSONObject jsonAuthObject) throws IOException {
        String clientTGSKey = jsonAuthObject.get("client_tgs_key").toString();

        System.out.println("clientTGSKey --> " + clientTGSKey);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("auth_block", encryptJson(generateAuthBlockJson(), Utils.stringToLong(clientTGSKey)));
        jsonObject.put("tgt_object", jsonAuthObject.get("tgt_object"));
        jsonObject.put("ss_host", Settings.SS_HOST);

        return sendRequest(String.format("http://%s", Settings.TGS_HOST), jsonObject);
    }



    public void start() throws IOException {
        HttpResponse responseAuthServer = sendRequestToAuthServer();
        int responseStatus = responseAuthServer.getStatusLine().getStatusCode();
        byte[] authEncryptedData = Base64.decodeBase64(readData(responseAuthServer));
        System.out.println(Arrays.toString(authEncryptedData));

        if (responseStatus != 200) {
            throw new HttpResponseException(responseStatus, new String(authEncryptedData));
        }

        JSONObject jsonAuthObject;
        try {
            jsonAuthObject = decryptJson(authEncryptedData, stringToLong(clientKey));
        }
        catch (ParseException e) {
            throw new HttpResponseException(400, String.format("Cannot parse json string. Reason: %s", e.toString()));
        }

        sendRequestToTGSServer(jsonAuthObject);

        System.out.println(jsonAuthObject.toJSONString());
    }

    public static void main(String[] args) throws IOException {
        HttpClient httpClient = new HttpClient();
        httpClient.start();

    }
}