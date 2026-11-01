package nexus.io.tio.boot.cloud.demo.consumer.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import nexus.io.tio.boot.cloud.client.ServiceClient;
import nexus.io.tio.boot.cloud.client.ServiceRequest;
import nexus.io.tio.boot.cloud.client.ServiceResponse;
import nexus.io.tio.boot.cloud.demo.consumer.model.ProviderGreeting;
import nexus.io.tio.boot.cloud.demo.consumer.model.ProviderReply;

/** Calls demo-provider by service name; Nacos supplies the instances */
public class ProviderService {
  private static final String PROVIDER = "demo-provider";

  public ProviderGreeting hello(String name) {
    String path = "/demo/hello?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
    ServiceResponse response = ServiceClient.me().call(PROVIDER, ServiceRequest.get(path));
    ProviderReply reply = response.json(ProviderReply.class);
    return reply == null ? null : reply.getData();
  }
}
