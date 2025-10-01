package via.vinylsystem.directory;

import via.vinylsystem.Model.Registration;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RegistryService
{
  private long defaultTtlSec;
  private Clock clock;
  private Map<String, Registration> byName; //Registration
  private Map<String,String> nameByIp; // ip/Name
  RegistryService(long defaultTtlSec, Clock clock)
  {
    this.defaultTtlSec = defaultTtlSec;
    this.clock = clock;
  }
  public synchronized long register(String name, String ip)
      throws StatusExeption
  {
    inputvalidation(name, ip);
    //Opret/forny registrering

    long now = clock.millis();

    long expiresAt = now + defaultTtlSec * 1000L;

    Registration reg = new Registration(name, ip, expiresAt);

    byName.put(name,reg);
    nameByIp.put(ip,name);

    //Retunere TTL i sekunder
    return defaultTtlSec;

  }
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

    //Retunere TTL
    return defaultTtlSec;

  }
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

  //DRY metode sker i både Register og Update
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

  public static boolean validName(String name)
  {
    if(name.length() > 30 || name.isEmpty())
    {
      return false;
    }
    else
      return name.matches(".*\\.group[0-9]+\\.pro2(x|y)?$");
  }

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
