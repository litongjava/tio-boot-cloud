package nexus.io.tio.boot.cloud.demo.provider.handler;

import nexus.io.jfinal.aop.Aop;
import nexus.io.model.body.RespBodyVo;
import nexus.io.tio.boot.cloud.demo.provider.model.Greeting;
import nexus.io.tio.boot.cloud.demo.provider.service.GreetingService;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;

/** GET /demo/hello?name=tio */
public class HelloHandler {
  public HttpResponse handle(HttpRequest request) {
    String name = request.getParam("name", "tio-boot");
    GreetingService greetingService = Aop.get(GreetingService.class);
    Greeting greeting = greetingService.greet(name);
    return TioRequestContext.getResponse().respond(RespBodyVo.ok(greeting));
  }
}
