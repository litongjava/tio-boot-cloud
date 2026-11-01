package nexus.io.tio.boot.cloud.nacos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import nexus.io.tio.boot.cloud.discovery.DiscoveryClient;
import nexus.io.tio.boot.cloud.discovery.ServiceInstance;

/**
 * Discovery backed by Nacos. The first lookup of a service subscribes to it;
 * after that the Nacos client keeps the instance list up to date from server
 * pushes, so lookups are served from memory.
 */
public class NacosDiscoveryClient implements DiscoveryClient {
  private static final Logger log = LoggerFactory.getLogger(NacosDiscoveryClient.class);

  private final NamingService namingService;
  private final String group;

  public NacosDiscoveryClient(NamingService namingService, String group) {
    this.namingService = namingService;
    this.group = group;
  }

  @Override
  public List<ServiceInstance> getInstances(String serviceId) {
    try {
      List<Instance> instances = namingService.selectInstances(serviceId, group, true, true);
      List<ServiceInstance> result = new ArrayList<>(instances.size());
      for (Instance instance : instances) {
        if (instance.isEnabled()) {
          result.add(toServiceInstance(serviceId, instance));
        }
      }
      return result;
    } catch (NacosException e) {
      log.error("query instances of {} failed: {}", serviceId, e.getErrMsg());
      return new ArrayList<>();
    }
  }

  @Override
  public List<String> getServices() {
    try {
      return namingService.getServicesOfServer(1, Integer.MAX_VALUE, group).getData();
    } catch (NacosException e) {
      log.error("query services failed: {}", e.getErrMsg());
      return new ArrayList<>();
    }
  }

  static ServiceInstance toServiceInstance(String serviceId, Instance instance) {
    Map<String, String> metadata = new LinkedHashMap<>();
    if (instance.getMetadata() != null) {
      metadata.putAll(instance.getMetadata());
    }
    ServiceInstance result = new ServiceInstance(serviceId, instance.getIp(), instance.getPort());
    result.setInstanceId(instance.getInstanceId() == null ? instance.getIp() + ":" + instance.getPort()
        : instance.getInstanceId());
    result.setWeight(instance.getWeight());
    result.setSecure("true".equalsIgnoreCase(metadata.get("secure")));
    result.setMetadata(metadata);
    return result;
  }
}
