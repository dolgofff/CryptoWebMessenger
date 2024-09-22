package com.project.Messenger.backend.encryption.dhProtocol;

import java.math.BigInteger;
import java.util.Random;

public class DiffieHellmanProtocol {


    //Числа для генератора aka основания
    private static final int[] PRIME_NUMBERS = new int[]{2, 3, 5, 7, 11, 13, 17};

    private static final Random RANDOM = new Random();

    private DiffieHellmanProtocol() {
    }

    public static BigInteger[] generationEngine(int length) {
        //Генерим случайное основание g из PRIME_NUMBERS
        BigInteger g = BigInteger.valueOf(PRIME_NUMBERS[RANDOM.nextInt(7)]);
        BigInteger p;


        //Ищем такое случайное простое p, чтобы g^(p-1) mod p = 1. т.е. образовывалась полная мультипликативная группа
        do {
            p = BigInteger.probablePrime(length, RANDOM);
        } while (!g.modPow(p.subtract(BigInteger.ONE), p).equals(BigInteger.ONE));

        return new BigInteger[]{p, g};
    }

    //Числа p и g сгенерированы на стороне комнаты и хранятся в её параметрах.
    //Когда User1 создал комнату с User2, kafka со стороны User1 посылает сигнал "roomCreated: {roomId,X}"
    //Где X - созданное из его вычислений число.
    //User2 по ключу roomId, понимает, что ему необходимо вычислить своё число Y и отправить его создателю

    //User1 заходит в комнату и сразу генерирует своё приватное число.
    //
}
