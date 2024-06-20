package io.seqera.migtool

import spock.lang.Specification

class DialectTest extends Specification {

    def 'should parse dialect from url'() {
        expect:
        Dialect.fromUrl(null) == null
        Dialect.fromUrl('jdbc:h2:file:./.db/h2/tower').isH2()
        Dialect.fromUrl('jdbc:mysql://foo.com:3306/licman').isMySQL()
        Dialect.fromUrl('jdbc:postgresql://localhost:5432/test').isPostgres()
    }
}
