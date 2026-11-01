package nexus.io.tio.boot.cloud.nacos;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import com.alibaba.nacos.api.PropertyKeyConst;

import nexus.io.tio.boot.cloud.config.RemoteConfig;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Nacos settings, read through {@code EnvUtils} so they can come from
 * {@code app.properties}, command-line arguments or environment variables.
 *
 * <pre>
 * nacos.server-addr=127.0.0.1:8848
 * nacos.namespace=hula-dev
 * nacos.username=nacos
 * nacos.password=nacos
 * nacos.discovery.metadata.gray_version=v2
 * nacos.config.data-ids=common.yml,user-service.yml
 * </pre>
 */
public class NacosProperties {
  public static final String SERVER_ADDR = "nacos.server-addr";
  public static final String NAMESPACE = "nacos.namespace";
  public static final String GROUP = "nacos.group";
  public static final String USERNAME = "nacos.username";
  public static final String PASSWORD = "nacos.password";

  public static final String DISCOVERY_ENABLED = "nacos.discovery.enabled";
  public static final String DISCOVERY_REGISTER_ENABLED = "nacos.discovery.register-enabled";
  public static final String DISCOVERY_SERVICE = "nacos.discovery.service";
  public static final String DISCOVERY_GROUP = "nacos.discovery.group";
  public static final String DISCOVERY_IP = "nacos.discovery.ip";
  public static final String DISCOVERY_PORT = "nacos.discovery.port";
  public static final String DISCOVERY_WEIGHT = "nacos.discovery.weight";
  public static final String DISCOVERY_CLUSTER = "nacos.discovery.cluster-name";
  public static final String DISCOVERY_EPHEMERAL = "nacos.discovery.ephemeral";
  public static final String DISCOVERY_METADATA_PREFIX = "nacos.discovery.metadata.";

  public static final String CONFIG_ENABLED = "nacos.config.enabled";
  public static final String CONFIG_DATA_IDS = "nacos.config.data-ids";
  public static final String CONFIG_GROUP = "nacos.config.group";
  public static final String CONFIG_REFRESH_ENABLED = "nacos.config.refresh-enabled";
  public static final String CONFIG_TIMEOUT = "nacos.config.timeout";

  public static final String DEFAULT_GROUP = "DEFAULT_GROUP";

  public static String serverAddr() {
    return EnvUtils.get(SERVER_ADDR, "127.0.0.1:8848");
  }

  public static String group() {
    return EnvUtils.get(GROUP, DEFAULT_GROUP);
  }

  public static String discoveryGroup() {
    return EnvUtils.get(DISCOVERY_GROUP, group());
  }

  public static String configGroup() {
    return EnvUtils.get(CONFIG_GROUP, group());
  }

  /**
   * @return the registered service name: {@code nacos.discovery.service}, else
   *         {@code app.name}
   */
  public static String serviceName() {
    String name = EnvUtils.get(DISCOVERY_SERVICE);
    if (name == null || name.trim().isEmpty()) {
      name = EnvUtils.get("app.name");
    }
    return name;
  }

  public static List<String> dataIds() {
    List<String> ids = new ArrayList<>();
    String value = EnvUtils.get(CONFIG_DATA_IDS);
    if (value == null) {
      return ids;
    }
    for (String id : value.split(",")) {
      if (!id.trim().isEmpty()) {
        ids.add(id.trim());
      }
    }
    return ids;
  }

  public static Map<String, String> metadata() {
    return RemoteConfig.valuesWithPrefix(DISCOVERY_METADATA_PREFIX);
  }

  /**
   * @return client properties for NamingService and ConfigService
   */
  public static Properties clientProperties() {
    Properties properties = new Properties();
    properties.setProperty(PropertyKeyConst.SERVER_ADDR, serverAddr());
    String namespace = EnvUtils.get(NAMESPACE);
    if (namespace != null && !namespace.trim().isEmpty()) {
      properties.setProperty(PropertyKeyConst.NAMESPACE, namespace.trim());
    }
    String username = EnvUtils.get(USERNAME);
    if (username != null && !username.isEmpty()) {
      properties.setProperty(PropertyKeyConst.USERNAME, username);
      properties.setProperty(PropertyKeyConst.PASSWORD, EnvUtils.get(PASSWORD, ""));
    }
    return properties;
  }

  /**
   * @return {@code nacos.discovery.ip}, else the first site-local IPv4 address
   *         of an interface that is up, else the local host address
   */
  public static String registerIp() {
    String ip = EnvUtils.get(DISCOVERY_IP);
    if (ip != null && !ip.trim().isEmpty()) {
      return ip.trim();
    }
    try {
      String fallback = null;
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface ni = interfaces.nextElement();
        String name = ni.getName().toLowerCase(Locale.ROOT);
        if (!ni.isUp() || ni.isLoopback() || ni.isVirtual() || name.startsWith("docker") || name.startsWith("veth")
            || name.startsWith("br-")) {
          continue;
        }
        Enumeration<InetAddress> addresses = ni.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress address = addresses.nextElement();
          if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
            if (address.isSiteLocalAddress()) {
              return address.getHostAddress();
            }
            if (fallback == null) {
              fallback = address.getHostAddress();
            }
          }
        }
      }
      if (fallback != null) {
        return fallback;
      }
      return InetAddress.getLocalHost().getHostAddress();
    } catch (Exception e) {
      return "127.0.0.1";
    }
  }
}
