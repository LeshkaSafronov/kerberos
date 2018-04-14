package network;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import encryptor.Decryptor;
import encryptor.Utils;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class HttpClient {
    private static CloseableHttpClient client = HttpClientBuilder.create().build();
    private static String clientId = "zhenya";
    private static String clientKey = "1234567";

    private byte[] readData(HttpResponse response) throws IOException {
        try(InputStream inputStream = response.getEntity().getContent();
            ByteOutputStream byteOutputStream = new ByteOutputStream()) {
            while (true) {
                byte[] buffer = new byte[8];
                int status = inputStream.read(buffer);
                if (status == -1) {
                    break;
                }
                byteOutputStream.write(buffer);
            }
            return byteOutputStream.getBytes();
        }
    }

    private HttpResponse sendRequestToAuthServer() throws IOException {
        HttpPost request = new HttpPost(String.format("http://%s", Settings.AS_HOST));

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("client_id", clientId);

        BasicHttpEntity httpEntity = new BasicHttpEntity();
        httpEntity.setContent(new ByteArrayInputStream(jsonObject.toJSONString().getBytes()));

        request.setEntity(httpEntity);
        return client.execute(request);
    }

    private void decryptAuthServerResponse(HttpResponse responseAuthServer) throws IOException {
        byte[] decodedBytes = Base64.decodeBase64(readData(responseAuthServer));
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(decodedBytes);
             ByteOutputStream byteOutputStream = new ByteOutputStream()) {
            Decryptor decryptor = new Decryptor(Utils.stringToLong(clientKey));
            while (true) {
                byte[] buffer = new byte[8];
                int status = byteInputStream.read(buffer);
                if (status == -1) {
                    break;
                }
                byteOutputStream.write(Utils.longToBytes(decryptor.decrypt(Utils.bytesToLong(buffer))));
            }
            System.out.println(new String(byteOutputStream.getBytes()));
        }
    }

    public void start() throws IOException {
        HttpResponse responseAuthServer = sendRequestToAuthServer();
        int responseStatus = responseAuthServer.getStatusLine().getStatusCode();

        if (responseStatus != 200) {
            throw new HttpResponseException(responseStatus, new String(readData(responseAuthServer)));
        }

        decryptAuthServerResponse(responseAuthServer);
    }

    public static void main(String[] args) throws IOException {
        HttpClient httpClient = new HttpClient();
        httpClient.start();

    }
}