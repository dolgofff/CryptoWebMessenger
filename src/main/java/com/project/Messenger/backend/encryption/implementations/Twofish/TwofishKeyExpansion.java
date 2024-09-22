package com.project.Messenger.backend.encryption.implementations.Twofish;

import com.project.Messenger.backend.encryption.interfaces.IKeyExpansion;
import com.project.Messenger.backend.encryption.utilities.AuxilaryFunctions;

import static com.project.Messenger.backend.encryption.utilities.UtilitiesStorage.*;

public class TwofishKeyExpansion implements IKeyExpansion {
    private final byte[] inputKey;

    public TwofishKeyExpansion(byte[] inputKey) {
        this.inputKey = inputKey;
    }
    // private final int keySize; // размер ключа в битах 128/192/256

    @Override
    public long[] expandKey() {
        //Разбиваем массив байтов ключа на слова по 4 байта и преобразуем каждое слово в long.
        long[] keyWords = new long[inputKey.length / 4];
        for (int i = 0; i < keyWords.length; i++) {
            byte[] temp = new byte[4];
            System.arraycopy(inputKey, i * 4, temp, 0, 4);
            /**В каждом слове байты переставляются в обратном порядке */
            AuxilaryFunctions.reverseByteArray(temp);
            keyWords[i] = AuxilaryFunctions.byteArrayToLongNumber(temp);
        }

        // Полученные 2*k 4х-байтных слов разбиваются на два вектора 𝑀𝑒 и 𝑀𝑜
        long[] mEven = new long[keyWords.length / 2];
        long[] mOdd = new long[keyWords.length / 2];

        for (int i = 0; i < keyWords.length; i++) {
            if (i % 2 == 0) {
                mEven[i / 2] = keyWords[i];
            } else {
                mOdd[i / 2] = keyWords[i];
            }
        }

        long[] expandedKey = new long[40]; //2 * keyWords.length

        for (int i = 0; i < expandedKey.length / 2; i++) {
            long Ai = h(i * 2 * RHO, mEven);
            long Bi = AuxilaryFunctions.cyclicShiftLeft(h((i * 2 + 1) * RHO, mOdd), 32, 8);

            long K2i = AuxilaryFunctions.encryptionSum(Ai, Bi, 32) & MOD_2_32;
            long K2ip1 = AuxilaryFunctions.cyclicShiftLeft(AuxilaryFunctions.encryptionSum(Ai, 2 * Bi, 33) & MOD_2_32, 32, 9);

            expandedKey[2 * i] = K2i;
            expandedKey[2 * i + 1] = K2ip1;
        }
        return expandedKey;
    }


    private static long h(long numberX, long[] mass) {
        //разлагаем входное 32х-битное слово numberX на 4 отдельных байта
        long[] x = new long[4];
        for (int i = 0; i < 4; i++) {
            x[i] = (numberX >>> (8 * (3 - i))) & 0xFF;
        }

        int k = mass.length;


        //https://upload.wikimedia.org/wikipedia/ru/c/cb/Twofish_h_func.png
        for (int i = k - 1; i > -1; i--) {
            if (i == 3) {
                x[0] = q(x[0], t_q1) ^ mass[i];
                x[1] = q(x[1], t_q0) ^ mass[i];
                x[2] = q(x[2], t_q0) ^ mass[i];
                x[3] = q(x[3], t_q1) ^ mass[i];
            }
            if (i == 2) {
                x[0] = q(x[0], t_q1) ^ mass[i];
                x[1] = q(x[1], t_q1) ^ mass[i];
                x[2] = q(x[2], t_q0) ^ mass[i];
                x[3] = q(x[3], t_q0) ^ mass[i];
            }
            if (i == 1) {
                x[0] = q(x[0], t_q0) ^ mass[i];
                x[1] = q(x[1], t_q1) ^ mass[i];
                x[2] = q(x[2], t_q0) ^ mass[i];
                x[3] = q(x[3], t_q1) ^ mass[i];
            }
            if (i == 0) {
                x[0] = q(q(x[0], t_q0) ^ mass[i], t_q1);
                x[1] = q(q(x[1], t_q0) ^ mass[i], t_q0);
                x[2] = q(q(x[2], t_q1) ^ mass[i], t_q1);
                x[3] = q(q(x[3], t_q1) ^ mass[i], t_q0);
            }
        }

        long[] z = multiplyByMDS(x);

        return (z[0] & 0xFF) |
                ((z[1] & 0xFF) << 8) |
                ((z[2] & 0xFF) << 16) |
                ((z[3] & 0xFF) << 24);
    }



    public long[] generateSboxes() {
        int iter = inputKey.length / 8;
        long[][] vectorS = new long[iter][4];
        long[] result = new long[iter];

        for (int i = 0; i < iter; i++) {
            long[] s = new long[8];
            for (int j = 0; j < 8; j++) {
                s[j] = inputKey[j + (i * 8)];
            }
            vectorS[i] = multiplyByRS(s);
        }

        for(int k = 0; k < iter; k++){
            result[k] = (vectorS[k][0] & 0xFF) |
                    ((vectorS[k][1] & 0xFF) << 8) |
                    ((vectorS[k][2] & 0xFF) << 16) |
                    ((vectorS[k][3] & 0xFF) << 24);
        }
        return result;
    }

    private static long q(long x, long[][] permutationTable) {
        //разбиваем на 2 4х-битные половинки a0 и b0
        long[] temp = AuxilaryFunctions.splitByte(x);
        long a0 = temp[0];
        long b0 = temp[1];

        long a1 = a0 ^ b0;
        long b1 = a0 ^ (AuxilaryFunctions.cyclicShiftRight(b0, 4, 1)) ^ ((a0 << 3) % 16);

        long a2 = permutationTable[0][(int) a1];
        long b2 = permutationTable[1][(int) b1];

        long a3 = a2 ^ b2;
        long b3 = a2 ^ (AuxilaryFunctions.cyclicShiftRight(b2, 4, 1)) ^ ((a2 << 3) % 16);

        long a4 = permutationTable[2][(int) a3];
        long b4 = permutationTable[3][(int) b3];

        return (b4 << 4) | a4;
    }

    // Умножение массива на MDS матрицу
    private static long[] multiplyByMDS(long[] input) {
        long[] result = new long[4];
        for (int i = 0; i < 4; i++) {
            result[i] = 0;
            for (int j = 0; j < 4; j++) {
                byte mdsValue = AuxilaryFunctions.longToByte((MDS[i][j]));
                byte inputValue = AuxilaryFunctions.longToByte(input[j]);
                result[i] ^= galoisMultiply(mdsValue, inputValue);
            }
        }
        return result;
    }

    public static long[] multiplyByRS(long[] input) {
        long[] result = new long[RS.length];

        for (int i = 0; i < RS.length; i++) {
            result[i] = 0;
            for (int j = 0; j < RS[i].length; j++) {
                result[i] ^= galoisMultiply((byte)RS[i][j], (byte)input[j]);
            }
        }

        return result;
    }

    private static byte galoisMultiply(byte a, byte b) {
        byte p = 0; // Результат умножения
        byte carry; // Переменная для проверки переноса

        for (int i = 0; i < 8; i++) {
            if ((b & 1) != 0) { // Если младший бит b равен 1
                p ^= a; // Добавляем a к результату
            }
            carry = (byte) (a & 0x80); // Проверка старшего бита a
            a <<= 1; // Сдвиг a влево
            if (carry != 0) { // Если старший бит был 1
                a ^= IRREDUCIBLE_POLY; // Применяем модульное сокращение
            }
            b >>= 1; // Сдвиг b вправо
        }
        return p;
    }
}
