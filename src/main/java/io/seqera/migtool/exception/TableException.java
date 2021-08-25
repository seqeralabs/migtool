package io.seqera.migtool.exception;

/**
 * Error occurred on table existence checking.
 */
public class TableException extends MigToolException {

    public TableException(String tableName, Throwable cause) {
        super("Unable to check table '" + tableName + "' existence", cause);
    }

}
