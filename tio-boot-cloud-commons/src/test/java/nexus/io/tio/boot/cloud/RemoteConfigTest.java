package nexus.io.tio.boot.cloud;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Test;

import nexus.io.tio.boot.cloud.config.RemoteConfig;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.environment.Prop;
import nexus.io.tio.utils.environment.PropUtils;

public class RemoteConfigTest {

  @After
  public void tearDown() {
    RemoteConfig.reset();
    System.clearProperty("rc.sys");
  }

  @Test
  public void remoteOverridesFilesButNotSystemProperties() {
    Prop local = new Prop();
    local.getProperties().setProperty("rc.local", "file");
    PropUtils.append(local);
    System.setProperty("rc.sys", "system");

    Map<String, String> remote = new HashMap<>();
    remote.put("rc.local", "remote");
    remote.put("rc.sys", "remote");
    remote.put("rc.new", "remote");
    RemoteConfig.apply("a.yml", remote);

    assertEquals("remote", EnvUtils.get("rc.local"));
    assertEquals("system", EnvUtils.get("rc.sys"));
    assertEquals("remote", EnvUtils.get("rc.new"));
  }

  @Test
  public void removedKeysFallBackToLocalValue() {
    Prop local = new Prop();
    local.getProperties().setProperty("rc.port", "8080");
    PropUtils.append(local);

    Map<String, String> remote = new HashMap<>();
    remote.put("rc.port", "9090");
    remote.put("rc.only", "x");
    RemoteConfig.apply("b.yml", remote);
    assertEquals("9090", EnvUtils.get("rc.port"));

    AtomicReference<Set<String>> changed = new AtomicReference<>();
    RemoteConfig.addListener((source, keys) -> changed.set(keys));
    RemoteConfig.apply("b.yml", new HashMap<String, String>());
    assertEquals("8080", EnvUtils.get("rc.port"));
    assertNull(EnvUtils.get("rc.only"));
    assertTrue(changed.get().contains("rc.port"));
    assertTrue(changed.get().contains("rc.only"));
  }

  @Test
  public void laterSourceWins() {
    Map<String, String> first = new HashMap<>();
    first.put("rc.k", "first");
    Map<String, String> second = new HashMap<>();
    second.put("rc.k", "second");
    RemoteConfig.apply("common.yml", first);
    RemoteConfig.apply("app.yml", second);
    // refreshing the earlier source keeps the later source's value
    first.put("rc.k", "first-v2");
    RemoteConfig.apply("common.yml", first);
    assertEquals("second", EnvUtils.get("rc.k"));
  }
}
