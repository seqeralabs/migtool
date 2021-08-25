package io.seqera.migtool;

import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

/**
 * Supported SQL dialects.
 */
public enum Dialect {

    h2, mysql, mariadb, postgresql;

    private static final Map<String, Dialect> valuesByString = Stream.of(values()).collect(toMap(Object::toString, e -> e));

    public static Dialect getByString(String representation) {
        return valuesByString.get(representation);
    }

}
