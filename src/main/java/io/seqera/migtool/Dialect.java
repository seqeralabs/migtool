package io.seqera.migtool;

import java.util.List;

public enum Dialect {
    MYSQL(List.of("mysql")),
    H2(List.of("h2")),
    MARIADB(List.of("mariadb")),
    SQLITE(List.of("sqlite")),
    POSTGRES(List.of("postgres", "postgresql")),
    TCPOSTGRES(List.of("tc"));

    private final List<String> names;

    Dialect(List<String> names) {
        this.names = names;
    }

    public static Dialect from(String dialect) {
        for (Dialect d : Dialect.values()) {
            if (d.names.contains(dialect.toLowerCase())) {
                return d;
            }
        }
        throw new IllegalStateException("Unknown dialect: " + dialect);
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

    boolean isTestContainersPostgres() {
        return this == TCPOSTGRES;
    }

    @Override
    public String toString() {
        if (this == TCPOSTGRES) {
            // the dialect is actually POSTGRES
            return "postgres";
        }
        return names.get(0);
    }
}
