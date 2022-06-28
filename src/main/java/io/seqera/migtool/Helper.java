package io.seqera.migtool;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Implement helper functions
 * 
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
public class Helper {

    public static Set<String> getResourceFiles(String path) {
        if( path==null )
            throw new IllegalArgumentException("Missing resource path");

        URL url = getResourceUrl(path);
        if( url==null )
            throw new IllegalStateException("Resource in '" + path + "' not found");

        boolean isInJar = url.getProtocol().equals("jar");
        if (isInJar) {
            return getResourceFilesFromJar(url, path);
        }

        boolean isFile = url.getProtocol().equals("file");
        if (isFile) {
            return getResourceFilesFromDir(url, path);
        }

        throw new IllegalStateException("Resources in '" + path + "' are neither in a JAR nor in the filesystem");
    }

    public static InputStream getResourceAsStream(String resourceName) {
        InputStream result = Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName);
        return result != null ? result : Helper.class.getResourceAsStream(resourceName);
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

    private static URL getResourceUrl(String resourceName) {
        URL resourceUrl = Thread
                .currentThread()
                .getContextClassLoader()
                .getResource(resourceName);

        return resourceUrl != null ? resourceUrl : Helper.class.getResource(resourceName);
    }

    private static Set<String> getResourceFilesFromJar(URL resourceUrl, String path) {
        Set<String> filenames = new HashSet<>();

        try {
            JarURLConnection connection = (JarURLConnection) resourceUrl.openConnection();
            JarFile jarFile = connection.getJarFile();

            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement().getName();
                if (entryName.startsWith(path)) {
                    filenames.add(entryName);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to scan classpath: " + path, e);
        }

        return filenames;
    }

    private static Set<String> getResourceFilesFromDir(URL resourceUrl, String path) {
        Set<String> filenames = new HashSet<>();

        try (
                InputStream in = resourceUrl.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(in))
        ) {
            String resource;
            while ((resource = br.readLine()) != null) {
                String name = path.endsWith("/") ? path : path + "/";
                filenames.add(name + resource);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to scan classpath: " + path, e);
        }

        return filenames;
    }

    private static void closeWithWarning(Closeable closeable) {
        tryClose(closeable);
    }

    static public String dialectFromUrl(String url) {
        if( url==null )
            return null;
        String[] parts = url.split(":");
        return parts.length > 1 ? parts[1] : null;
    }

    static public String driverFromUrl(String url) {
        final String dialect = dialectFromUrl(url);
        if( "mysql".equals(dialect) )
            return "com.mysql.cj.jdbc.Driver";
        if( "h2".equals(dialect))
            return "org.h2.Driver";
        if( "sqlite".equals(dialect))
            return "org.sqlite.JDBC";
        return null;
    }
}
