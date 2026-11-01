package nexus.io.tio.boot.cloud.demo.gateway.config;

import nexus.io.context.BootConfiguration;
import nexus.io.tio.boot.cloud.demo.gateway.filter.BlockListFilter;
import nexus.io.tio.boot.cloud.gateway.TioBootGateway;
import nexus.io.tio.boot.cloud.nacos.TioBootNacos;

public class GatewayConfig implements BootConfiguration {

  @Override
  public void config() throws Exception {
    TioBootNacos.start();
    TioBootGateway.start();
    TioBootGateway.addGlobalFilter(new BlockListFilter());
  }
}
