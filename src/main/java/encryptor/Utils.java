package encryptor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Utils {

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
