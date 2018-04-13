package encryptor;

import java.io.*;
import java.util.List;

public class Encryptor extends Cryptor {
    private final long extendedKey;
    private final List<Long> keys;

    public Encryptor(long key) {
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

    public long encrypt(long block) {
        long initialPermutation = transformKey(Tables.IP, block);
        long leftPart = initialPermutation >>> 32;
        long rightPart = initialPermutation & (((long)1 << 32) -1);


        for (int i = 0; i < 16; i++) {
            long backup_rightPart = rightPart;
            rightPart = leftPart ^ encryptionFunction(rightPart, keys.get(i));
            leftPart = backup_rightPart;
        }

        long result = (leftPart << 32) | rightPart;
        return transformKey(Tables.IP_1, result);
    }


    public static String filename = "2016-04-07-152225.jpg";
    public static String encryptFilename = "2016-04-07-152225_encrypt.jpg";
    public static String key = "abcdefg";


    public static void main(String[] args) {
        long keyInt = Utils.stringToLong(key);
        Encryptor encryptor = new Encryptor(keyInt);

        try(BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(filename));
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(encryptFilename))) {
            int offset = 0, bufferSize = 8;
            while (true) {
                byte[] buffer = new byte[bufferSize];
                int status = bufferedInputStream.read(buffer, offset, bufferSize);
                if (status == -1) {
                    break;
                }

                long encrypted = encryptor.encrypt(Utils.bytesToLong(buffer));
                bufferedOutputStream.write(Utils.longToBytes(encrypted));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}