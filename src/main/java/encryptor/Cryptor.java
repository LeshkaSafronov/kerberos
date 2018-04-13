package encryptor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Cryptor {
    byte getBit(long number, int index) {
        return (byte) ((number >> index) & 1);
    }

    long transformKey(byte[] table, long block) {
        long result = 0;
        for (int i = 0; i < table.length; i++) {
            byte currentBit = getBit(block, table[i]);
            result |= (long)currentBit << i;
        }
        return result;
    }

    long leftShift(long block, int size, int position) {
        long rightPart = block >> (size - position);
        long leftPart = (block & ((long) 1 << (size - position)) - 1) << position;
        return leftPart | rightPart;
    }

    long extendKey(long key) {
        long extendedKey = 0;
        int currentBitPosition = 0, countBit = 0;
        for (int i = 0; i < 56; i++) {
            byte currentBit = getBit(key, i);
            extendedKey |= (long)currentBit << currentBitPosition++;
            countBit += currentBit;

            if ((i+1) % 7 == 0) {
                if (countBit % 2 == 0) {
                    extendedKey |= (long)1 << currentBitPosition;
                }
                currentBitPosition++;
                countBit = 0;
            }
        }
        return extendedKey;
    }

    List<Long> generateKeys(long extendedKey) {
        List<Long> list = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            long firstPart = transformKey(Tables.C, extendedKey);
            long secondPart = transformKey(Tables.D, extendedKey);
            long vector = leftShift((firstPart << 32) | secondPart, 64, Tables.shiftingCount[i]);
            list.add(transformKey(Tables.CD, vector));
            extendedKey = vector;
        }
        return list;
    }


}
