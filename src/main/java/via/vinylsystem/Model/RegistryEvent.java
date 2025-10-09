package via.vinylsystem.Model;

public class RegistryEvent {
  private long tsMillis;
  private String ts;
  private String type;
  private String name;
  private String ip;
  private Long ttlSec;
  private String origin;
  private String details;


  public RegistryEvent(long tsMillis, String type, String name, String ip,
                        Long ttlSec, String origin, String details){
    this.tsMillis = tsMillis;
    this.ts = java.time.Instant.ofEpochMilli(tsMillis).toString();
    this.type = type;
    this.name = name;
    this.ip = ip;
    this.ttlSec = ttlSec;
    this.origin = origin; // "TCP", "UDP"
    this.details = details;
  }
  public String getTs()
  {
    return ts;
  }
  public long getTsMillis()
  {
    return tsMillis;
  }

  public String getType()
  {
    return type;
  }

  public String getName()
  {
    return name;
  }

  public String getIp()
  {
    return ip;
  }

  public String getOrigin()
  {
    return origin;
  }

  public Long getTtlSec()
  {
    return ttlSec;
  }

  public String getDetails()
  {
    return details;
  }
}
