package com.project.Messenger.backend.encryption.implementations.paddings.impl;

import com.project.Messenger.backend.encryption.implementations.paddings.PaddingService;

import java.util.Arrays;
import java.util.Random;

public class ISO10126 implements PaddingService {
    Random random = new Random();

    @Override
    public byte[] applyPadding(byte[] input, int blockSizeInBytes) {
        byte[] result = expandInitialInput(input, blockSizeInBytes);

        int remainder = (blockSizeInBytes - input.length % blockSizeInBytes);
        byte[] additional = new byte[remainder];
        random.nextBytes(additional);

        System.arraycopy(additional, 0, result, input.length, additional.length);
        result[result.length - 1] = (byte) remainder;

        return result;
    }

    @Override
    public byte[] removePadding(byte[] input) {
        return Arrays.copyOf(input, input.length - (input[input.length - 1] & 0xFF));
    }
}
