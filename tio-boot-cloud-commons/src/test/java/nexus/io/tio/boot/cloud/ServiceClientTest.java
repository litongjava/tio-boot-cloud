package nexus.io.tio.boot.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.net.httpserver.HttpServer;

import nexus.io.tio.boot.cloud.client.ServiceCallException;
import nexus.io.tio.boot.cloud.client.ServiceClient;
import nexus.io.tio.boot.cloud.client.ServiceRequest;
import nexus.io.tio.boot.cloud.client.ServiceResponse;
import nexus.io.tio.boot.cloud.discovery.SimpleDiscoveryClient;
import nexus.io.tio.boot.cloud.loadbalancer.RoundRobinLoadBalancer;
import okhttp3.OkHttpClient;

public class ServiceClientTest {
  private HttpServer one;
  private HttpServer two;

  @Before
  public void setUp() throws IOException {
    one = start("one");
    two = start("two");
    CloudContext.setLoadBalancer(new RoundRobinLoadBalancer());
  }

  @After
  public void tearDown() {
    one.stop(0);
    two.stop(0);
    CloudContext.setDiscoveryClient(new SimpleDiscoveryClient());
  }

  private static HttpServer start(String name) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      byte[] in;
      try (InputStream body = exchange.getRequestBody()) {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int n;
        while ((n = body.read(chunk)) > 0) {
          buffer.write(chunk, 0, n);
        }
        in = buffer.toByteArray();
      }
      String text = name + " " + exchange.getRequestMethod() + " " + exchange.getRequestURI() + " token="
          + exchange.getRequestHeaders().getFirst("token") + " body=" + new String(in, StandardCharsets.UTF_8);
      byte[] out = text.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(200, out.length);
      try (OutputStream os = exchange.getResponseBody()) {
        os.write(out);
      }
    });
    server.start();
    return server;
  }

  private static String uri(HttpServer server) {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  @Test
  public void balancesAcrossInstances() {
    CloudContext.setDiscoveryClient(new SimpleDiscoveryClient().add("demo", uri(one), uri(two)));
    ServiceClient client = new ServiceClient(new OkHttpClient(), 2);
    Set<String> served = new HashSet<>();
    for (int i = 0; i < 4; i++) {
      ServiceResponse response = client.call("demo",
          ServiceRequest.post("/echo?x=1").header("token", "t1").json("{\"a\":1}"));
      assertEquals(200, response.getStatus());
      served.add(response.getBodyString().split(" ")[0]);
      assertEquals("POST /echo?x=1 token=t1 body={\"a\":1}",
          response.getBodyString().substring(response.getBodyString().indexOf(' ') + 1));
    }
    assertEquals(2, served.size());
  }

  @Test
  public void failsOverWhenAnInstanceIsDown() throws IOException {
    int deadPort;
    try (ServerSocket socket = new ServerSocket(0)) {
      deadPort = socket.getLocalPort();
    }
    CloudContext.setDiscoveryClient(
        new SimpleDiscoveryClient().add("demo", "http://127.0.0.1:" + deadPort, uri(one)));
    ServiceClient client = new ServiceClient(new OkHttpClient(), 2);
    for (int i = 0; i < 4; i++) {
      ServiceResponse response = client.call("demo", ServiceRequest.post("/write").json("{}"));
      assertEquals("one", response.getBodyString().split(" ")[0]);
    }
  }

  @Test
  public void throwsWhenNoInstance() {
    ServiceClient client = new ServiceClient(new OkHttpClient(), 2);
    try {
      client.get("missing-service", "/x");
      fail();
    } catch (ServiceCallException e) {
      assertEquals("no available instance of missing-service", e.getMessage());
    }
  }
}
