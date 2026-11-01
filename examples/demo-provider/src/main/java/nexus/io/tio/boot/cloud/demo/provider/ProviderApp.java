package nexus.io.tio.boot.cloud.demo.provider;

import nexus.io.tio.boot.TioApplication;
import nexus.io.tio.boot.cloud.demo.provider.config.ProviderConfig;
import nexus.io.tio.boot.cloud.nacos.TioBootNacos;

public class ProviderApp {
  public static void main(String[] args) {
    // pull demo-provider.yml from Nacos before startup
    TioBootNacos.loadConfig(args);
    TioApplication.run(ProviderApp.class, new ProviderConfig(), args);
  }
}
