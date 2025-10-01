package via.vinylsystem.server;

import java.util.List;
import java.util.stream.Collectors;

public class VinylCatalog {
    private final List<VinylRecord> records = List.of(
        new VinylRecord("1", "Pink Floyd", "The Dark Side of the Moon", 1973, "Progressive Rock"),
            new VinylRecord("2", "Bob Dylan", "Like a Rolling Stone", 1965, "Folk Rock"),
            new VinylRecord("3", "Led Zeppelin", "Stairway to Heaven", 1971, "Rock"),
            new VinylRecord("4", "Nirvana", "Smells Like Teen Spirit", 1991, "Grunge"),
            new VinylRecord("5", "Queen", "Bohemian Rhapsody", 1975, "Rock"),
            new VinylRecord("6", "The Beatles", "Hey Jude", 1968, "Rock"),
            new VinylRecord("7", "The Rolling Stones", "Gimme Shelter", 1969, "Rock"),
            new VinylRecord("8", "David Bowie", "Heroes", 1977, "Art Rock"),
            new VinylRecord("9", "Fleetwood Mac", "Go Your Own Way", 1977, "Rock"),
            new VinylRecord("10", "Prince", "Purple Rain", 1984, "Pop"),
            new VinylRecord("11", "Bruce Springsteen", "Born to Run", 1975, "Rock"),
            new VinylRecord("12", "U2", "With or Without You", 1987, "Rock"),
            new VinylRecord("13", "The Clash", "London Calling", 1979, "Punk Rock"),
            new VinylRecord("14", "Eagles", "Hotel California", 1976, "Rock"),
            new VinylRecord("15", "AC/DC", "Back in Black", 1980, "Hard Rock"),
            new VinylRecord("16", "Radiohead", "Creep", 1992, "Alternative"),
            new VinylRecord("17", "The Who", "Baba O'Riley", 1971, "Rock"),
            new VinylRecord("18", "Simon & Garfunkel", "Bridge Over Troubled Water", 1970, "Folk"),
            new VinylRecord("19", "Michael Jackson", "Billie Jean", 1982, "Pop"),
            new VinylRecord("20", "Metallica", "Master of Puppets", 1986, "Metal")
    );

    public List<VinylRecord> search(String artist, String title) {
        String a = artist == null ? null : artist.trim().toLowerCase();
        String t = title == null ? null : title.trim().toLowerCase();
        return records.stream()
            .filter(r -> a == null || r.getArtist().toLowerCase().contains(a))
            .filter(r -> t == null || r.getTitle().toLowerCase().contains(t))
            .collect(Collectors.toList());
    }
}