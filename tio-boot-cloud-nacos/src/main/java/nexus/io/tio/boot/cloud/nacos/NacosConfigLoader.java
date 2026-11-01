package nexus.io.tio.boot.cloud.nacos;

import java.util.List;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import nexus.io.tio.boot.cloud.config.RemoteConfig;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Reads the configured data ids from Nacos into {@link RemoteConfig} and keeps
 * them up to date. Later data ids win over earlier ones for the same key.
 */
public class NacosConfigLoader {
  private static final Logger log = LoggerFactory.getLogger(NacosConfigLoader.class);

  private final ConfigService configService;

  public NacosConfigLoader(ConfigService configService) {
    this.configService = configService;
  }

  /**
   * Loads every data id of {@code nacos.config.data-ids} and, unless
   * {@code nacos.config.refresh-enabled=false}, listens for changes.
   */
  public void load() {
    String group = NacosProperties.configGroup();
    long timeout = EnvUtils.getLong(NacosProperties.CONFIG_TIMEOUT, 5000L);
    boolean refresh = EnvUtils.getBoolean(NacosProperties.CONFIG_REFRESH_ENABLED, true);
    List<String> dataIds = NacosProperties.dataIds();
    for (String dataId : dataIds) {
      try {
        String content = configService.getConfig(dataId, group, timeout);
        if (content == null) {
          log.warn("nacos config {} ({}) not found", dataId, group);
          RemoteConfig.apply(dataId, "");
        } else {
          RemoteConfig.apply(dataId, content);
          log.info("loaded nacos config {} ({})", dataId, group);
        }
        if (refresh) {
          configService.addListener(dataId, group, new Listener() {
            @Override
            public Executor getExecutor() {
              return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
              try {
                RemoteConfig.apply(dataId, configInfo == null ? "" : configInfo);
              } catch (Exception e) {
                log.error("apply nacos config {} failed", dataId, e);
              }
            }
          });
        }
      } catch (NacosException e) {
        throw new IllegalStateException("load nacos config " + dataId + " failed: " + e.getErrMsg(), e);
      }
    }
  }
}
