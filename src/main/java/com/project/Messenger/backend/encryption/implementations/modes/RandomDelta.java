package com.project.Messenger.backend.encryption.implementations.modes;

import com.project.Messenger.backend.encryption.interfaces.ICipher;
import com.project.Messenger.backend.encryption.interfaces.IModeCipher;
import com.project.Messenger.backend.encryption.utilities.AuxilaryFunctions;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class RandomDelta implements IModeCipher {

    @Override
    public byte[] encrypt(byte[] input, byte[] initializationVector, ICipher cipher, int blockSizeInBytes) {
        byte[] encryptedText = new byte[input.length];

        //Из второй половины вектора инициализации создаем объект Big Int,
        //Это значение определяет случайное смещение для каждого блока
        BigInteger delta = new BigInteger(Arrays.copyOfRange(initializationVector,
                initializationVector.length / 2, initializationVector.length));
        //Полный вектор инициализации конвертируем в объект Big Int как стартовое значение
        BigInteger initial = new BigInteger(initializationVector);

        IntStream.range(0, input.length / blockSizeInBytes).parallel().forEach(ctr -> {
            int shift = ctr * blockSizeInBytes;

            byte[] temp = Arrays.copyOfRange(input, shift, shift + blockSizeInBytes);
            BigInteger initialPlusDelta = initial.add(delta.multiply(BigInteger.valueOf(ctr)));
            byte[] encryptedBlock = cipher.encryptBlock(AuxilaryFunctions.xor(temp, initialPlusDelta.toByteArray()));

            System.arraycopy(encryptedBlock, 0, encryptedText, shift, encryptedBlock.length);
        });

        return encryptedText;
    }

    @Override
    public byte[] decrypt(byte[] input, byte[] initializationVector, ICipher cipher, int blockSizeInBytes) {
        byte[] decryptedText = new byte[input.length];

        BigInteger delta = new BigInteger(Arrays.copyOfRange(initializationVector,
                initializationVector.length / 2, initializationVector.length));
        BigInteger initial = new BigInteger(initializationVector);

        IntStream.range(0, input.length / blockSizeInBytes).parallel().forEach(ctr -> {
            int shift = ctr * blockSizeInBytes;

            byte[] temp = Arrays.copyOfRange(input, shift, shift + blockSizeInBytes);
            BigInteger initialPlusDelta = initial.add(delta.multiply(BigInteger.valueOf(ctr)));
            byte[] decryptedBlock = AuxilaryFunctions.xor(cipher.decryptBlock(temp), initialPlusDelta.toByteArray());

            System.arraycopy(decryptedBlock, 0, decryptedText, shift, decryptedBlock.length);
        });

        return decryptedText;
    }
}
