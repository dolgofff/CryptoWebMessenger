package com.project.Messenger.backend.encryption.implementations.modes;

import com.project.Messenger.backend.encryption.interfaces.ICipher;
import com.project.Messenger.backend.encryption.interfaces.IModeCipher;
import com.project.Messenger.backend.encryption.utilities.AuxilaryFunctions;

import java.util.Arrays;
import java.util.List;

public class OFB implements IModeCipher {

    @Override
    public byte[] encrypt(byte[] input, byte[] initializationVector, ICipher cipher, int blockSizeInBytes) {
        byte[] encryptedText = new byte[input.length];
        byte[] encryptedForXor = initializationVector;
        int n = input.length / blockSizeInBytes;

        for (int i = 0; i < n; i++) {
            int shift = i * blockSizeInBytes;

            byte[] temp = Arrays.copyOfRange(input, shift, shift + blockSizeInBytes);
            encryptedForXor = cipher.encryptBlock(encryptedForXor);
            byte[] encryptedBlock = AuxilaryFunctions.xor(temp, encryptedForXor);

            System.arraycopy(encryptedBlock, 0, encryptedText, shift, encryptedBlock.length);
        }

        return encryptedText;
    }

    @Override
    public byte[] decrypt(byte[] input, byte[] initializationVector, ICipher cipher, int blockSizeInBytes) {
        byte[] decryptedText = new byte[input.length];
        byte[] decryptedForXor = initializationVector;
        int n = input.length / blockSizeInBytes;

        for (int i = 0; i < n; i++) {
            int shift = i * blockSizeInBytes;

            byte[] temp = Arrays.copyOfRange(input, shift, shift + blockSizeInBytes);
            decryptedForXor = cipher.encryptBlock(decryptedForXor);
            byte[] decryptedBlock = AuxilaryFunctions.xor(temp, decryptedForXor);

            System.arraycopy(decryptedBlock, 0, decryptedText, shift, decryptedBlock.length);
        }

        return decryptedText;
    }
}
