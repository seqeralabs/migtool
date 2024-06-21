package io.seqera.migtool;

import jakarta.annotation.Nullable;

public enum Driver {
    MYSQL("com.mysql.cj.jdbc.Driver"),
    H2("org.h2.Driver"),
    SQLITE("org.sqlite.JDBC"),
    POSTGRES("org.postgresql.Driver"),
    ;

    private final String driver;

    Driver(String driver) {
        this.driver = driver;
    }

    static Driver fromDriverName(String driver) {
        for (Driver d : Driver.values()) {
            if (d.driver.equals(driver)) {
                return d;
            }
        }
        throw new IllegalStateException("Unknown driver: " + driver);
    }

    @Nullable
    static Driver fromUrl(@Nullable String url) {
        final Dialect dialect = Dialect.fromUrl(url);
        return dialect == null ? null : dialect.driver();
    }

    @Override
    public String toString() {
        return driver;
    }
}
