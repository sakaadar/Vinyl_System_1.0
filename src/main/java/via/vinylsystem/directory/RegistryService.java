package via.vinylsystem.directory;

import java.time.Clock;
import java.util.Map;
import java.util.regex.Pattern;

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
    //Opret/forny registrering

    Long now = clock.millis();

    Long expiresAt = now + defaultTtlSec * 1000;

    Registration reg = new Registration(name, ip, expiresAt);

    byName.put(name,reg);
    nameByIp.put(ip,name);

    //Retunere TTL i sekunder
    return defaultTtlSec;

  }

  public boolean validName(String name)
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
