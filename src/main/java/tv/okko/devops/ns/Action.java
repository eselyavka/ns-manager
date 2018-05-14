package tv.okko.devops.ns;

public enum Action {
    ENABLE, DISABLE;

    public static Action toAction(String name) throws Exception {
        if (name == null){
            throw new NullPointerException(" :: Parameter 'name' was null inside method toAction.");
        } else {
            return Action.valueOf(name.toUpperCase());
        }
    }
}