package via.vinylsystem.server;


public class Vinyl {
    private String artist;
    private String title;
    private int publicationYear;

    public Vinyl(String artist, String title, int publicationYear) {
        this.artist = artist;
        this.title = title;
        this.publicationYear = publicationYear;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    // A method to convert the object to a JSON-like string
    public String toJson() {
        return String.format("{\"artist\":\"%s\", \"title\":\"%s\", \"publicationYear\":%d}",
                artist, title, publicationYear);
    }
}
