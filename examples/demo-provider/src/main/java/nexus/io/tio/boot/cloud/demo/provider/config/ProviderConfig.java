package nexus.io.tio.boot.cloud.demo.provider.config;

import nexus.io.context.BootConfiguration;
import nexus.io.tio.boot.cloud.demo.provider.handler.HelloHandler;
import nexus.io.tio.boot.cloud.demo.provider.handler.StreamHandler;
import nexus.io.tio.boot.cloud.demo.provider.ws.EchoWebSocketHandler;
import nexus.io.tio.boot.cloud.nacos.TioBootNacos;
import nexus.io.tio.boot.server.TioBootServer;
import nexus.io.tio.boot.websocket.WebSocketRouter;
import nexus.io.tio.http.server.router.HttpRequestRouter;


public class ProviderConfig implements BootConfiguration {

  @Override
  public void config() throws Exception {
    TioBootNacos.start();

    HttpRequestRouter router = TioBootServer.me().getRequestRouter();
    router.get("/demo/hello", new HelloHandler()::handle);
    router.get("/demo/stream", new StreamHandler()::handle);

    WebSocketRouter webSocketRouter = TioBootServer.me().getWebSocketRouter();
    webSocketRouter.add("/demo/ws", new EchoWebSocketHandler());
  }
}
