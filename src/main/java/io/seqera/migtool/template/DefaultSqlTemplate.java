package io.seqera.migtool.template;

/**
 * Default SQL template for migtool SQL statements
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
class DefaultSqlTemplate extends SqlTemplate {
    @Override
    public String selectMaxRank(String table) {
        return "select max(`rank`) from " + table;
    }

    @Override
    public String insetMigration(String table) {
        return "insert into "+table+" (`rank`,`script`,`checksum`,`created_on`,`execution_time`) values (?,?,?,?,?)";
    }

    @Override
    public String selectMigration(String table) {
        return "select `id`, `checksum`, `script` from "+table+ " where `rank` = ? and `script` = ?";
    }
}
