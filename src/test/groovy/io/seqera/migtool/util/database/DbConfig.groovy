package io.seqera.migtool.util.database

import groovy.transform.CompileStatic

@CompileStatic
class DbConfig {

    final String url
    final String user
    final String password
    final String driver
    final String dialect

    DbConfig(String url, String user, String password, String driver, String dialect) {
        this.url = url
        this.user = user
        this.password = password
        this.driver = driver
        this.dialect = dialect
    }

}
