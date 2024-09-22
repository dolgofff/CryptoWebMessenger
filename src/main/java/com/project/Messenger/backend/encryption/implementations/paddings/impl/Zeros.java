package com.project.Messenger.backend.encryption.implementations.paddings.impl;

import com.project.Messenger.backend.encryption.implementations.paddings.PaddingService;

import java.util.Arrays;

public class Zeros implements PaddingService {
    @Override
    public byte[] applyPadding(byte[] input, int blockSizeInBytes) {
        return expandInitialInput(input, blockSizeInBytes);
    }

    @Override
    public byte[] removePadding(byte[] input) {
        int startFrom = 0;

        for (int i = input.length - 1; i >= 0; i--) {
            if(input[i] != 0){
                startFrom = i;
                break;
            }
        }

        return Arrays.copyOf(input, startFrom + 1);
    }
}
