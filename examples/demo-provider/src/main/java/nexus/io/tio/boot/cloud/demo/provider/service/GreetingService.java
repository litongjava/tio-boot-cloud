package nexus.io.tio.boot.cloud.demo.provider.service;

import nexus.io.tio.boot.cloud.CloudContext;
import nexus.io.tio.boot.cloud.demo.provider.model.Greeting;
import nexus.io.tio.boot.cloud.discovery.ServiceInstance;
import nexus.io.tio.utils.environment.EnvUtils;

public class GreetingService {

  public Greeting greet(String name) {
    // demo.greeting comes from Nacos and changes without a restart
    String greeting = EnvUtils.get("demo.greeting", "Hello");
    return new Greeting(greeting + ", " + name, instanceName());
  }

  public String instanceName() {
    ServiceInstance local = CloudContext.getLocalInstance();
    if (local == null) {
      return "port " + EnvUtils.get("server.port");
    }
    return local.getHost() + ":" + local.getPort();
  }
}
