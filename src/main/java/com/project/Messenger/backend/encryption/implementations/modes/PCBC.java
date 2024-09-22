package com.project.Messenger.backend.encryption.implementations.modes;

import com.project.Messenger.backend.encryption.interfaces.ICipher;
import com.project.Messenger.backend.encryption.interfaces.IModeCipher;
import com.project.Messenger.backend.encryption.utilities.AuxilaryFunctions;

import java.util.Arrays;
import java.util.List;

public class PCBC implements IModeCipher {

    @Override
    public byte[] encrypt(byte[] input, byte[] initializationVector, ICipher cipher, int blockSizeInBytes){
        byte[] encryptedText = new byte[input.length];
        byte[] encryptedForXOR = initializationVector;
        int n = input.length / blockSizeInBytes;

        for (int i = 0; i < n; i++) {
            int shift = i * blockSizeInBytes;

            byte[] temp = Arrays.copyOfRange(input, shift, shift + blockSizeInBytes);
            byte[] encryptedBlock = cipher.encryptBlock(AuxilaryFunctions.xor(encryptedForXOR, temp));
            System.arraycopy(encryptedBlock, 0, encryptedText, shift, encryptedBlock.length);

            encryptedForXOR = AuxilaryFunctions.xor(temp, encryptedBlock);
        }

        return encryptedText;
    }

    @Override
    public byte[] decrypt(byte[] input, byte[] initializationVector, ICipher cipher, int blockSizeInBytes){
        byte[] decryptedText = new byte[input.length];
        byte[] decryptedForXOR = initializationVector;
        int n = input.length / blockSizeInBytes;

        for (int i = 0; i < n; i++) {
            int shift = i * blockSizeInBytes;

            byte[] temp = Arrays.copyOfRange(input, shift, shift + blockSizeInBytes);
            byte[] decryptedBlock = AuxilaryFunctions.xor(cipher.decryptBlock(temp), decryptedForXOR);
            System.arraycopy(decryptedBlock, 0, decryptedText, shift, decryptedBlock.length);

            decryptedForXOR = AuxilaryFunctions.xor(temp, decryptedBlock);
        }

        return decryptedText;
    }
}
