package io.seqera.migtool.exception;

/**
 * Superclass of specific idiomatic exceptions in the application.
 */
public class MigToolException extends RuntimeException {

    MigToolException(String message, Throwable cause) {
        super(message, cause);
    }

}
