package io.seqera.migtool;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Mig tool main launcher
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Command(name = "migtool", mixinStandardHelpOptions = true, description = "Database schema migration tool")
public class App implements Callable<Integer> {

    private static System.Logger logger = System.getLogger(App.class.getSimpleName());

    @Option(names = {"-u", "--username"}, description = "DB connection user")
    private String username;

    @Option(names = {"-p", "--password"}, description = "DB connection password")
    private String password;

    @Option(names = {"--url"}, description = "DB connection URL (uses JDBC syntax)")
    private String url;

    @Option(names = {"--dialect"}, description = "DB dialect")
    private String dialect;

    @Option(names = {"--driver"}, description = "JDBC driver class name")
    private String driver;

    @Option(names = {"--location"}, description = "DB migration scripts location path (local path should be prefixed with `file:`)")
    private String location;

    @Option(names = {"--pattern"}, description = "DB migration scripts file names pattern")
    private String pattern = "^m(\\d+)__(.+)";

    @Override
    public Integer call() {

        // set optional fields
        MigTool tool = new MigTool()
                .withUser(username)
                .withPassword(password)
                .withUrl(url)
                .withDialect(dialect)
                .withDriver(driver)
                .withLocations(location)
                .withPattern(pattern);

        try {
            tool.run();
            return 0;
        }
        catch (Throwable e) {
            final String msg = e.getMessage();
            if( msg != null )
                logger.log(System.Logger.Level.ERROR, msg);
            else
                logger.log(System.Logger.Level.ERROR, e.toString(), e);
            return 1;
        }
    }


    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

}
