package via.vinylsystem.directory;

public class StatusCodes
{
  public static final String UNKNOWN_CMD = "000001";
  public static final String NAME_ON_OTHER_IP = "000002";
  public static final String UPDATE_UNKNOWN = "000003";
  public static final String NONE_REGISTERED = "000100";


    private final int code;
    private final String message;

    StatusCodes(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
