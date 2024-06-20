package io.seqera.migtool;

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

    public static Dialect from(String dialect) {
        for (Dialect d : Dialect.values()) {
            if (d.names.contains(dialect.toLowerCase())) {
                return d;
            }
        }
        throw new IllegalStateException("Unknown dialect: " + dialect);
    }

    public static Dialect from(Driver driver) {
        for (Dialect d : Dialect.values()) {
            if (d.driver == driver) {
                return d;
            }
        }
        throw new IllegalStateException("Cannot get dialect for driver: " + driver);
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

    boolean isSQLite() {
        return this == SQLITE;
    }

    boolean isMariaDB() {
        return this == MARIADB;
    }

    @Override
    public String toString() {
        return names.get(0);
    }
}
