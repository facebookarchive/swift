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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import org.apache.tomcat.jni.SessionTicketKey;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.facebook.nifty.ssl.CryptoUtil.decodeHex;
import static com.facebook.nifty.ssl.CryptoUtil.hkdf;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * To make distribution of ticket keys and rotating ticket keys easier, tickets can be distributed
 * as a seed file in the following format
 * <pre>
 * {
 *     "current": ["5afc6fb03ba4b15", "9547a3ab68b440ef7"],
 *     "new": ["5afc6fb03ba4b15", "9547a3ab68b440ef7"],
 *     "old": ["5afc6fb03ba4b15", "9547a3ab68b440ef7"]
 * }
 * </pre>
 *
 * The real ticket keys are generated from the seeds. The seeds can be arbitrary length hex encoded values.
 * The current seeds are used to generate new tickets, and tickets encrypted with the old and new keys are
 * accepted to allow for some leeway in rotation of tickets.
 *
 * The algorithm used to compute the session ticket encryption keys is the following:
 *
 * <pre>
 * aesKey  = hkdf(seed, "aes")  - truncated to AES bytes.
 * hmacKey = hkdf(seed, "hmac") - truncated to HMAC bytes
 * name    = hkdf(seed, "name") - truncated to name bytes
 * </pre>
 */
public class TicketSeedFileParser {

    private static final byte[] NAME_BYTES = new byte[]{'n', 'a', 'm', 'e'};
    private static final byte[] AES_BYTES = new byte[]{'a', 'e', 's'};
    private static final byte[] HMAC_BYTES = new byte[]{'h', 'm', 'a', 'c'};
    private static final List<String> EMPTY_STRING_LIST = ImmutableList.of();

    /*
     * Randomly generated salt to use for the purpose of ticket seeds. The salt in the HKDF is meant to be public.
     * We do not want to use a random salt so that every machine that gets the same seed will compute the same
     * ticket keys.
    */
    private static final byte[] DEFAULT_TICKET_SALT = decodeHex("b78973d13c2d0eb24cf94cd692239867");

    private final byte[] salt;
    private final ObjectMapper objectMapper;

    /**
     * Creates a ticket seed file parser with the default salt value.
     */
    public TicketSeedFileParser() {
        this(DEFAULT_TICKET_SALT);
    }

    /**
     * Creates a ticket seed file parser with the given hex-encoded salt value.
     *
     * @param hexSalt the hex-encoded salt value to use. Must not be null.
     */
    public TicketSeedFileParser(String hexSalt) {
        this(decodeHex(hexSalt));
    }

    /**
     * Creates a ticket seed file parser with the given binary salt value. All servers in a given logical tier
     * should use the same salt value (otherwise tickets will not be interchangeable between servers), but
     * different tiers of machines should use different, unique-per-tier salt values.
     * The salts do not need to be kept secret.
     *
     * @param salt the binary salt value to use. If null, use DEFAULT_TICKET_SALT.
     */
    public TicketSeedFileParser(byte[] salt) {
        if (salt == null) {
            salt = DEFAULT_TICKET_SALT;
        }
        // Copy the salt array to make sure the one we have is not modified later
        this.salt = new byte[salt.length];
        System.arraycopy(salt, 0, this.salt, 0, salt.length);
        objectMapper = new ObjectMapper();
    }

    /**
     * Returns a list of tickets parsed from the ticket file. The keys are returned in a format suitable for use
     * with netty. The first keys are the current keys, following that are the new keys and old keys.
     *
     * @param file the ticket seed file.
     * @return a list of ticket keys. Current keys are first, then new keys, then old keys.
     * @throws IOException if reading the file or parsing the JSON fails.
     * @throws IllegalArgumentException if the JSON file does not contain any current seeds.
     */
    public List<SessionTicketKey> parse(File file) throws IOException {
        return parseBytes(Files.toByteArray(file));
    }

    /**
     * Returns a list of tickets parsed from the given JSON bytes. The keys are returned in a format suitable for
     * use with netty. The first keys are the current keys, following that are the new keys and old keys.
     *
     * @param json the JSON bytes containing ticket seed data.
     * @return a list of ticket keys. Current keys are first, then new keys, then old keys.
     * @throws IOException if parsing the JSON fails.
     * @throws IllegalArgumentException if the JSON does not contain any current seeds.
     */
    public List<SessionTicketKey> parseBytes(byte[] json) throws IOException {
        List<String> allSeeds = TicketSeeds.parseFromJSONBytes(json, objectMapper).getAllSeeds();
        return allSeeds.stream().map(this::deriveKeyFromSeed).collect(Collectors.toList());
    }

    /**
     * Helper class used to represent the contents of a parsed ticket seed file.
     */
    private static class TicketSeeds {
        final List<String> currentSeeds;
        final List<String> newSeeds;
        final List<String> oldSeeds;

        TicketSeeds(List<String> currentSeeds, List<String> newSeeds, List<String> oldSeeds) {
            checkArgument(currentSeeds != null && currentSeeds.size() > 0, "current seeds must not be empty");
            this.currentSeeds = ImmutableList.copyOf(currentSeeds);
            this.newSeeds = ImmutableList.copyOf(newSeeds);
            this.oldSeeds = ImmutableList.copyOf(oldSeeds);
        }

        /**
         * Returns a list of all ticket seeds. The order is: currentSeeds, newSeeds, oldSeeds.
         *
         * @return a list of all ticket seeds.
         */
        List<String> getAllSeeds() {
            return new ImmutableList.Builder<String>()
                .addAll(currentSeeds)
                .addAll(newSeeds)
                .addAll(oldSeeds)
                .build();
        }

        /**
         * Parses the contents of the given bytes as JSON to construct a TicketSeeds object. See comment at the
         * top of this file for example ticket seed file format.
         *
         * @param bytes the contents of a JSON file containing the ticket seeds data.
         * @return a TicketSeeds object.
         * @throws IOException if reading the file or parsing the contents as JSON fails.
         * @throws IllegalArgumentException if the file does not contain any current seeds.
         */
        static TicketSeeds parseFromJSONBytes(byte[] bytes, ObjectMapper objectMapper) throws IOException {
            Map<String, List<String>> map = objectMapper.readValue(
                bytes, new TypeReference<Map<String, List<String>>>(){});
            return new TicketSeeds(
                map.getOrDefault("current", EMPTY_STRING_LIST),
                map.getOrDefault("new", EMPTY_STRING_LIST),
                map.getOrDefault("old", EMPTY_STRING_LIST));
        }
    }

    /**
     * Derives a {@link SessionTicketKey} from the given ticket seed using the {@link CryptoUtil#hkdf} function.
     *
     * @param seed the ticket seed.
     * @return the ticket key.
     */
    private SessionTicketKey deriveKeyFromSeed(String seed) {
        byte[] seedBin = decodeHex(seed);
        byte[] keyName = hkdf(seedBin, salt, NAME_BYTES, SessionTicketKey.NAME_SIZE);
        byte[] aesKey = hkdf(seedBin, salt, AES_BYTES, SessionTicketKey.AES_KEY_SIZE);
        byte[] hmacKey = hkdf(seedBin, salt, HMAC_BYTES, SessionTicketKey.HMAC_KEY_SIZE);
        return new SessionTicketKey(keyName, hmacKey, aesKey);
    }
}
