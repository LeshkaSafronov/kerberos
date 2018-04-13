package encryptor;

import java.io.*;
import java.util.List;

public class Decryptor extends Cryptor {
    private final long extendedKey;
    private final List<Long> keys;

    public Decryptor(long key) {
        extendedKey = extendKey(key);
        keys = generateKeys(extendedKey);
    }

    private long encryptionFunction(long block, long key) {
        long extendedBlock = transformKey(Tables.E, block);
        long blockChainB = extendedBlock ^ key;
        long result = 0;

        for (int i = 0; i < 8; i++) {
            long blockB = blockChainB & ((1 << 6) -1);
            int row = (getBit(blockB, 5) << 1) | getBit(blockB, 0);
            int column = ((int)blockB >> 1) & ((1 << 4) - 1);
            result |= Tables.S[i][row][column] << (i*4);
        }
        return transformKey(Tables.P, result);
    }

    public long decrypt(long block) {
        long initialPermutation = transformKey(Tables.IP, block);
        long leftPart = initialPermutation >>> 32;
        long rightPart = initialPermutation & (((long)1 << 32) -1);

        for (int i = 15; i >= 0; i--) {
            long backup_leftPart = leftPart;
            leftPart = rightPart ^ encryptionFunction(leftPart, keys.get(i));
            rightPart = backup_leftPart;
        }

        long result = (leftPart << 32) | rightPart;
        return transformKey(Tables.IP_1, result);
    }

    public static String decryptFilename = "2016-04-07-152225_decrypt.jpg";
    public static String encryptFilename = "2016-04-07-152225_encrypt.jpg";
    public static String key = "abcdefg";


    public static void main(String[] args) {
        long keyInt = Utils.stringToLong(key);
        Decryptor encryptor = new Decryptor(keyInt);

        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(encryptFilename));
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(decryptFilename))) {
            int offset = 0, bufferSize = 8;
            while (true) {
                byte[] buffer = new byte[bufferSize];
                int status = bufferedInputStream.read(buffer, offset, bufferSize);
                if (status == -1) {
                    break;
                }

                long encrypted = encryptor.decrypt(Utils.bytesToLong(buffer));
                bufferedOutputStream.write(Utils.longToBytes(encrypted));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
