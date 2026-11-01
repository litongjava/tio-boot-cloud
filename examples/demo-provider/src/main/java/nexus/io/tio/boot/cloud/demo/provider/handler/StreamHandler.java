package nexus.io.tio.boot.cloud.demo.provider.handler;

import nexus.io.jfinal.aop.Aop;
import nexus.io.tio.boot.cloud.demo.provider.service.GreetingService;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.core.Tio;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.server.util.SseEmitter;

/** GET /demo/stream: five Server-Sent Events, one every 500 ms */
public class StreamHandler {
  public HttpResponse handle(HttpRequest request) throws Exception {
    HttpResponse response = TioRequestContext.getResponse();
    response.addServerSentEventsHeader();
    response.addHeader("Transfer-Encoding", "chunked");
    ChannelContext channelContext = request.channelContext;
    Tio.bSend(channelContext, response);
    response.setSend(false);

    String instance = Aop.get(GreetingService.class).instanceName();
    for (int i = 1; i <= 5; i++) {
      SseEmitter.pushSSEChunk(channelContext, "message", "{\"index\":" + i + ",\"instance\":\"" + instance + "\"}");
      Thread.sleep(500);
    }
    SseEmitter.closeChunkConnection(channelContext);
    return response;
  }
}
