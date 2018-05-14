package tv.okko.devops.ns;

public class AwaitTimeExhausted extends RuntimeException {
    public AwaitTimeExhausted (String message) {
        super(message);
    }
}
