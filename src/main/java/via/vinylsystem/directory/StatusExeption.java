package via.vinylsystem.directory;

/**
 * Custom exception class for status code-based error handling in the directory service.
 * <p>
 * This exception is used throughout the directory system to signal errors with specific
 * status codes. When thrown, it carries a status code (from {@link StatusCodes}) that
 * can be returned to clients in the protocol response.
 * </p>
 * <p>
 * This approach provides a standardized way to handle errors across the directory service,
 * ensuring that all errors can be communicated to clients with appropriate status codes
 * defined in the directory protocol.
 * </p>
 * <p>
 * Example usage:
 * <pre>
 * if (registration == null) {
 *     throw new StatusExeption(StatusCodes.NONE_REGISTERED);
 * }
 * </pre>
 * </p>
 *
 * @author Ghiyath & sakariae
 * @version 1.0
 * @see StatusCodes
 */
public class StatusExeption extends Exception
{
    private String code;

    /**
     * Constructs a new StatusExeption with the specified status code.
     * <p>
     * The status code should typically be one of the constants defined in
     * {@link StatusCodes}, such as {@code StatusCodes.UNKNOWN_CMD} or
     * {@code StatusCodes.NAME_ON_OTHER_IP}.
     * </p>
     *
     * @param code the status code representing the error condition
     * @see StatusCodes
     */
    public StatusExeption(String code)
    {
        this.code = code;
    }

    /**
     * Returns the status code associated with this exception.
     * <p>
     * This code can be used to determine the specific error condition and
     * to construct an appropriate response to send back to the client.
     * </p>
     *
     * @return the status code string (e.g., "000001", "000002")
     */
    public String getCode()
    {
        return this.code;
    }
}