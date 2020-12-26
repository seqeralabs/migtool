package io.seqera.migtool;

/**
 * Mig tool main launcher
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
public class App {

    public static void main(String[] args) {
        MigTool tool = new MigTool();
        try {
            tool.run();
        }
        finally {
            tool.close();
        }
    }
}
