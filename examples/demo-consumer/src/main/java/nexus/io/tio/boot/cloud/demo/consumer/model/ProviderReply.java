package nexus.io.tio.boot.cloud.demo.consumer.model;

public class ProviderReply {
  private int code;
  private String msg;
  private ProviderGreeting data;

  public String getMsg() {
    return msg;
  }

  public void setMsg(String msg) {
    this.msg = msg;
  }

  public int getCode() {
    return code;
  }

  public void setCode(int code) {
    this.code = code;
  }

  public ProviderGreeting getData() {
    return data;
  }

  public void setData(ProviderGreeting data) {
    this.data = data;
  }
}
