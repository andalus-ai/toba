package com.dremioplatform.catalog.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The Iceberg REST Catalog spec encodes a multi-level namespace as a single path
 * segment with levels joined by the unit separator character (0x1F), e.g. the
 * namespace ["sales", "q4"] appears in a URL as "sales%1Fq4". This class converts
 * between that wire format and the dot-joined format {@link com.dremioplatform.catalog.domain.CatalogNamespace}
 * stores internally (dots are easier to read/debug and don't need URL-decoding
 * twice, but must never appear as a literal character inside a namespace level —
 * {@link #encode} rejects levels containing a dot for exactly that reason).
 */
public final class NamespaceCodec {

    private static final char UNIT_SEPARATOR = '\u001F';

    private NamespaceCodec() {
    }

    public static List<String> decodeWireFormat(String pathSegment) {
        return Arrays.stream(pathSegment.split(String.valueOf(UNIT_SEPARATOR)))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static String toStoragePath(List<String> levels) {
        for (String level : levels) {
            if (level.contains(".")) {
                throw new IllegalArgumentException(
                        "Namespace level '" + level + "' contains '.', which this storage encoding reserves as a separator");
            }
        }
        return String.join(".", levels);
    }

    public static List<String> fromStoragePath(String storagePath) {
        return Arrays.stream(storagePath.split("\\."))
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static String wireFormatToStoragePath(String pathSegment) {
        return toStoragePath(decodeWireFormat(pathSegment));
    }

    public static String storagePathToDisplayJoined(String storagePath) {
        return fromStoragePath(storagePath).stream().collect(Collectors.joining("."));
    }
}
