package via.vinylsystem.directory;

public class LookUpResult {
    private final String name;
    private final String ip;
    private final long ttlSeconds;

    public LookUpResult(String name, String ip, long ttlSeconds) {
        this.name = name;
        this.ip = ip;
        this.ttlSeconds = ttlSeconds;
    }

    public String getName() { return name; }
    public String getIp() { return ip; }
    public long getTtlSeconds() { return ttlSeconds; }
}