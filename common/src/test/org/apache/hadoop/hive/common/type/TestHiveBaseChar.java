package org.apache.hadoop.hive.common.type;

import java.util.Random;

import junit.framework.TestCase;

public class TestHiveBaseChar extends TestCase {
  static Random rnd = new Random();

  public static int getRandomSupplementaryChar() {
    int lowSurrogate = 0xDC00 + rnd.nextInt(1024);
    //return 0xD8000000 + lowSurrogate;
    int highSurrogate = 0xD800;
    return Character.toCodePoint((char)highSurrogate, (char)lowSurrogate);
  }

  public static int getRandomCodePoint() {
    int codePoint;
    if (rnd.nextDouble() < 0.50) {
      codePoint = 32 + rnd.nextInt(90);
    } else {
      codePoint = getRandomSupplementaryChar();
    }
    if (!Character.isValidCodePoint(codePoint)) {
      System.out.println(Integer.toHexString(codePoint) + " is not a valid code point");
    }
    return codePoint;
  }

  public static int getRandomCodePoint(int excludeChar) {
    while (true) {
      int codePoint = getRandomCodePoint();
      if (codePoint != excludeChar) {
        return codePoint;
      }
    }
  }

  public static String createRandomSupplementaryCharString(int len) {
    StringBuffer sb = new StringBuffer();
    for (int idx = 0; idx < len; ++idx) {
      sb.appendCodePoint(getRandomCodePoint(' '));
    }
    return sb.toString();
  }

  public void testStringLength() throws Exception {
    int strLen = 20;
    int[] lengths = { 15, 20, 25 };
    // Try with supplementary characters
    for (int idx1 = 0; idx1 < lengths.length; ++idx1) {
      // Create random test string
      int curLen = lengths[idx1];
      String testString = createRandomSupplementaryCharString(curLen);
      assertEquals(curLen, testString.codePointCount(0, testString.length()));
      String enforcedString = HiveBaseChar.enforceMaxLength(testString, strLen);
      if (curLen <= strLen) {
        // No truncation needed
        assertEquals(testString, enforcedString);
      } else {
        // String should have been truncated.
        assertEquals(strLen, enforcedString.codePointCount(0, enforcedString.length()));
      }
    }
  }

  public void testGetPaddedValue() {
    int strLen = 20;
    int[] lengths = { 15, 20, 25 };
    for (int idx1 = 0; idx1 < lengths.length; ++idx1) {
      int curLen = lengths[idx1];
      // Random test string
      String testString = createRandomSupplementaryCharString(curLen);
      assertEquals(curLen, testString.codePointCount(0, testString.length()));
      String paddedString = HiveBaseChar.getPaddedValue(testString, strLen);
      assertEquals(strLen, paddedString.codePointCount(0, paddedString.length()));
    }

    assertEquals("abc       ", HiveBaseChar.getPaddedValue("abc", 10));
    assertEquals("abc       ", HiveBaseChar.getPaddedValue("abc ", 10));
  }
}
