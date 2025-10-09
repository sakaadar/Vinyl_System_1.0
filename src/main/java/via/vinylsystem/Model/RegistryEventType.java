package via.vinylsystem.Model;

public class RegistryEventType
{
  private RegistryEventType(){}
  public static final String REGISTER = "REGISTER";
  public static final String RENEW      = "RENEW";
  public static final String EXPIRE     = "EXPIRE";
  public static final String LOOKUP     = "LOOKUP";
  public static final String INVALIDATE = "INVALIDATE";
  public static final String ERROR      = "ERROR";
}
