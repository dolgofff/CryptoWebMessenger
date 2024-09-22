package com.project.Messenger.backend.encryption.implementations.modes;

import com.project.Messenger.backend.encryption.interfaces.ICipher;
import com.project.Messenger.backend.encryption.interfaces.IModeCipher;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class ECB implements IModeCipher {

    @Override
    public byte[] encrypt(byte[] input, byte[] initializationVector, ICipher cipher, int blockSizeInBytes) {
        byte[] encryptedInput = new byte[input.length];

        //Проходимся по всему тексту и распараллеливаем операции над стримом данных
        IntStream.range(0, input.length / blockSizeInBytes).parallel().forEach(i -> {
                    int shift = i * blockSizeInBytes;

                    byte[] temp = Arrays.copyOfRange(input, shift, shift + blockSizeInBytes);
                    byte[] encryptedBlock = cipher.encryptBlock(temp);

                    System.arraycopy(encryptedBlock, 0, encryptedInput, shift, encryptedBlock.length);
                });
        return encryptedInput;
    }

    @Override
    public byte[] decrypt(byte[] input, byte[] initializationVector, ICipher cipher, int blockSizeInBytes) {
        byte[] decryptedInput = new byte[input.length];
        IntStream.range(0, input.length / blockSizeInBytes).parallel().forEach(i -> {
                    int shift = i * blockSizeInBytes;

                    byte[] temp = Arrays.copyOfRange(input, shift, shift + blockSizeInBytes);
                    byte[] decryptedBlock = cipher.decryptBlock(temp);

                    System.arraycopy(decryptedBlock, 0, decryptedInput, shift, decryptedBlock.length);
                });
        return decryptedInput;
    }
}
