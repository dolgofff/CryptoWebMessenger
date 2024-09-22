package com.project.Messenger.backend.encryption.implementations.paddings;

public interface PaddingService {
     byte[] applyPadding(byte[] input, int blockSizeInBytes);

     byte[] removePadding(byte[] input);

    default byte[] expandInitialInput(byte[] input, int blockSizeInBytes) {
        byte[] paddedData;
        int remainder = input.length % blockSizeInBytes;
        int paddedSize = input.length + blockSizeInBytes;

        if (remainder == 0) {
            paddedData = new byte[paddedSize];
        } else {
            paddedData = new byte[paddedSize - remainder];
        }

        System.arraycopy(input, 0, paddedData, 0, input.length);

        return paddedData;
    }
}
