/*
 * Copyright (C) 2012-2016 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.nifty.ssl;

import org.testng.Assert;
import org.testng.annotations.Test;

import static com.facebook.nifty.ssl.CryptoUtil.decodeHex;
import static com.facebook.nifty.ssl.CryptoUtil.hkdf;

public class CryptoUtilTest {

    class TestVector {
        String key;
        String salt;
        String info;
        String expected;
    }

    // Test cases taken from https://tools.ietf.org/html/rfc5869
    public void validateVector(TestVector vector) {
        byte[] key = decodeHex(vector.key);
        byte[] salt = vector.salt.isEmpty() ? null : decodeHex(vector.salt);
        byte[] info = vector.info.isEmpty() ? null : decodeHex(vector.info);
        byte[] expected = decodeHex(vector.expected);
        byte[] actual = hkdf(key, salt, info, expected.length);
        Assert.assertEquals(actual, expected);
    }

    /**
     * Test vector data is from RFC 5869, appendix A.1. (Test Case 1)
     */
    @Test
    public void testHkdf1() {
        TestVector vector = new TestVector();
        vector.key = "0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b";
        vector.salt = "000102030405060708090a0b0c";
        vector.info = "f0f1f2f3f4f5f6f7f8f9";
        vector.expected = "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865";
        validateVector(vector);
    }

    /**
     * Test vector data is from RFC 5869, appendix A.2. (Test Case 2)
     */
    @Test
    public void testHkdf2() {
        TestVector vector = new TestVector();
        vector.key = "000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b" +
            "2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f";
        vector.salt = "606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8" +
            "b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeaf";
        vector.info = "b0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dad" +
            "bdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff";
        vector.expected = "b11e398dc80327a1c8e7f78c596a49344f012eda2d4efad8a050cc4c19afa97c59045a99cac7827271c" +
            "b41c65e590e09da3275600c2f09b8367793a9aca3db71cc30c58179ec3e87c14c01d5c1f3434f";
        validateVector(vector);
    }

    /**
     * Test vector data is from RFC 5869, appendix A.3. (Test Case 3)
     */
    @Test
    public void testHkdf3() {
        TestVector vector = new TestVector();
        vector.key = "0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b";
        vector.salt = "";
        vector.info = "";
        vector.expected = "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8";
        validateVector(vector);
    }
}
