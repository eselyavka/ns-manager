package tv.okko.devops.ns;

public class VersionClass {

    public String getVersion() {
        return this.getClass().getPackage().getImplementationVersion();
    }
}