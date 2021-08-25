package io.seqera.migtool.exception;

/**
 * Error occurred on SQL statement execution.
 */
public class StatementException extends MigToolException {

    public StatementException(String stmText, Throwable cause) {
        super("Unable to execute statement '" + stmText + "'", cause);
    }

}
