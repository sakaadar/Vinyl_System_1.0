package via.vinylsystem.Util;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * Utility class for JSON operations and related helper methods.
 * <p>
 * Provides methods for parsing JSON strings into maps, converting maps to JSON,
 * formatting numbers as 6-digit strings, safely reading lines from streams,
 * writing JSON payloads, closing sockets/connections, and clamping TTL values.
 * </p>
 * <p>
 * All methods are static and can be called without creating an instance.
 * </p>
 *
 * @author Ghiyath & sakariae
 * @version 1.0
 */
public class JsonUtils
{
    /** Gson instance for JSON serialization and deserialization. */
    public static final Gson gson = new Gson();

    /**
     * Attempts to parse a JSON string into a {@code Map<String, String>}.
     * <p>
     * If parsing fails or the input is not valid JSON, returns {@code null}.
     * All values in the returned map are converted to strings.
     * </p>
     *
     * @param text the JSON string to parse
     * @return a map of key-value pairs, or {@code null} if parsing fails
     */
    public static Map<String, String> tryParseJsonMap(String text){
        try
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = gson.fromJson(text, Map.class);
            if (raw == null)
                return null;

            Map<String, String> out = new java.util.HashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet())
            {
                out.put(entry.getKey(),
                        entry.getValue() == null ? null : String.valueOf(entry.getValue()));
            }
            return out;
        } catch (JsonSyntaxException ex)
        {
            return null;
        }
    }

    /**
     * Converts a map of string key-value pairs into a JSON string.
     *
     * @param map the map to convert
     * @return JSON representation of the map
     */
    public static String toJson(Map<String,String> map){
        return gson.toJson(map);
    }

    /**
     * Formats a number as a zero-padded 6-digit string.
     *
     * @param n the number to format
     * @return a string with exactly 6 digits, padded with leading zeros if necessary
     */
    public static String format6(long n)
    {
        return String.format("%06d",n);
    }

    /**
     * Reads a line from a {@link BufferedReader} and enforces a maximum length.
     * <p>
     * Trims whitespace from the returned string. Returns {@code null} if
     * the line is {@code null} or exceeds the specified maximum length.
     * </p>
     *
     * @param reader the BufferedReader to read from
     * @param maxLen the maximum allowed line length
     * @return the trimmed line string, or {@code null} if it is too long or EOF
     */
    public static String readLineWithLimit(BufferedReader reader,long maxLen){
        try
        {
            String line = reader.readLine();
            if(line == null)
            {
                return null;
            }
            if(line.length() > maxLen)
            {
                return null;
            }
            return line.trim();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not read line: "+e);
        }
    }

    /**
     * Writes a map as a JSON line to a {@link BufferedWriter} and flushes the stream.
     *
     * @param writer the BufferedWriter to write to
     * @param payload the map to serialize as JSON
     * @throws IOException if an I/O error occurs
     */
    public static void writeJsonLine(BufferedWriter writer,  Map<String, ?> payload)
            throws IOException
    {
        String json = gson.toJson(payload);
        writer.write(json);
        writer.newLine();
        writer.flush();
    }

    /**
     * Safely closes a {@link Closeable} resource, such as a socket or stream.
     * <p>
     * Ignores any {@link IOException} thrown during closing.
     * </p>
     *
     * @param c the resource to close, may be {@code null}
     */
    public static void closeSocketCon(Closeable c)
    {
        try{
            if(c != null)
            {
                c.close();
            }
        }
        catch (IOException e)
        {
            // ignore
        }
    }

    /**
     * Clamps a TTL value to a 6-digit string representation.
     * <p>
     * Values are clamped to the range 0–999999 and returned as a zero-padded string.
     * </p>
     *
     * @param ttlSec the TTL value in seconds
     * @return a zero-padded 6-digit string representing the TTL
     */
    public static String ttl6(int ttlSec)
    {
        int clamped = Math.max(0, Math.min(ttlSec, 999_999));
        return format6(clamped);
    }
}
