package com.project.Messenger.backend.encryption.implementations.Twofish;


import com.project.Messenger.backend.encryption.interfaces.ICipher;
import com.project.Messenger.backend.encryption.utilities.AuxilaryFunctions;

import static com.project.Messenger.backend.encryption.utilities.UtilitiesStorage.*;

//TODO: ПЕРЕНЕСТИ В ОДИН КЛАСС
public class Twofish implements ICipher {
    private final int wordSize = 128;   // длина блока в битах
    private final long[] roundKeys;

    private final long[] sBoxes;

    public Twofish(int keySize, byte[] inputKey) {
        if ((keySize != 128) && (keySize != 192) && (keySize != 256)) {
            throw new IllegalArgumentException("#### (Twofish) Invalid key size! (Twofish) ####");
        }
        TwofishKeyExpansion expansion = new TwofishKeyExpansion(inputKey);
        this.roundKeys = expansion.expandKey();
        this.sBoxes = expansion.generateSboxes();
    }

    @Override
    public byte[] encryptBlock(byte[] text) {
        //Разбиваем блок на слова по 4 байта и приводим к long
        long[] unitedWords = new long[text.length / 4];
        for (int i = 0; i < unitedWords.length; i++) {
            byte[] temp = new byte[4];
            System.arraycopy(text, i * 4, temp, 0, 4);
            unitedWords[i] = AuxilaryFunctions.byteArrayToLongNumber(temp);
        }

        //Входное отбеливание -> R0,R1,R2,R3
        for (int i = 0; i < 4; i++) {
            unitedWords[i] ^= roundKeys[i];
        }

        //Сеть Фейстеля
        for (int i = 0; i < 16; i++) {
            //T0 И T1 -> PHT
            long[] phtResults = pht(g(unitedWords[0], sBoxes), g(AuxilaryFunctions.cyclicShiftLeft(unitedWords[1], 32, 8), sBoxes));

            long F0 = AuxilaryFunctions.encryptionSum(phtResults[0], roundKeys[2 * i + 8], 32);
            long F1 = AuxilaryFunctions.encryptionSum(phtResults[1], roundKeys[2 * i + 9], 32);


            long C2 = AuxilaryFunctions.cyclicShiftRight(F0 ^ unitedWords[2], 32, 1);
            long C3 = F1 ^ AuxilaryFunctions.cyclicShiftLeft(unitedWords[3], 32, 1);

            unitedWords[2] = unitedWords[0];
            unitedWords[3] = unitedWords[1];
            unitedWords[0] = C2;
            unitedWords[1] = C3;
        }

        //Выходное отбеливание
        for (int i = 0; i < 4; i++) {
            unitedWords[i] ^= roundKeys[i + 4];
        }


        return AuxilaryFunctions.convertLongArrayToByteArray(unitedWords);
    }

    @Override
    public byte[] decryptBlock(byte[] text) {
        //Разбиваем блок на слова по 4 байта и приводим к long
        long[] unitedWords = new long[text.length / 4];
        for (int i = 0; i < unitedWords.length; i++) {
            byte[] temp = new byte[4];
            System.arraycopy(text, i * 4, temp, 0, 4);
            unitedWords[i] = AuxilaryFunctions.byteArrayToLongNumber(temp);
        }

        //Отмена выходного отбеливания
        for (int i = 0; i < 4; i++) {
            unitedWords[i] ^= roundKeys[i + 4];
        }

        // Обратная сеть Фейстеля
        for (int i = 15; i >= 0; i--) {
            long C2 = unitedWords[0];
            long C3 = unitedWords[1];

           /* long F0 = AuxilaryFunctions.encryptionSum(C2, roundKeys[2 * i + 8], 32);
            long F1 = AuxilaryFunctions.encryptionSum(C3, roundKeys[2 * i + 9], 32);

            long[] phtResults = pht(F0, F1);
            long T0 = g(AuxilaryFunctions.cyclicShiftRight(phtResults[0], 32, 8), sBoxes);
            long T1 = g(phtResults[1], sBoxes);*/
            long[] phtResults =  pht(g(unitedWords[2], sBoxes), g(AuxilaryFunctions.cyclicShiftLeft(unitedWords[3], 32, 8), sBoxes));

            long F0 = AuxilaryFunctions.encryptionSum(phtResults[0], roundKeys[2 * i + 8], 32);
            long F1 = AuxilaryFunctions.encryptionSum(phtResults[1], roundKeys[2 * i + 9], 32);

            long ex1 = F0 ^ AuxilaryFunctions.cyclicShiftLeft(C2,32,1);
            long ex2 = AuxilaryFunctions.cyclicShiftRight(C3 ^ F1,32,1);

            unitedWords[0] = unitedWords[2];
            unitedWords[1] = unitedWords[3];
            unitedWords[2] = ex1;
            unitedWords[3] = ex2;
        }

        // Входное отбеливание
        for (int i = 0; i < 4; i++) {
            unitedWords[i] ^= roundKeys[i];
        }

        return AuxilaryFunctions.convertLongArrayToByteArray(unitedWords);
    }


    @Override
    public int getBlockSizeInBytes() {
        return 0;
    }


    public static long[] pht(long a, long b) {
        long aPrime = AuxilaryFunctions.encryptionSum(a, b, 32) & 0xFFFFFFFFL;  // Первая часть преобразования
        long bPrime = AuxilaryFunctions.encryptionSum(a, b, 33) & 0xFFFFFFFFL; // Вторая часть преобразования
        return new long[]{aPrime, bPrime};
    }

    private static long g(long numberX, long[] mass) {
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
