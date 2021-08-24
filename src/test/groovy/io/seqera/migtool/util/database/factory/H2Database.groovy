package io.seqera.migtool.util.database.factory

import groovy.transform.Memoized
import io.seqera.migtool.util.database.DbConfig

class H2Database extends AbstractDatabase {

    private static final String user = 'sa'
    private static final String password = ''
    private static final String schema = 'test'

    protected H2Database() {}

    @Override
    @Memoized
    DbConfig getConfig() {
        return new DbConfig(
                "jdbc:h2:file:./.db/h2/${schema};DB_CLOSE_ON_EXIT=FALSE",
                user,
                password,
                'org.h2.Driver',
                'h2'
        )
    }

    @Override
    protected String getListTablesDDL() {
        return """
            SELECT t.table_name FROM information_schema.tables t
            WHERE (t.table_schema = 'PUBLIC') AND (t.table_type LIKE '%TABLE')
        """
    }

    @Override
    protected String getConstraintsCheckDDL(boolean enable) {
        return "SET REFERENTIAL_INTEGRITY ${enable.toString().toUpperCase()};"
    }

}
