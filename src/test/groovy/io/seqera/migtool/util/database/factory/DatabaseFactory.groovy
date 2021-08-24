package io.seqera.migtool.util.database.factory

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
final class DatabaseFactory {

    private enum DatabaseType { mysql, mariadb, postgres, h2 }

    private DatabaseFactory() {}

    static Database getDatabase() {
        String db = System.getenv('MIGTOOL_DB') ?: 'h2'
        DatabaseType databaseType = determineDatabaseType(db)

        if (databaseType == DatabaseType.h2) return new H2Database()
        if (databaseType == DatabaseType.mysql) return new MySqlDatabase('5.6')
        if (databaseType == DatabaseType.mariadb) return new MariaDbDatabase()

        log.info("Unknown DB type: '${db}'")
        return new H2Database()
    }

    private static DatabaseType determineDatabaseType(String db) {
        return DatabaseType.values().find { it.toString() == db }
    }

}
