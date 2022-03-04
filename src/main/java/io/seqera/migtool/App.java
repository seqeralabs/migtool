package io.seqera.migtool;

/**
 * Mig tool main launcher
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
public class App {

    public static void main(String[] args) {
        if( args.length != 7){
            System.out.println("invalid arguments");
            System.out.println("user password url dialect driver locations pattern");
            System.exit(-1);
            return;
        }
        MigTool tool = new MigTool()
                .withUser(args[0])
                .withPassword(args[1])
                .withUrl(args[2])
                .withDialect(args[3])
                .withDriver(args[4])
                .withLocations(args[5])
                .withClassLoader(App.class.getClassLoader())
                .withPattern(args[6]);
        tool.run();
    }
}
