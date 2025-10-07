package via.vinylsystem.Model;

/**
 * Represents a service registration in the directory system.
 * <p>
 * A registration associates a service name with an IP address and maintains
 * an expiration timestamp. This class is immutable except through the creation
 * of new instances via the {@link #withNewRegis(long)} method.
 * </p>
 * <p>
 * Each registration represents a specific Vinyl-Server service that is running
 * at a particular IP address and remains valid until its expiration time.
 * </p>
 *
 * @author Ghiyath & sakariae
 * @version 1.0
 */
public class Registration
{
    private String name;
    private String ip;
    private long expiresAtMillis;

    /**
     * Constructs a new Registration with the specified parameters.
     *
     * @param name the service name (must follow the pattern *.group[0-9]+.pro2(x|y)?)
     * @param ip the IPv4 address where the service is running
     * @param expiresAtMillis the expiration timestamp in milliseconds since epoch
     */
    public Registration(String name, String ip, long expiresAtMillis)
    {
        this.name = name;
        this.ip = ip;
        this.expiresAtMillis = expiresAtMillis;
    }

    /**
     * Returns the service name for this registration.
     *
     * @return the service name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the IP address for this registration.
     *
     * @return the IPv4 address
     */
    public String getIp()
    {
        return ip;
    }

    /**
     * Returns the expiration timestamp for this registration.
     *
     * @return the expiration time in milliseconds since epoch
     */
    public long getExpiresAtMillis()
    {
        return expiresAtMillis;
    }

    /**
     * Calculates the remaining time-to-live (TTL) in seconds.
     * <p>
     * Computes the number of seconds remaining until this registration expires,
     * based on the provided current time. Returns 0 if the registration has
     * already expired.
     * </p>
     *
     * @param nowMillis the current time in milliseconds since epoch
     * @return the remaining TTL in seconds, or 0 if expired
     */
    public long ttlSeconds(long nowMillis)
    {
        long remaining = (expiresAtMillis- nowMillis)/1000;
        if(remaining <= 0)
        {
            return 0;
        }
        return remaining;
    }

    /**
     * Checks whether this registration has expired.
     * <p>
     * Compares the expiration timestamp with the provided current time to
     * determine if this registration is still valid.
     * </p>
     *
     * @param nowMillis the current time in milliseconds since epoch
     * @return true if the registration has expired, false otherwise
     */
    public boolean isExpired(long nowMillis)
    {
        if(expiresAtMillis <= nowMillis)
        {
            return true;
        }
        return false;
    }

    /**
     * Creates a new Registration with an updated expiration time.
     * <p>
     * This method provides a way to renew a registration by creating a new
     * instance with the same name and IP but a different expiration timestamp.
     * The original registration remains unchanged.
     * </p>
     *
     * @param newExpiresAtMillis the new expiration timestamp in milliseconds since epoch
     * @return a new Registration instance with the updated expiration time
     */
    public Registration withNewRegis(long newExpiresAtMillis)
    {
        return new Registration(name,ip, newExpiresAtMillis);
    }

    /**
     * Returns a string representation of this registration.
     * <p>
     * The format includes the service name, IP address, and expiration timestamp.
     * </p>
     *
     * @return a string representation in the format "Registration{name=..., ip=..., expiresAt=...}"
     */
    public String toString()
    {
        return "Registration{name= " + name + " , ip=" + ip +
                " , expiresAt=" + expiresAtMillis + "}";
    }
}