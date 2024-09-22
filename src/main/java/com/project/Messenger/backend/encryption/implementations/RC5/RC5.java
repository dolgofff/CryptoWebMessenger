package com.project.Messenger.backend.encryption.implementations.RC5;

import com.project.Messenger.backend.encryption.interfaces.ICipher;
import com.project.Messenger.backend.encryption.utilities.AuxilaryFunctions;
import org.apache.commons.lang3.tuple.Pair;

public class RC5 implements ICipher {
    private final int blockSize; //размер блока из 2х слов в битах 'w'
    private final int rounds; // количество раундов 'r'
    private final long[] S; // массив с раундовыми ключами

    public RC5(int blockSize, int rounds, byte[] key, int keySize) {

        if ((blockSize != 32) && (blockSize != 64) && (blockSize != 128)) {
            throw new IllegalArgumentException("#### (RC5) Invalid block size! (RC5) ####");
        }

        if ((rounds < 0) || (rounds > 255)) {
            throw new IllegalArgumentException("#### (RC5) Invalid rounds amount! (RC5) ####");
        }

        if ((keySize < 0) || (keySize > 255)) {
            throw new IllegalArgumentException("#### (RC5) Invalid key size! (RC5) ####");
        }

        this.blockSize = blockSize;
        this.rounds = rounds;
        this.S = new RC5KeyExpansion(blockSize, key, rounds, keySize).expandKey();
    }

    @Override
    public int getBlockSizeInBytes() {
        return blockSize / 8;
    }

    @Override
    public byte[] encryptBlock(byte[] text) {
        Pair<byte[], byte[]> dividedText = null;
        int len = text.length / 2;

        if (text != null) {
            byte[][] dividedPartsStorage = new byte[2][len];

            System.arraycopy(text, 0, dividedPartsStorage[0], 0, len);
            System.arraycopy(text, len, dividedPartsStorage[1], 0, len);

            dividedText = Pair.of(dividedPartsStorage[0], dividedPartsStorage[1]);
        }

        long a = AuxilaryFunctions.encryptionSum(AuxilaryFunctions.byteArrayToLongNumber(dividedText.getLeft()),
                S[0], blockSize / 2);
        long b = AuxilaryFunctions.encryptionSum(AuxilaryFunctions.byteArrayToLongNumber(dividedText.getRight()),
                S[1], blockSize / 2);

        for (int i = 1; i <= rounds; i++) {
            a = AuxilaryFunctions.encryptionSum(AuxilaryFunctions.cyclicShiftLeft((a ^ b), blockSize / 2, b),
                    S[2 * i], blockSize / 2);
            b = AuxilaryFunctions.encryptionSum(AuxilaryFunctions.cyclicShiftLeft((b ^ a), blockSize / 2, a),
                    S[2 * i + 1], blockSize / 2);
        }

        //Объединяем A и B
        byte[] firstHalf = new byte[len];
        byte[] secondHalf = new byte[len];
        byte[] mergedHalfs = new byte[len * 2];

        for (int i = 0; i < len; i++) {
            firstHalf[len - i - 1] = (byte) ((a >> (i * 8)) & ((1 << 8) - 1));
            secondHalf[len - i - 1] = (byte) ((b >> (i * 8)) & ((1 << 8) - 1));
        }

        System.arraycopy(firstHalf, 0, mergedHalfs, 0, len);
        System.arraycopy(secondHalf, 0, mergedHalfs, len, len);

        return mergedHalfs;
    }

    @Override
    public byte[] decryptBlock(byte[] text) {
        Pair<byte[], byte[]> dividedText = null;
        int len = text.length / 2;

        if (text != null) {
            byte[][] dividedPartsStorage = new byte[2][len];

            System.arraycopy(text, 0, dividedPartsStorage[0], 0, len);
            System.arraycopy(text, len, dividedPartsStorage[1], 0, len);

            dividedText = Pair.of(dividedPartsStorage[0], dividedPartsStorage[1]);
        }

        long a = AuxilaryFunctions.byteArrayToLongNumber(dividedText.getLeft());
        long b = AuxilaryFunctions.byteArrayToLongNumber(dividedText.getRight());

        for (int i = rounds; i >= 1; i--) {
            b = AuxilaryFunctions.cyclicShiftRight(AuxilaryFunctions.encryptionSub(b, S[2 * i + 1], blockSize / 2), blockSize / 2, a) ^ a;
            a = AuxilaryFunctions.cyclicShiftRight(AuxilaryFunctions.encryptionSub(a, S[2 * i], blockSize / 2), blockSize / 2, b) ^ b;
        }

        b = AuxilaryFunctions.encryptionSub(b, S[1], blockSize / 2);
        a = AuxilaryFunctions.encryptionSub(a, S[0], blockSize / 2);


        //Объединяем A и B
        byte[] firstHalf = new byte[len];
        byte[] secondHalf = new byte[len];
        byte[] mergedHalfs = new byte[len * 2];

        for (int i = 0; i < len; i++) {
            firstHalf[len - i - 1] = (byte) ((a >> (i * 8)) & ((1 << 8) - 1));
            secondHalf[len - i - 1] = (byte) ((b >> (i * 8)) & ((1 << 8) - 1));
        }

        System.arraycopy(firstHalf, 0, mergedHalfs, 0, len);
        System.arraycopy(secondHalf, 0, mergedHalfs, len, len);

        return mergedHalfs;
    }
}
