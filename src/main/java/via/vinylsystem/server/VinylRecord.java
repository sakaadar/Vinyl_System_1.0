package via.vinylsystem.server;

public class VinylRecord {
    private final String id;
    private final String artist;
    private final String title;
    private final int year;
    private final String genre;

    public VinylRecord(String id, String artist, String title, int year, String genre) {
        this.id = id;
        this.artist = artist;
        this.title = title;
        this.year = year;
        this.genre = genre;
    }

    public String getId() { return id; }
    public String getArtist() { return artist; }
    public String getTitle() { return title; }
    public int getYear() { return year; }
    public String getGenre() { return genre; }

    @Override
    public String toString() {
        return id + "|" + artist + "|" + title + "|" + year + "|" + genre;
    }
}