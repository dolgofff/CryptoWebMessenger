package com.project.Messenger.backend.encryption.implementations.paddings.impl;

import com.project.Messenger.backend.encryption.implementations.paddings.PaddingService;

import java.util.Arrays;

public class PKCS7 implements PaddingService {
    @Override
    public byte[] applyPadding(byte[] input, int blockSizeInBytes) {
        byte[] result = expandInitialInput(input, blockSizeInBytes);

        for (int i = input.length; i < result.length; i++) {
            result[i] = (byte) (blockSizeInBytes - input.length % blockSizeInBytes);
        }

        return result;
    }

    @Override
    public byte[] removePadding(byte[] input) {
        return Arrays.copyOf(input, input.length - (input[input.length - 1] & 0xFF));
    }
}
