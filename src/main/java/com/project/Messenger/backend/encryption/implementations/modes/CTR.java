package com.project.Messenger.backend.encryption.implementations.modes;

import com.project.Messenger.backend.encryption.interfaces.ICipher;
import com.project.Messenger.backend.encryption.interfaces.IModeCipher;
import com.project.Messenger.backend.encryption.utilities.AuxilaryFunctions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class CTR implements IModeCipher {

    @Override
    public byte[] encrypt(byte[] input, byte[] initializationVector, ICipher cipher, int blockSizeInBytes) {
        byte[] encryptedText = new byte[input.length];
        byte[] encryptedForXOR = initializationVector;
        int n = input.length / blockSizeInBytes;

        for (int i = 0; i < n; i++) {
            int shift = i * blockSizeInBytes;

            byte[] temp = Arrays.copyOfRange(input, shift, shift + blockSizeInBytes);
            byte[] encryptedBlock = cipher.encryptBlock(AuxilaryFunctions.xor(encryptedForXOR, temp));
            System.arraycopy(encryptedBlock, 0, encryptedText, shift, encryptedBlock.length);

            encryptedForXOR = encryptedBlock;
        }

        return encryptedText;
    }

    @Override
    public byte[] decrypt(byte[] input, byte[] initializationVector, ICipher cipher, int blockSizeInBytes) {
        byte[] decryptedText = new byte[input.length];

        IntStream.range(0, input.length / blockSizeInBytes).parallel().forEach(i -> {
            int shift = i * blockSizeInBytes;

            byte[] decryptedForXOR = (i == 0) ? initializationVector : Arrays.copyOfRange(input, shift - blockSizeInBytes, shift);
            byte[] temp = Arrays.copyOfRange(input, shift, shift + blockSizeInBytes);
            byte[] decryptedBlock = AuxilaryFunctions.xor(decryptedForXOR, cipher.decryptBlock(temp));

            System.arraycopy(decryptedBlock, 0, decryptedText, shift, decryptedBlock.length);
        });

        return decryptedText;
    }
}
