package io.seqera.migtool.template;

/**
 * PostreSQL dialect implementation
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class PostgreSqlTemplate extends SqlTemplate {

    @Override
    public String selectMaxRank(String table) {
        return "select max(rank) from " + table;
    }

    @Override
    public String insetMigration(String table) {
        return "insert into "+table+" (rank,script,checksum,created_on,execution_time) values (?,?,?,?,?)";
    }

    @Override
    public String selectMigration(String table) {
        return "select id, checksum, script from "+table+ " where rank = ? and script = ?";
    }
    
}
