package via.vinylsystem.directory;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegistryService {
  private static final String INVALID_NAME_CODE = "000010";
  private static final String INVALID_IP_CODE = "000011";

  private final long defaultTtlSec;
  private final Clock clock;
  private final Map<String, Registration> byName;

  public RegistryService(long defaultTtlSec, Clock clock) {
    this.defaultTtlSec = defaultTtlSec;
    this.clock = clock;
    this.byName = new ConcurrentHashMap<>();
  }

  public synchronized long register(String name, String ip) throws StatusExeption {
    if (!validName(name)) throw new StatusExeption(INVALID_NAME_CODE);
    if (!validIPv4(ip)) throw new StatusExeption(INVALID_IP_CODE);
    Registration existing = byName.get(name);
    if (existing != null && !existing.getIp().equals(ip)) {
      throw new StatusExeption(StatusCodes.NAME_ON_OTHER_IP);
    }
    long now = clock.millis();
    long expires = now + defaultTtlSec * 1000;
    byName.put(name, new Registration(name, ip, expires));
    return defaultTtlSec;
  }

  public synchronized LookUpResult lookup(String name) throws StatusExeption {
    if (!validName(name)) throw new StatusExeption("000010"); // invalid name
    long now = clock.millis();
    Registration reg = byName.get(name);
    if (reg == null || reg.isExpired(now)) {
      if (reg != null && reg.isExpired(now)) {
        byName.remove(name);
      }
      throw new StatusExeption(StatusCodes.NONE_REGISTERED);
    }
    long ttl = reg.ttlSeconds(now);
    return new LookUpResult(reg.getName(), reg.getIp(),ttl);
  }


    public synchronized LookUpResult lookupByIp(String ip) throws StatusExeption {
        if (!validIPv4(ip)) throw new StatusExeption("000011"); // invalid IP
        long now = clock.millis();
        for (Registration reg : byName.values()) {
        if (reg.getIp().equals(ip)) {
            if (reg.isExpired(now)) {
            byName.remove(reg.getName());
            throw new StatusExeption(StatusCodes.NONE_REGISTERED);
            }
            long ttl = reg.ttlSeconds(now);
            return new LookUpResult(reg.getName(), reg.getIp(), ttl);
        }
        }
        throw new StatusExeption(StatusCodes.NONE_REGISTERED);
    }

    public synchronized long update(String name, String ip) throws StatusExeption {
        Registration entry = byName.get(name);
        if (entry == null) {
            throw new StatusExeption(StatusCodes.UPDATE_UNKNOWN);
        }
        if (!entry.getIp().equals(ip)) {
            throw new StatusExeption(StatusCodes.NAME_ON_OTHER_IP);
        }
        long now = clock.millis();
        entry.refreshTtl(now, defaultTtlSec);
        return entry.ttlSeconds(now);
    }


  private boolean validName(String name) {
    return name != null && !name.isEmpty() && name.length() <= 30 &&
        name.matches(".*\\.group[0-9]+\\.pro2(x|y)?$");
  }

  private boolean validIPv4(String ip) {
    if (ip == null) return false;
    String[] p = ip.split("\\.");
    if (p.length != 4) return false;
    for (String s : p) {
      if (!s.matches("\\d+")) return false;
      int v = Integer.parseInt(s);
      if (v < 0 || v > 255) return false;
    }
    return true;
  };



}