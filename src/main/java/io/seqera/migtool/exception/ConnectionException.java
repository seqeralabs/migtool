package io.seqera.migtool.exception;

/**
 * Error occurred during database connection phase.
 */
public class ConnectionException extends RuntimeException {

    public ConnectionException(String url, Throwable cause) {
        super("Unable to connect DB instance '" + url + "'", cause);
    }

}