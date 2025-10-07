package via.vinylsystem.Model;

/**
 * Represents a music track in the Vinyl System.
 * <p>
 * This immutable class encapsulates the essential information about a music track,
 * including its unique identifier, artist name, title, and release year. Once created,
 * a Track object cannot be modified.
 * </p>
 * <p>
 * Track objects are used throughout the Vinyl System to represent individual songs
 * that can be stored, searched, and played.
 * </p>
 *
 * @author Ghiyath & sakariae
 * @version 1.0
 */
public class Track
{
    private final String id;
    private final String artist;
    private final String title;
    private final int year;

    /**
     * Constructs a new Track with the specified details.
     * <p>
     * All fields are immutable after construction.
     * </p>
     *
     * @param id the unique identifier for this track
     * @param artist the name of the artist or band
     * @param title the title of the track
     * @param year the release year of the track
     */
    public Track(String id, String artist, String title, int year){
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.year = year;
    }

    /**
     * Returns the unique identifier for this track.
     *
     * @return the track ID
     */
    public String getId()
    {
        return id;
    }

    /**
     * Returns the artist name for this track.
     *
     * @return the artist or band name
     */
    public String getArtist()
    {
        return artist;
    }

    /**
     * Returns the title of this track.
     *
     * @return the track title
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Returns the release year of this track.
     *
     * @return the year the track was released
     */
    public int getYear()
    {
        return year;
    }

    /**
     * Returns a string representation of this track.
     * <p>
     * The format is: "id - artist - title (year)"
     * </p>
     * <p>
     * Example: "001 - The Beatles - Hey Jude (1968)"
     * </p>
     *
     * @return a formatted string containing the track's details
     */
    public String toString()
    {
        return id + " - " + artist + " - " + title + " (" + year + " )";
    }
}