package tv.okko.devops.ns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.citrix.netscaler.nitro.exception.nitro_exception;
import com.citrix.netscaler.nitro.resource.config.basic.server.gracefulEnum;
import com.citrix.netscaler.nitro.resource.config.basic.service;
import com.citrix.netscaler.nitro.resource.config.basic.service.monitor_stateEnum;
import com.citrix.netscaler.nitro.service.nitro_service;
import com.citrix.netscaler.nitro.resource.stat.ha.hanode_stats;
import com.citrix.netscaler.nitro.resource.config.ha.hanode.stateEnum;

public class ServiceManager {

    private static final int MAX_AWAIT_TIME = 90000; //max await time in ms.
    private final Options options;
    private nitro_service[] theNitros;
    private static final Logger LOG = Logger.getLogger(ServiceManager.class.getName());

    public ServiceManager(Options options) throws nitro_exception {
        this.options = options;
        this.theNitros = new nitro_service[options.getIp().length];
        for (Integer i = 0; i < options.getIp().length; i++) {
            this.theNitros[i] = new nitro_service(options.getIp()[i], "HTTP");
        }
    }

    public boolean isPrimary(String ip, nitro_service nitro) throws Exception {
        String haStatus;
        String message;
        haStatus = hanode_stats.get(nitro).get_hacurmasterstate();
        switch (haStatus) {
            case stateEnum.Primary:
                message = "Server " + ip + " is a " + stateEnum.Primary + " node";
                LOG.info(message);
                return true;
            case stateEnum.Secondary:
                message = "Server " + ip + " is a " + stateEnum.Secondary + " node";
                LOG.info(message);
                return false;
            default:
                message = "Can't determinate " + ip + " HA status, current status: " + haStatus;
                LOG.error(message);
                throw new RuntimeException(message);
        }
    }

    public void disableService() throws Exception {
        new Callback() {
            @Override
            public void doExecute(service srv, nitro_service nsmaster) throws Exception {
                String svrstate = srv.get_svrstate();
                switch (svrstate) {
                    case "UP":
                        disable(srv, nsmaster, 90);
                        awaitStatus(srv, nsmaster, monitor_stateEnum.OUT_OF_SERVICE, monitor_stateEnum.DOWN);
                        break;
                    case "OUT OF SERVICE":
                    case "DOWN":
                        LOG.warn("Service " + srv.get_name() + " already in " + svrstate + " state, nothing to do");
                        break;
                    default:
                        String message = "Unknown service " + svrstate + " state type, should be " + monitor_stateEnum.UP;
                        LOG.warn(message);
                        throw new RuntimeException(message);
                }
            }
        }.execute();
    }

    private void awaitStatus(service srv, nitro_service nsmaster, String... states) throws Exception {
        Set<String> terminalStates = new HashSet<>(Arrays.asList(states));

        long time = System.currentTimeMillis();
        while (true) {
            srv = service.get(nsmaster, srv.get_name());
            String state = srv.get_svrstate();
            if (terminalStates.contains(state)) {
                LOG.debug("Service " + srv.get_name() + " is now " + state);
                break;
            } else if (System.currentTimeMillis() - time > MAX_AWAIT_TIME) {
                LOG.debug("Service " + srv.get_name() + " status undefined due to a timeout");
                throw new AwaitTimeExhausted("Service " + srv.get_name() + " status undefined due to a timeout");
            } else {
                TimeUnit.SECONDS.sleep(1);
            }
        }
    }

    private void disable(service srv, nitro_service nsmaster, long timeout) throws Exception {
        srv.set_graceful(gracefulEnum.YES);
        srv.set_delay(timeout);
        service.disable(nsmaster, srv);
    }

    public void enableService() throws Exception {
        new Callback() {
            @Override
            public void doExecute(service srv, nitro_service nsmaster) throws Exception {
                switch (srv.get_svrstate()) {
                    case "UP":
                        LOG.debug("Service " + srv.get_name() + " already in UP state, nothing to do");
                        break;
                    case "OUT OF SERVICE":
                    case "DOWN":
                        service.enable(nsmaster, srv);
                        awaitStatus(srv, nsmaster, monitor_stateEnum.UP);
                        break;
                    default:
                        String message = "Unknown service " + srv.get_name() + " state type, should be " + monitor_stateEnum.OUT_OF_SERVICE + " or " +
                                monitor_stateEnum.DOWN + " state";
                        LOG.warn(message);
                        throw new RuntimeException(message);
                }
            }
        }.execute();
    }

    public service[] getServices(nitro_service nsmaster) throws Exception {
        String fqdn = options.getServerName();
        if (fqdn == null) {
            throw new NullPointerException(" :: Variable 'fqdn' was null inside method getServices.");
        } else {
            String host = fqdn.split("\\.")[0];
            int port = options.getPort();

            service[] services = service.get_filtered(nsmaster, "servername:" + host + ",port:" + port);
            if (services == null || services.length == 0) {
                LOG.debug("No matches found in NS services with server name: " + host + ", trying " + fqdn);
                services = service.get_filtered(nsmaster, "servername:" + fqdn + ",port:" + port);
                if (services == null || services.length == 0) {
                    String addr = toAddress(fqdn);
                    LOG.debug("No matches found in NS services with server name: " + fqdn + ", trying " + addr);
                    services = service.get_filtered(nsmaster, "servername:" + addr + ",port:" + port);
                    if (services == null || services.length == 0) {
                        LOG.debug("Can't find service by server name " + fqdn + " with corresponding port " + port + " in NS config, nothing to do");
                        return null;
                    }
                }
            }

            return services;
        }
    }

    private static String toAddress(String name) {
        try {
            return InetAddress.getByName(name).getHostAddress();
        } catch (UnknownHostException e) {
            LOG.error("Can't resolve host name " + name + ", check DNS or host name is correct");
            throw new RuntimeException(e);
        }
    }

    private abstract class Callback {
        abstract void doExecute(service srv, nitro_service nsmaster) throws Exception;

        public void execute() throws Exception {
            for (Integer i = 0; i < options.getIp().length; i++) {
                try {
                    theNitros[i].login(options.getUsername(), options.getPassword());
                    if (isPrimary(options.getIp()[i], theNitros[i])) {
                        service[] services = getServices(theNitros[i]);
                        if (services != null) {
                            doExecute(services[0], theNitros[i]);
                        }
                        break;
                    }
                } finally {
                    try {
                        theNitros[i].logout();
                    } catch (Exception e) {
                        e.printStackTrace();
                        LOG.error(e.getMessage());
                    }
                }
            }
        }
    }
}
