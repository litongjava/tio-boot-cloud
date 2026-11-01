package nexus.io.tio.boot.cloud.nacos;

import java.util.LinkedHashMap;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import nexus.io.tio.boot.cloud.discovery.ServiceInstance;
import nexus.io.tio.boot.cloud.discovery.ServiceRegistry;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Registers instances in Nacos. Ephemeral instances (the default) are kept
 * alive by the Nacos client and removed by the server when the process dies.
 */
public class NacosServiceRegistry implements ServiceRegistry {
  private final NamingService namingService;
  private final String group;

  public NacosServiceRegistry(NamingService namingService, String group) {
    this.namingService = namingService;
    this.group = group;
  }

  @Override
  public void register(ServiceInstance serviceInstance) {
    try {
      namingService.registerInstance(serviceInstance.getServiceId(), group, toInstance(serviceInstance));
    } catch (NacosException e) {
      throw new IllegalStateException("register " + serviceInstance + " failed: " + e.getErrMsg(), e);
    }
  }

  @Override
  public void deregister(ServiceInstance serviceInstance) {
    try {
      namingService.deregisterInstance(serviceInstance.getServiceId(), group, toInstance(serviceInstance));
    } catch (NacosException e) {
      throw new IllegalStateException("deregister " + serviceInstance + " failed: " + e.getErrMsg(), e);
    }
  }

  static Instance toInstance(ServiceInstance serviceInstance) {
    Instance instance = new Instance();
    instance.setIp(serviceInstance.getHost());
    instance.setPort(serviceInstance.getPort());
    instance.setWeight(serviceInstance.getWeight());
    instance.setClusterName(EnvUtils.get(NacosProperties.DISCOVERY_CLUSTER, "DEFAULT"));
    instance.setEphemeral(EnvUtils.getBoolean(NacosProperties.DISCOVERY_EPHEMERAL, true));
    instance.setHealthy(true);
    instance.setEnabled(true);
    instance.setMetadata(new LinkedHashMap<>(serviceInstance.getMetadata()));
    return instance;
  }
}
