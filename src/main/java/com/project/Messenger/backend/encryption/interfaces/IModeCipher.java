package com.project.Messenger.backend.encryption.interfaces;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface IModeCipher {
    byte[] encrypt(byte[] input, byte[] initializationVector, ICipher cipher, int blockSizeInBytes);

    byte[] decrypt(byte[] input, byte[] initializationVector, ICipher cipher, int blockSizeInBytes);

}
