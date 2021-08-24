package io.seqera.migtool.util.database

import spock.lang.Specification

import io.seqera.migtool.util.database.factory.DatabaseFactory
import io.seqera.migtool.util.database.factory.Database


abstract class DatabaseSpecification extends Specification {

    protected static final Database database = DatabaseFactory.database

    void cleanup() {
        database.cleanup()
    }

}
