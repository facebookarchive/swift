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

import com.fasterxml.jackson.core.JsonParseException;
import org.apache.tomcat.jni.SessionTicketKey;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TicketSeedFileParserTest {

    @Test
    public void testParseSeeds() throws IOException {
        List<SessionTicketKey> keys = new TicketSeedFileParser().parse(
            new File(TicketSeedFileParserTest.class.getResource("/good_seeds.json").getFile()));
        Assert.assertEquals(6, keys.size());

        // The seeds in the file are arranged so that every 2 are the same, and adjacent ones are not.
        for (int i = 0; i < keys.size() / 2; i += 2) {
            Assert.assertEquals(keys.get(i).getHmacKey(), keys.get(i + 2).getHmacKey(), "HMAC keys not equal");
            Assert.assertEquals(keys.get(i).getName(), keys.get(i + 2).getName(), "key names not equal");
            Assert.assertEquals(keys.get(i).getAesKey(), keys.get(i + 2).getAesKey(), "AES keys not equal");
        }
        for (int i = 0; i < keys.size() - 1; i++) {
            Assert.assertNotEquals(keys.get(i).getHmacKey(), keys.get(i + 1).getHmacKey());
            Assert.assertNotEquals(keys.get(i).getName(), keys.get(i + 1).getName());
            Assert.assertNotEquals(keys.get(i).getAesKey(), keys.get(i + 1).getAesKey());
        }

        List<SessionTicketKey> keys2 = new TicketSeedFileParser().parse(
            new File(TicketSeedFileParserTest.class.getResource("/good_seeds.json").getFile()));
        Assert.assertEquals(keys.size(), keys2.size());
        for (int i = 0; i < keys.size(); ++i) {
            Assert.assertEquals(keys.get(i).getAesKey(), keys2.get(i).getAesKey(), "AES keys not equal");
            Assert.assertEquals(keys.get(i).getName(), keys2.get(i).getName(), "key names not equal");
            Assert.assertEquals(keys.get(i).getHmacKey(), keys2.get(i).getHmacKey(), "HMAC keys not equal");
        }
    }

    @Test
    public void testParseCurrentSeeds() throws IOException {
        List<SessionTicketKey> keys = new TicketSeedFileParser().parse(
            new File(TicketSeedFileParserTest.class.getResource("/seeds_with_only_current.json").getFile()));
        Assert.assertEquals(2, keys.size());
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "current seeds must not be empty")
    public void testParseNoCurrentSeeds() throws IOException {
        new TicketSeedFileParser().parse(
            new File(TicketSeedFileParserTest.class.getResource("/seeds_with_no_current.json").getFile()));
    }

    @Test(expectedExceptions = JsonParseException.class)
    public void testParseBadJson() throws IOException {
        new TicketSeedFileParser().parse(
            new File(TicketSeedFileParserTest.class.getResource("/seeds_bad_json.json").getFile()));
    }
}
