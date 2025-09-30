package via.vinylsystem.Model;

public class Registration
{
  //Registration repræsentere en bestemt Vinyl-Server med et navn som kører
  //På en bestemt IP og er gyldigt indtil udløbstidspunktet.

  private String name;
  private String ip;
  private long expiresAtMillis;

  public Registration(String name, String ip, long expiresAtMillis)
  {
    this.name = name;
    this.ip = ip;
    this.expiresAtMillis = expiresAtMillis;
  }

  public String getName()
  {
    return name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getIp()
  {
    return ip;
  }

  public void setIp(String ip)
  {
    this.ip = ip;
  }

  public long getExpiresAtMillis()
  {
    return expiresAtMillis;
  }

  public void setExpiresAtMillis(long expiresAtMillis)
  {
    this.expiresAtMillis = expiresAtMillis;
  }

  public long ttlSeconds(long nowMillis)
  {
    long remaining = (expiresAtMillis- nowMillis)/1000;
    if(remaining <= 0)
    {
      return 0;
    }
    return remaining;
  }

  //hjælpefunktion til at se om registration er udløbet
  public boolean isExpired(long nowMillis)
  {
    if(expiresAtMillis <= nowMillis)
    {
      return true;
    }
    return false;
  }
  //hjælpefunktion til at få en ny registration
  public Registration withNewRegis(long newExpiresAtMillis)
  {
    return new Registration(name,ip, newExpiresAtMillis);
  }

  public String toString()
  {
    return "Registration{name= " + name + " , ip=" + ip +
        " , expiresAt=" + expiresAtMillis + "}";
  }
}
