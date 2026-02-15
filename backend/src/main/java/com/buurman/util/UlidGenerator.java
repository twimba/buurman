package com.buurman.util;

import com.buurman.domain.Ulid;
import de.huxhorn.sulky.ulid.ULID;

public class UlidGenerator {

    private static final ULID ulid = new ULID();

    private UlidGenerator() {
        // Private constructor to prevent instantiation
    }

    public static Ulid generate() {
        return Ulid.of(ulid.nextULID());
    }

    public static Ulid generate(EntityPrefix prefix) {
        return Ulid.of(prefix.getCode() + ulid.nextULID());
    }
}
