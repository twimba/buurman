package com.buurman.util;

import de.huxhorn.sulky.ulid.ULID;

public class UlidGenerator {

    private static final ULID ulid = new ULID();

    private UlidGenerator() {
        // Private constructor to prevent instantiation
    }

    public static String generate() {
        return ulid.nextULID();
    }

    public static String generate(EntityPrefix prefix) {
        return prefix.getCode() + ulid.nextULID();
    }
}
