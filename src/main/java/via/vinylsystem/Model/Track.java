package via.vinylsystem.Model;

public class Track
{
  private final String id;
  private final String artist;
  private final String title;
  private final int year;

  public Track(String id, String artist, String title, int year){
    this.id = id;
    this.artist = artist;
    this.title = title;
    this.year = year;
  }

  public String getId()
  {
    return id;
  }

  public String getArtist()
  {
    return artist;
  }

  public String getTitle()
  {
    return title;
  }

  public int getYear()
  {
    return year;
  }
  public String toString()
  {
    return id + " - " + artist + " - " + title + " (" + year + " )";
  }
}
