package nexus.io.tio.boot.cloud.demo.gateway;

import nexus.io.tio.boot.TioApplication;
import nexus.io.tio.boot.cloud.demo.gateway.config.GatewayConfig;

public class GatewayApp {
  public static void main(String[] args) {
    TioApplication.run(GatewayApp.class, new GatewayConfig(), args);
  }
}
