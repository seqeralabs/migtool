package io.seqera.migtool.util.database.factory

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.seqera.migtool.Dialect

@Slf4j
@CompileStatic
final class DatabaseFactory {

    private DatabaseFactory() {}

    static Database getDatabase() {
        Dialect dialect = dialectFromEnv

        if (dialect == Dialect.h2) return new H2Database()
        if (dialect == Dialect.mysql) return new MySqlDatabase('5.6')
        if (dialect == Dialect.mariadb) return new MariaDbDatabase()

        log.info("Unknown DB type: '${rawDbFromEnv}'. Defaulting to '${Dialect.h2}'")
        return new H2Database()
    }

    static Dialect getDialectFromEnv() {
        return Dialect.getByString(rawDbFromEnv)
    }

    private static String getRawDbFromEnv() {
        return System.getenv('MIGTOOL_DB')
    }

}
