package com.project.Messenger.backend.encryption.utilities;

public class AuxilaryFunctions {

    public static void reverseByteArray(byte[] array) {
        if (array == null || array.length <= 1) {
            return; // Нечего переворачивать
        }

        int length = array.length;
        for (int i = 0; i < length / 2; i++) {
            byte temp = array[i];
            array[i] = array[length - 1 - i];
            array[length - 1 - i] = temp;
        }
    }

    public static long cyclicShiftLeft(long num, int bitLength, long shift) {
        long controlCycle = shift % bitLength;
        // Избегаем отрицательных сдвигов, поскольку это может привести к неожиданным результатам
        if (controlCycle < 0) {
            controlCycle += bitLength;
        }

        // Маска для извлечения битов, которые будут сдвинуты в начало
        long mask = (1L << controlCycle) - 1;
        // Извлекаем биты, которые будут сдвинуты в начало
        long extractedBits = (num >>> (bitLength - controlCycle)) & mask;
        // Основной сдвиг влево и добавление извлечённых битов
        return ((num << controlCycle) | extractedBits) & ((1L << bitLength) - 1);
    }

    public static long cyclicShiftRight(long num, int bitLength, long shift) {
        long controlCycle = shift % bitLength;
        // Избегаем отрицательных сдвигов, поскольку это может привести к неожиданным результатам
        if (controlCycle < 0) {
            controlCycle += bitLength;
        }

        // Маска для извлечения битов, которые будут сдвинуты в конец
        long mask = (1L << controlCycle) - 1;
        // Извлекаем биты, которые будут сдвинуты в конец
        long extractedBits = (num & mask) << (bitLength - controlCycle);
        // Основной сдвиг вправо и добавление извлечённых битов
        return (num >>> controlCycle) | extractedBits;
    }

    //массив байтов -> одно целое число
    public static long byteArrayToLongNumber(byte[] bytesValue) {
        long result = 0L;

        for (byte byteValue : bytesValue) {

            long longValue = singleUnitProcedure(byteValue);
            result = (result << 8) | longValue;
        }

        return result;
    }


    public static long singleUnitProcedure(byte value) {
        int sign = (value >> 7) & 1;
        long result = value & ((1 << 7) - 1);

        if (sign == 1) {
            result |= 1 << 7;
        }

        return result;
    }

    // Utility method for summarizing two numbers in encryption algorithms
    public static long encryptionSum(long a, long b, int bitLength) {
        long result = 0;
        long remainder = 0;

        for (int i = 0; i < bitLength; i++) {

            //Сумма с учётом переноса с прошлого разряда
            long temp = ((a >> i) & 1) ^ ((b >> i) & 1) ^ remainder;
            remainder = (((a >> i) & 1) + ((b >> i) & 1) + remainder) >> 1;

            result |= temp << i;
        }

        return result;
    }

    public static long encryptionSub(long first, long second, int numBits) {
        return encryptionSum(first, ~second + 1, numBits);
    }


    //Извлекает очередной набор битов из ключа и приводит их к long
    public static long getBits(byte[] bytes, int start, int bitsAmount) {
        byte[] result = new byte[(bitsAmount + 7) / 8];

        for (int i = 0; i < bitsAmount; i++) {
            //Если индекс текущего бита выходит за пределы исходного массива,
            //то устанавливается бит false (нулевой бит) в результирующий массив.
            if ((start + i) >= bytes.length * 8) {
                //mb i/bitsAmount
                setExactBit(result, i, false);
                //В противном случае, используется метод cutExactBit для извлечения
                //соответствующего бита из массива bytes и метод setExactBit для установки этого бита в массив result.
            } else {
                setExactBit(result, i, cutExactBit(bytes, start + i) == 1);
            }
        }

        return AuxilaryFunctions.byteArrayToLongNumber(result);
    }


    //Возвращает значение конкретного бита в массиве
    //находим нужный байт, в котором находится бит ->  вычисляем позицию нужного бита в этом байте -> извлекаем маской
    public static int cutExactBit(byte[] bytes, int index) {
        return (bytes[index / 8] >> (8 - index % 8 - 1)) & 1;
    }

    //Устанавливает значение конкретного бита в массиве
    public static void setExactBit(byte[] bytes, int index, boolean value) {
        if (value) {
            bytes[index / 8] |= (byte) (1 << (8 - index % 8 - 1));
        } else {
            bytes[index / 8] &= (byte) ~(1 << (8 - index % 8 - 1));
        }
    }

    // Функция для разделения байта на две переменные типа long по 4 бита
    public static long[] splitByte(long byteValue) {
        // Убедимся, что byteValue действительно содержит только 1 байт (8 бит)
        byteValue = byteValue & 0xFF;

        // Извлекаем старшие 4 бита
        long highBits = (byteValue >> 4) & 0xF; // * 00001111

        // Извлекаем младшие 4 бита
        long lowBits = byteValue & 0xF;

        long[] result = new long[2];
        result[0] = highBits;
        result[1] = lowBits;
        // Возвращаем результат в виде массива
        return result;
    }

    public static byte longToByte(long value) {
        return (byte) (value & 0xFF);
    }

    public static byte[] convertIntToByteArr(int value) {
        byte[] byteArray = new byte[4];
        for (int i = 3; i >= 0; i--) {
            byteArray[i] = (byte) (value & 0xFF);
            value >>>=8;
        }
        return byteArray;
    }
    public static byte[] convertLongArrayToByteArray(long[] longArray) {
        // Создаем массив byte[] длиной 4 байта на каждый элемент long
        byte[] byteArray = new byte[longArray.length * 4];

        // Проходим по каждому элементу long[]
        for (int i = 0; i < longArray.length; i++) {
            long word = longArray[i];

            // Извлекаем 4 байта из текущего элемента long и сохраняем их в byteArray
            byteArray[i * 4] = ((byte) ((word >> 24) & 0xFF));
            byteArray[i * 4 + 1] = (byte) ((word >> 16) & 0xFF);
            byteArray[i * 4 + 2] = (byte) ((word >> 8) & 0xFF);
            byteArray[i * 4 + 3] = (byte) (word & 0xFF);
        }

        return byteArray;
    }

    //for cipher modes
    public static byte[] xor(byte[] a, byte[] b) {
        int max = Integer.max(a.length, b.length);
        byte[] result = new byte[max];

        for (int i = 0; i < max; i++) {
            byte aByte = a.length - i - 1 >= 0 ? a[a.length - i - 1] : 0;
            byte bByte = b.length - i - 1 >= 0 ? b[b.length - i - 1] : 0;

            result[max - i - 1] = (byte) (aByte ^ bByte);
        }
        return result;
    }
}
