package nexus.io.tio.boot.cloud.demo.gateway;

import nexus.io.tio.boot.TioApplication;
import nexus.io.tio.boot.cloud.demo.gateway.config.GatewayConfig;
import nexus.io.tio.boot.cloud.nacos.TioBootNacos;

public class GatewayApp {
  public static void main(String[] args) {
    TioBootNacos.loadConfig(args);
    TioApplication.run(GatewayApp.class, new GatewayConfig(), args);
  }
}
