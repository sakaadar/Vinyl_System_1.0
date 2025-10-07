package via.vinylsystem.directory;

/**
 * Status code constants used throughout the directory service system.
 * <p>
 * This class defines standardized status codes for various operation outcomes
 * in the directory service protocol. Status codes are represented as 6-digit
 * strings (with one exception) to maintain consistency in the JSON protocol.
 * </p>
 * <p>
 * Status codes are organized by category:
 * <ul>
 *   <li>000000 - Success</li>
 *   <li>0000XX - Command/request errors</li>
 *   <li>0001XX - Registry lookup errors</li>
 *   <li>0002XX - Bad request errors</li>
 *   <li>0005XX - Server errors</li>
 * </ul>
 * </p>
 *
 * @author Ghiyath & sakariae
 * @version 1.0
 */
public class StatusCodes
{
    /**
     * Operation completed successfully.
     * <p>
     * Returned when a registration, update, or lookup operation completes without errors.
     * </p>
     */
    public static final String OK = "000000";

    /**
     * Unknown or invalid command.
     * <p>
     * Returned when the server receives a command it doesn't recognize, or when
     * required parameters are missing or invalid.
     * </p>
     */
    public static final String UNKNOWN_CMD = "000001";

    /**
     * Service name already registered to a different IP address.
     * <p>
     * Returned when attempting to register or update a service name that is already
     * associated with a different IP address.
     * </p>
     */
    public static final String NAME_ON_OTHER_IP = "000002";

    /**
     * Update requested for an unknown service.
     * <p>
     * Returned when attempting to update a service that has not been registered.
     * The service must be registered first before it can be updated.
     * </p>
     */
    public static final String UPDATE_UNKNOWN = "000003";

    /**
     * No registration found for the requested name or IP.
     * <p>
     * Returned when a lookup operation cannot find a matching registration
     * in the directory service.
     * </p>
     */
    public static final String NONE_REGISTERED = "000100";

    /**
     * Internal server error occurred.
     * <p>
     * Returned when an unexpected error occurs during request processing.
     * </p>
     */
    public static final String SERVER_ERROR = "000500";

    /**
     * Malformed or invalid request.
     * <p>
     * Returned when the request format is incorrect or cannot be processed.
     * </p>
     */
    public static final String BAD_REQUEST = "000200";

    /**
     * Resource not found.
     * <p>
     * Returned when the requested resource does not exist.
     * Note: This is a 7-digit code, unlike the other 6-digit codes.
     * </p>
     */
    public static final String NOT_FOUND = "0001000";
}