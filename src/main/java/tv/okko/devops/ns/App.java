package tv.okko.devops.ns;

import com.citrix.netscaler.nitro.exception.nitro_exception;

import org.apache.log4j.Logger;

import java.util.Arrays;

public class App {
    private static final Logger LOG = Logger.getLogger(ServiceManager.class.getName());

    public static void main(String[] args) throws nitro_exception {
        try {
            ServiceManager sm = new ServiceManager(parseArgs(args));

            switch (parseAction(args)) {
                case ENABLE:
                    sm.enableService();
                    break;
                case DISABLE:
                    sm.disableService();
                    break;
                default:
                    throw new RuntimeException("Unknown behavior");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e.getMessage());
            System.exit(1);
        }
    }

    private static Options parseArgs(String[] args) {
        if (args.length != 6) {
            VersionClass version = new VersionClass();
            System.err.println("Version: " + version.getVersion() + "\nUsage: ns-manager <ip,ip2,...ipn> <username> <password> <servername> <port> <action>");
            System.exit(2);
        }

        int port = -1;
        try {
            port = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println("Port " + args[4] + " is not a digit");
            System.exit(2);
        }

        return new Options(args[0], args[1], args[2], args[3], port);
    }

    private static Action parseAction(String[] args) {
        Action action = null;
        try {
            action = Action.toAction(args[5].trim());
        } catch (IllegalArgumentException e) {
            System.err.println("Action [" + args[5] + "] is no supported, allow only " + Arrays.asList(Action.values()));
            System.exit(2);
        } catch (NullPointerException npe) {
            LOG.error(npe.getMessage());
            System.exit(1);
        } catch (Exception e) {
            LOG.error(e.getMessage());
            System.exit(1);
        }
        return action;
    }

}
