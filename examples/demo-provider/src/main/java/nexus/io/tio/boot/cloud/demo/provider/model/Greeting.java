package nexus.io.tio.boot.cloud.demo.provider.model;

public class Greeting {
  private String message;
  private String instance;

  public Greeting() {
  }

  public Greeting(String message, String instance) {
    this.message = message;
    this.instance = instance;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getInstance() {
    return instance;
  }

  public void setInstance(String instance) {
    this.instance = instance;
  }
}
