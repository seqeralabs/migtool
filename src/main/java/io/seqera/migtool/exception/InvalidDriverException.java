package io.seqera.migtool.exception;

public class InvalidDriverException extends MigToolException {

    public InvalidDriverException(String driver, Throwable cause) {
        super("Unable to find driver class: '" + driver + "'", cause);
    }

}
