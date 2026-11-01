package nexus.io.tio.boot.cloud;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;

import nexus.io.tio.boot.cloud.config.ConfigFormats;

public class ConfigFormatsTest {

  @Test
  public void flattensYamlMapsAndLists() {
    String yaml = "server:\n  port: 18760\ngateway:\n  routes:\n    - id: im\n      uri: lb://im-server\n"
        + "      predicates:\n        - Path=/im/**\n      filters:\n        - StripPrefix=1\n";
    Map<String, String> values = ConfigFormats.parse("gateway.yml", yaml);
    assertEquals("18760", values.get("server.port"));
    assertEquals("im", values.get("gateway.routes[0].id"));
    assertEquals("lb://im-server", values.get("gateway.routes[0].uri"));
    assertEquals("Path=/im/**", values.get("gateway.routes[0].predicates[0]"));
    assertEquals("StripPrefix=1", values.get("gateway.routes[0].filters[0]"));
  }

  @Test
  public void parsesProperties() {
    Map<String, String> values = ConfigFormats.parse("common.properties", "a=1\nb.c=two\n");
    assertEquals("1", values.get("a"));
    assertEquals("two", values.get("b.c"));
  }
}
