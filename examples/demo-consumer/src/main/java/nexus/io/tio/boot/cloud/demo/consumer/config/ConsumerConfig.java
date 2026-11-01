package nexus.io.tio.boot.cloud.demo.consumer.config;

import nexus.io.context.BootConfiguration;
import nexus.io.tio.boot.cloud.demo.consumer.handler.CallProviderHandler;
import nexus.io.tio.boot.cloud.nacos.TioBootNacos;
import nexus.io.tio.boot.server.TioBootServer;
import nexus.io.tio.http.server.router.HttpRequestRouter;

public class ConsumerConfig implements BootConfiguration {

  @Override
  public void config() throws Exception {
    TioBootNacos.start();
    HttpRequestRouter router = TioBootServer.me().getRequestRouter();
    router.get("/consumer/hello", new CallProviderHandler()::handle);
  }
}
