package nexus.io.tio.boot.cloud.demo.consumer.handler;

import nexus.io.jfinal.aop.Aop;
import nexus.io.model.body.RespBodyVo;
import nexus.io.tio.boot.cloud.demo.consumer.model.ProviderGreeting;
import nexus.io.tio.boot.cloud.demo.consumer.service.ProviderService;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.utils.hutool.StrUtil;

/** GET /consumer/hello?name=tio */
public class CallProviderHandler {
  public HttpResponse handle(HttpRequest request) {
    String name = request.getParam("name");
    if (StrUtil.isBlank(name)) {
      name = "tio-boot";
    }
    ProviderService providerService = Aop.get(ProviderService.class);
    ProviderGreeting greeting = providerService.hello(name);
    return TioRequestContext.getResponse().respond(RespBodyVo.ok(greeting));
  }
}
