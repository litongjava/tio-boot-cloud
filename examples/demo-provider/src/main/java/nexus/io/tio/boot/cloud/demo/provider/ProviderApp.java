package nexus.io.tio.boot.cloud.demo.provider;

import nexus.io.tio.boot.TioApplication;
import nexus.io.tio.boot.cloud.demo.provider.config.ProviderConfig;

public class ProviderApp {
  public static void main(String[] args) {
    TioApplication.run(ProviderApp.class, new ProviderConfig(), args);
  }
}
