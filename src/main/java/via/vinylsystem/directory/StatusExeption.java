package via.vinylsystem.directory;
//Formålet med denne exeption-klasse er hvis noget går galt i eks. RegistryService
//Vil man gerne kunne kaste en exeption med en bestemt statuskode, som kan bruges i hele Directory





public class StatusExeption extends Exception {
  private final String code;

  public StatusExeption(String code) {
    super(code);
    this.code = code;
  }

  public String getCode() {
    return code;
  }
}