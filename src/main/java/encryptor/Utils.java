package encryptor;

import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.http.HttpResponse;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static org.apache.commons.text.CharacterPredicates.DIGITS;
import static org.apache.commons.text.CharacterPredicates.LETTERS;

public class Utils {

    public static RandomStringGenerator GENERATOR = new RandomStringGenerator.Builder()
                                                            .withinRange('0', 'z')
                                                            .filteredBy(LETTERS, DIGITS)
                                                            .build();

    public static byte[] readData(HttpResponse response) throws IOException {
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

    public static JSONObject decryptJson(byte[] encryptedData, long key) throws IOException, ParseException {
        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(encryptedData);
             ByteOutputStream byteOutputStream = new ByteOutputStream()) {
            Decryptor decryptor = new Decryptor(key);
            while (true) {
                byte[] buffer = new byte[8];
                int status = byteInputStream.read(buffer);
                if (status == -1) {
                    break;
                }
                byteOutputStream.write(Utils.longToBytes(decryptor.decrypt(Utils.bytesToLong(buffer))));
            }
            byte[] outputBuffer = Arrays.copyOf(byteOutputStream.getBytes(), byteOutputStream.getCount());

            JSONParser jsonParser = new JSONParser();
            return (JSONObject) jsonParser.parse(new String(outputBuffer).trim());
        }
    }

    public static byte[] encryptJson(JSONObject jsonObject, long key) throws IOException {
        Encryptor encryptor = new Encryptor(key);

        try (ByteArrayInputStream byteInputStream = new ByteArrayInputStream(jsonObject.toJSONString().getBytes());
             ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream()) {

            while (true) {
                byte[] buffer = new byte[8];
                int status = byteInputStream.read(buffer);
                if (status == -1) {
                    break;
                }

                long encodedChunk = encryptor.encrypt(Utils.bytesToLong(buffer));
                byteOutputStream.write(Utils.longToBytes(encodedChunk));
            }
            return byteOutputStream.toByteArray();
        }
    }

    public static long stringToLong(String key) {
        byte[] buffer = Arrays.copyOf(key.getBytes(), 8);
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getLong();
    }

    public static long bytesToLong(byte[] buffer) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return byteBuffer.getLong();
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(x);
        return buffer.array();
    }

    public static byte getBit(long number, int index) {
        return (byte) ((number >> index) & 1);
    }

    public static long transformKey(byte[] table, long block) {
        long result = 0;
        for (int i = 0; i < table.length; i++) {
            byte currentBit = getBit(block, table[i]);
            result |= (long)currentBit << i;
        }
        return result;
    }

    public static long leftShift(long block, int size, int position) {
        long rightPart = block >> (size - position);
        long leftPart = (block & ((long) 1 << (size - position)) - 1) << position;
        return leftPart | rightPart;
    }



}
