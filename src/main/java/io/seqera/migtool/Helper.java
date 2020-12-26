package io.seqera.migtool;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implement helper functions
 * 
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
public class Helper {

    public static List<String> getResourceFiles(String path) {
        if( path==null )
            throw new IllegalArgumentException("Missing resource path");

        List<String> filenames = new ArrayList<>();

        try (
                InputStream in = getResourceAsStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(in)) ) {
            String resource;

            while ((resource = br.readLine()) != null) {
                String name = path.endsWith("/") ? path : path +"/";
                filenames.add(name + resource);
            }
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to scan classpath: " + path, e);
        }

        return filenames;
    }

    public static InputStream getResourceAsStream(String resourceName) {
        final InputStream in = getContextClassLoader().getResourceAsStream(resourceName);
        return in == null ? Helper.class.getResourceAsStream(resourceName) : in;
    }

    public static String getResourceAsString(String resourceName) {
        try {
            InputStream is = getResourceAsStream(resourceName);
            return Helper.getText(is);
        }
        catch (IOException e) {
            throw new IllegalStateException("Unable to get resource string: " + resourceName + " -- cause: " + e.getMessage(), e);
        }
    }

    static private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static String computeSha256(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload);
            return bytesToHex(hash);
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Unable to compute SHA-256 message digest -- cause: " + e.getMessage(), e);
        }
    }

    public static String computeSha256(String payload) {
        return computeSha256(payload.getBytes(StandardCharsets.UTF_8));
    }

    public static String computeSha256(InputStream payload) throws IOException {
        return computeSha256( Helper.getBytes(payload) );
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    static String getText(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        return getText(reader);
    }

    static String getText(BufferedReader reader) throws IOException {
        StringBuilder answer = new StringBuilder();
        // reading the content of the file within a char buffer
        // allow to keep the correct line endings
        char[] charBuffer = new char[8192];
        int nbCharRead /* = 0*/;
        try {
            while ((nbCharRead = reader.read(charBuffer)) != -1) {
                // appends buffer
                answer.append(charBuffer, 0, nbCharRead);
            }
            Reader temp = reader;
            reader = null;
            temp.close();
        } finally {
            closeWithWarning(reader);
        }
        return answer.toString();
    }

    static byte[] getBytes(InputStream is) throws IOException {
        ByteArrayOutputStream answer = new ByteArrayOutputStream();
        // reading the content of the file within a byte buffer
        byte[] byteBuffer = new byte[8192];
        int nbByteRead /* = 0*/;
        try {
            while ((nbByteRead = is.read(byteBuffer)) != -1) {
                // appends buffer
                answer.write(byteBuffer, 0, nbByteRead);
            }
        } finally {
            closeWithWarning(is);
        }
        return answer.toByteArray();
    }

    public static Throwable tryClose(AutoCloseable closeable) {
        Throwable thrown = null;
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                thrown = e;
            }
        }
        return thrown;
    }

    private static void closeWithWarning(Closeable closeable) {
        tryClose(closeable);
    }

}
