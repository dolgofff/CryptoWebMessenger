package com.project.Messenger.backend.encryption.implementations.RC5;

import com.project.Messenger.backend.encryption.interfaces.IKeyExpansion;
import com.project.Messenger.backend.encryption.utilities.AuxilaryFunctions;
import lombok.AllArgsConstructor;

import static com.project.Messenger.backend.encryption.utilities.UtilitiesStorage.RC5_PARAMETERS;


public class RC5KeyExpansion implements IKeyExpansion {
    private final int blockSize; // размер блока из 2х слов в битах 'w'
    private final byte[] inputKey; // исходный ключ (слово)
    private final int rounds; // количество раундов 'r'

    private final int keySize; // размер ключа в байтах 'b'

    public RC5KeyExpansion(int blockSize, byte[] inputKey, int rounds, int keySize) {
        this.blockSize = blockSize;
        this.inputKey = inputKey;
        this.rounds = rounds;
        this.keySize = keySize;
    }

    @Override
    public long[] expandKey() {

        //блок состоит из двух слов, len - длина одного в БАЙТАХ
        int len = (blockSize / 8) / 2;

        //L_0 ... L_(c-1), c = b/u, u = w/8
        //Если b не кратен w/8, то дополняем L нулевыми битами до ближайшего кратного w/8 размера

        //Кол-во слов, на которые будет разбит исходный ключ
        int wordsAmount = (keySize + len - 1) / len;


        long[] L = new long[wordsAmount];
        //Копируем исходный ключ блоками по w бит в L
        for (int i = 0; i < wordsAmount; i++) {
            L[i] = AuxilaryFunctions.getBits(inputKey, i * len * 8, len * 8);
        }


        int wordsAmountExpandedKey = (rounds + 1) * 2; //2r + 2 слова

        //Генерируем "магические" константы
        long P = RC5_PARAMETERS.get(len * 8).getLeft();
        long Q = RC5_PARAMETERS.get(len * 8).getRight();

        //строим таблицу расширенных ключей: S[0] = Pw, S[i] = S[i-1] + Q
        long[] S = new long[wordsAmountExpandedKey];
        S[0] = P;
        for (int i = 1; i < wordsAmountExpandedKey; i++) {
            S[i] = AuxilaryFunctions.encryptionSum(S[i - 1], Q, len * 8);
        }

        int sizeOfS = S.length;
        int countWords = L.length;

        long A = 0;
        long B = 0;
        int i = 0;
        int j = 0;

        for (int counter = 0; counter < 3 * Integer.max(sizeOfS, countWords); counter++) {
            A = S[i] = AuxilaryFunctions.cyclicShiftLeft(AuxilaryFunctions.encryptionSum(
                    AuxilaryFunctions.encryptionSum(S[i], A, len * 8), B, len * 8), len * 8, 3);

            B = L[j] = AuxilaryFunctions.cyclicShiftLeft(AuxilaryFunctions.encryptionSum(
                            AuxilaryFunctions.encryptionSum(L[j], A, len * 8), B, len * 8), len * 8,
                    AuxilaryFunctions.encryptionSum(A, B, len * 8));

            i = (i + 1) % sizeOfS;
            j = (j + 1) % countWords;
        }

        return S;
    }
}
