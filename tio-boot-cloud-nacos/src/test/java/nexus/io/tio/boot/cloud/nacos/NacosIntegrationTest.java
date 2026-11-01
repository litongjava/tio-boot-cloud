package nexus.io.tio.boot.cloud.nacos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import com.alibaba.nacos.api.config.ConfigService;

import nexus.io.tio.boot.cloud.CloudContext;
import nexus.io.tio.boot.cloud.config.RemoteConfig;
import nexus.io.tio.boot.cloud.discovery.ServiceInstance;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Runs against a real Nacos server when the environment variable
 * NACOS_IT=true is set, e.g. {@code NACOS_IT=true mvn test}.
 */
public class NacosIntegrationTest {

  @BeforeClass
  public static void setUp() {
    Assume.assumeTrue("true".equals(System.getenv("NACOS_IT")));
    EnvUtils.set(NacosProperties.SERVER_ADDR, System.getenv().getOrDefault("NACOS_ADDR", "127.0.0.1:8848"));
    EnvUtils.set(NacosProperties.NAMESPACE, System.getenv().getOrDefault("NACOS_NAMESPACE", "hula-dev"));
    EnvUtils.set(NacosProperties.USERNAME, "nacos");
    EnvUtils.set(NacosProperties.PASSWORD, System.getenv().getOrDefault("NACOS_PASSWORD", "nacos"));
  }

  @AfterClass
  public static void tearDown() {
    if ("true".equals(System.getenv("NACOS_IT"))) {
      TioBootNacos.shutdown();
    }
  }

  @Test
  public void registersAndDiscovers() throws Exception {
    NacosServiceRegistry registry = new NacosServiceRegistry(TioBootNacos.namingService(), "DEFAULT_GROUP");
    CloudContext.setDiscoveryClient(new NacosDiscoveryClient(TioBootNacos.namingService(), "DEFAULT_GROUP"));
    ServiceInstance instance = new ServiceInstance("it-service", "127.0.0.1", 18999);
    instance.getMetadata().put("gray_version", "v2");
    registry.register(instance);
    try {
      List<ServiceInstance> found = waitForInstances("it-service", 1);
      assertEquals(1, found.size());
      assertEquals(18999, found.get(0).getPort());
      assertEquals("v2", found.get(0).getMetadata("gray_version"));
    } finally {
      registry.deregister(instance);
    }
    assertTrue(waitForInstances("it-service", 0).isEmpty());
  }

  @Test
  public void loadsAndRefreshesConfig() throws Exception {
    ConfigService config = TioBootNacos.configService();
    String dataId = "it-config.yml";
    assertTrue(config.publishConfig(dataId, "DEFAULT_GROUP", "it:\n  greeting: hello\n", "yaml"));
    Thread.sleep(1000);
    EnvUtils.set(NacosProperties.CONFIG_DATA_IDS, dataId);
    CountDownLatch changed = new CountDownLatch(1);
    AtomicReference<Set<String>> keys = new AtomicReference<>();
    new NacosConfigLoader(config).load();
    assertEquals("hello", EnvUtils.get("it.greeting"));

    RemoteConfig.addListener((source, changedKeys) -> {
      keys.set(changedKeys);
      changed.countDown();
    });
    assertTrue(config.publishConfig(dataId, "DEFAULT_GROUP", "it:\n  greeting: hi\n", "yaml"));
    assertTrue(changed.await(15, TimeUnit.SECONDS));
    assertEquals("hi", EnvUtils.get("it.greeting"));
    assertTrue(keys.get().contains("it.greeting"));
    config.removeConfig(dataId, "DEFAULT_GROUP");
    RemoteConfig.reset();
  }

  private static List<ServiceInstance> waitForInstances(String serviceId, int expected) throws InterruptedException {
    List<ServiceInstance> found = CloudContext.getInstances(serviceId);
    for (int i = 0; i < 50 && found.size() != expected; i++) {
      Thread.sleep(200);
      found = CloudContext.getInstances(serviceId);
    }
    return found;
  }
}
