package io.seqera.migtool;

public enum Driver {
    MYSQL("com.mysql.cj.jdbc.Driver"),
    H2("org.h2.Driver"),
    SQLITE("org.sqlite.JDBC"),
    POSTGRES("org.postgresql.Driver"),
    TCPOSTGRES("org.testcontainers.jdbc.ContainerDatabaseDriver");

    private final String driver;

    Driver(String driver) {
        this.driver = driver;
    }

    static Driver from(String driver) {
        for (Driver d : Driver.values()) {
            if (d.driver.equals(driver)) {
                return d;
            }
        }
        throw new IllegalStateException("Unknown driver: " + driver);
    }

    @Override
    public String toString() {
        return driver;
    }
}
