package com.project.Messenger.backend.encryption.interfaces;

public interface ICipher{
    byte[] encryptBlock(byte[] input);
    byte[] decryptBlock(byte[] output);
    int getBlockSizeInBytes();
}
