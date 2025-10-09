package via.vinylsystem.directory;

import via.vinylsystem.Model.Registration;
import via.vinylsystem.Model.RegistryEvent;
import via.vinylsystem.Model.RegistryEventType;
import via.vinylsystem.Util.AuditLog;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core registry service for managing service registrations in the directory system.
 * <p>
 * This service maintains a registry of service names mapped to IP addresses with
 * time-to-live (TTL) expiration. It provides operations to register new services,
 * update existing registrations, and lookup services by name or IP address.
 * </p>
 * <p>
 * All operations are thread-safe using synchronized methods. Expired registrations
 * are automatically removed during lookup operations.
 * </p>
 * <p>
 * Service names must follow the pattern: *.group[0-9]+.pro2(x|y)? with a maximum
 * length of 30 characters.
 * </p>
 *
 * @author Ghiyath & sakariae
 * @version 1.0
 */
public class RegistryService
{
    private long defaultTtlSec;
    private Clock clock;
    private Map<String, Registration> byName;
    private Map<String,String> nameByIp;
    private final AuditLog audit;

    /**
     * Constructs a new RegistryService.
     *
     * @param defaultTtlSec the default time-to-live in seconds for new registrations
     * @param clock the clock instance used for time-based operations and expiration checks
     */
    RegistryService(long defaultTtlSec, Clock clock, AuditLog audit)
    {
        this.defaultTtlSec = defaultTtlSec;
        this.clock = clock;
        this.byName = new HashMap<>();
        this.nameByIp = new HashMap<>();
        this.audit = audit;
    }

    /**
     * Registers a new service with the specified name and IP address.
     * <p>
     * Creates a new registration entry with the default TTL. The service name must
     * be valid and not already registered to a different IP address.
     * </p>
     *
     * @param name the service name to register (must match the pattern *.group[0-9]+.pro2(x|y)?)
     * @param ip the IPv4 address to associate with the service name
     * @return the TTL in seconds for this registration
     * @throws StatusExeption if the name or IP is invalid, or if the name is already
     *                        registered to a different IP address
     */
    public synchronized long register(String name, String ip)
            throws StatusExeption
    {
        inputvalidation(name, ip);
        //Opret registrering

        long now = clock.millis();

        long expiresAt = now + defaultTtlSec * 1000L;

        Registration reg = new Registration(name, ip, expiresAt);

        byName.put(name,reg);
        nameByIp.put(ip,name);

        audit.append(new RegistryEvent(now, RegistryEventType.REGISTER,name,ip,defaultTtlSec,"TCP","OK")); //LOG

        //Retunere TTL i sekunder
        return defaultTtlSec;

    }

    /**
     * Updates an existing service registration, renewing its TTL.
     * <p>
     * The service must already be registered with the same name and IP address.
     * This operation extends the expiration time by the default TTL from the current time.
     * </p>
     *
     * @param name the service name to update
     * @param ip the IPv4 address (must match the existing registration)
     * @return the new TTL in seconds for this registration
     * @throws StatusExeption if the name is not registered (UPDATE_UNKNOWN), or if the
     *                        IP address doesn't match the existing registration (NAME_ON_OTHER_IP)
     */
    public synchronized long update(String name, String ip) throws StatusExeption
    {
        inputvalidation(name, ip);

        //hent eksisterende

        Registration existing = byName.get(name);
        if(existing == null)
        {
            throw new StatusExeption(StatusCodes.UPDATE_UNKNOWN);
        }
        if(!existing.getIp().equals(ip))
        {
            throw new StatusExeption(StatusCodes.NAME_ON_OTHER_IP);
        }

        long now = clock.millis();

        long expiresAt = now + defaultTtlSec * 1000L;
        Registration renewed = new Registration(name,ip, expiresAt);

        //Gem i begge maps
        byName.put(name,renewed);
        nameByIp.put(ip,name);

        audit.append(new RegistryEvent(now,RegistryEventType.RENEW,name,ip,defaultTtlSec,"TCP", "OK"));

        //Retunere TTL
        return defaultTtlSec;

    }

    /**
     * Finds a registration by service name.
     * <p>
     * Removes all expired registrations before performing the lookup.
     * </p>
     *
     * @param name the service name to look up
     * @return the Registration object for the specified name
     * @throws StatusExeption if no registration exists for the given name (NONE_REGISTERED)
     */
    public synchronized Registration findByName(String name) throws StatusExeption
    {
        removeExpiredNow();

        Registration reg = byName.get(name);
        if(reg == null)
        {
            throw new StatusExeption(StatusCodes.NONE_REGISTERED);
        }
        return reg;
    }

    /**
     * Finds a registration by IP address.
     * <p>
     * Removes all expired registrations before performing the lookup. First maps
     * the IP to a service name, then retrieves the full registration.
     * </p>
     *
     * @param ip the IPv4 address to look up
     * @return the Registration object associated with the specified IP
     * @throws StatusExeption if no registration exists for the given IP (NONE_REGISTERED)
     */
    public synchronized Registration findByIp(String ip) throws StatusExeption
    {
        removeExpiredNow();

        String name = nameByIp.get(ip);
        if(name==null)
        {
            throw new StatusExeption(StatusCodes.NONE_REGISTERED);
        }
        Registration reg = byName.get(name);
        if(reg == null)
        {
            throw new StatusExeption(StatusCodes.NONE_REGISTERED);
        }
        return reg;
    }

    /**
     * Validates input parameters for registration operations.
     * <p>
     * Checks that the name matches the required pattern, the IP is a valid IPv4 address,
     * and that the name is not already registered to a different IP.
     * </p>
     *
     * @param name the service name to validate
     * @param ip the IP address to validate
     * @throws StatusExeption if validation fails (UNKNOWN_CMD or NAME_ON_OTHER_IP)
     */
    private void inputvalidation(String name, String ip) throws StatusExeption
    {
        if(!validName(name)){
            throw new StatusExeption(StatusCodes.UNKNOWN_CMD);
        }
        if(ip == null || !validIPv4(ip))
        {
            throw new StatusExeption(StatusCodes.UNKNOWN_CMD);
        }
        Registration existing = byName.get(name);
        if(existing != null && !existing.getIp().equals(ip))
        {
            throw new StatusExeption(StatusCodes.NAME_ON_OTHER_IP);
        }
    }

    /**
     * Removes all expired registrations from the registry.
     * <p>
     * Iterates through all registrations and removes those that have expired based
     * on the current time from the clock. Removes entries from both the byName and
     * nameByIp maps to maintain consistency.
     * </p>
     */
    public synchronized void removeExpiredNow()
    {
        long now = clock.millis();
        List<String> expiredNames = new ArrayList<>();
        for (Map.Entry<String, Registration> entry : byName.entrySet())
        {
            Registration registration = entry.getValue();
            if (registration.isExpired(now))
            {
                expiredNames.add(entry.getKey());
            }
        }
        for (String name : expiredNames)
        {
            Registration registration = byName.remove(name);
            if (registration != null)
            {
                nameByIp.remove(registration.getIp());
            }
        }
    }

    /**
     * Validates a service name against the required pattern and length constraints.
     * <p>
     * Valid names must:
     * <ul>
     *   <li>Be between 1 and 30 characters long</li>
     *   <li>Match the pattern: *.group[0-9]+.pro2(x|y)?</li>
     * </ul>
     * </p>
     *
     * @param name the service name to validate
     * @return true if the name is valid, false otherwise
     */
    public static boolean validName(String name)
    {
        if(name.length() > 30 || name.isEmpty())
        {
            return false;
        }
        else
            return name.matches(".*\\.group[0-9]+\\.pro2(x|y)?$");
    }

    /**
     * Validates an IPv4 address format.
     * <p>
     * Valid IPv4 addresses must consist of exactly four numeric octets separated
     * by dots, where each octet is between 0 and 255 inclusive.
     * </p>
     *
     * @param ip the IP address string to validate
     * @return true if the IP is a valid IPv4 address, false otherwise
     */
    public boolean validIPv4(String ip)
    {
        if(ip == null || ip.isEmpty())
        {
            return false;
        }
        String[] parts = ip.split("\\.");
        if(parts.length != 4){
            return false;
        }
        for(String part : parts)
        {
            if(!part.matches("\\d+")) // \\d betyder "en eller flere cifre"
            {
                return false;
            }
            int value = Integer.parseInt(part);
            if(value < 0 || value > 255)
            {
                return false;
            }
        }
        return true;
    }
}