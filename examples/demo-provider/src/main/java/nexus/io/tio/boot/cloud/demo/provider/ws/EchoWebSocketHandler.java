package nexus.io.tio.boot.cloud.demo.provider.ws;

import nexus.io.jfinal.aop.Aop;
import nexus.io.tio.boot.cloud.demo.provider.service.GreetingService;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.Tio;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.websocket.common.WebSocketRequest;
import nexus.io.tio.websocket.common.WebSocketResponse;
import nexus.io.tio.websocket.server.handler.IWebSocketHandler;

/** WebSocket /demo/ws: echoes text messages and tells which instance answered */
public class EchoWebSocketHandler implements IWebSocketHandler {

  @Override
  public HttpResponse handshake(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext) {
    return httpResponse;
  }

  @Override
  public void onAfterHandshaked(HttpRequest httpRequest, HttpResponse httpResponse, ChannelContext channelContext) {
    String instance = Aop.get(GreetingService.class).instanceName();
    Tio.send(channelContext, WebSocketResponse.fromText("connected to " + instance, "utf-8"));
  }

  @Override
  public Object onBytes(WebSocketRequest wsRequest, byte[] bytes, ChannelContext channelContext) {
    return bytes;
  }

  @Override
  public Object onClose(WebSocketRequest wsRequest, byte[] bytes, ChannelContext channelContext) {
    Tio.remove(channelContext, "client closed");
    return null;
  }

  @Override
  public Object onText(WebSocketRequest wsRequest, String text, ChannelContext channelContext) {
    return "echo: " + text;
  }
}
