package io.seqera.migtool;

import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import java.util.List;

public enum Dialect {
    MYSQL(List.of("mysql"), Driver.MYSQL),
    H2(List.of("h2"), Driver.H2),
    MARIADB(List.of("mariadb"), Driver.MYSQL),
    SQLITE(List.of("sqlite"), Driver.SQLITE),
    POSTGRES(List.of("postgres", "postgresql"), Driver.POSTGRES),
    ;

    private final List<String> names;
    private final Driver driver;

    Dialect(List<String> names, Driver driver) {
        this.names = names;
        this.driver = driver;
    }

    static Dialect fromDialectName(String dialectName) {
        for (Dialect d : Dialect.values()) {
            if (d.names.contains(dialectName.toLowerCase())) {
                return d;
            }
        }
        throw new IllegalStateException("Unknown dialect: " + dialectName);
    }

    @Nullable
    static Dialect fromUrl(@Nullable String url) {
        if (url == null) {
            return null;
        }
        String[] parts = url.split(":");
        return parts.length > 1 ? fromDialectName(parts[1]) : null;
    }

    public Driver driver() {
        return driver;
    }

    boolean isPostgres() {
        return this == POSTGRES;
    }

    boolean isMySQL() {
        return this == MYSQL;
    }

    boolean isH2() {
        return this == H2;
    }

    @Override
    public String toString() {
        return names.get(0);
    }
}
