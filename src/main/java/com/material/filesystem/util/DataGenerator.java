package com.material.filesystem.util;

import java.util.Random;


public class DataGenerator {

  public static byte[] randomArray(int size) {
    byte[] randomA = new byte[size];
    new Random().nextBytes(randomA);
    return randomA;
  }
}
