package tv.okko.devops.ns;

public class Options {
    private final String[] ip;
    private final String username;
    private final String password;
    private final String serverName;
    private final int port;

    public Options(String ip, String username, String password, String serverName, int port) {
        this.ip = ip.split(",");
        this.username = username;
        this.password = password;
        this.serverName = serverName;
        this.port = port;
    }

    public String[] getIp() {
        return ip;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public String getServerName() {
        return serverName;
    }

    public String getUsername() {
        return username;
    }
}
