# tio-boot-cloud

为 tio-boot 应用提供微服务能力：

- **tio-boot-cloud-commons**：服务实例、发现与注册接口、负载均衡（轮询、加权随机、按实例元数据灰度）、远程配置合并与变更通知、按服务名调用的 `ServiceClient`（失败自动换实例、请求头透传）。
- **tio-boot-cloud-nacos**：注册到 Nacos、从 Nacos 发现服务、加载 Nacos 配置并热更新。
- **tio-boot-cloud-gateway**：API 网关，路由（Path、Method、Header、Query、Host、RemoteAddr）、过滤器（StripPrefix、PrefixPath、RewritePath、SetPath、请求头与响应头）、全局过滤器（CORS、Trace Id、自定义鉴权），HTTP、SSE 流式响应和 WebSocket 都按服务名负载均衡转发，路由可放在 Nacos 中热更新。

## 构建

库模块使用 JDK 8 构建：

```shell
export JAVA_HOME=/usr/java/jdk1.8.0_411
mvn clean install -DskipTests -Dgpg.skip
```

`examples` 目录是三个可运行的示例（JDK 21）：

- `demo-provider`：注册为 `demo-provider`，提供 `/demo/hello`、SSE `/demo/stream` 和 WebSocket `/demo/ws`，问候语来自 Nacos 配置 `demo-provider.yml`。
- `demo-consumer`：通过 `ServiceClient` 按服务名调用 `demo-provider`。
- `demo-gateway`：网关，`/api/provider/**` 转发到 `demo-provider`，`/api/consumer/**` 转发到 `demo-consumer`。

```shell
cd examples
mvn clean package -DskipTests -Dgpg.skip
java -jar demo-provider/target/demo-provider-1.0.0.jar
java -jar demo-provider/target/demo-provider-1.0.0.jar --server.port=18083 --nacos.discovery.metadata.version=v2
java -jar demo-consumer/target/demo-consumer-1.0.0.jar
java -jar demo-gateway/target/demo-gateway-1.0.0.jar

curl "localhost:18080/api/provider/demo/hello?name=tio"
curl "localhost:18080/api/consumer/consumer/hello?name=tio"
curl -H "gray_version: v2" "localhost:18080/api/consumer/consumer/hello?name=gray"
curl -N localhost:18080/api/provider/demo/stream
```

## 最小用法

```java
public class App {
  public static void main(String[] args) {
    TioApplication.run(App.class, () -> {
      TioBootNacos.start();
      // TioBootGateway.start(); // 网关应用再加这一行
    }, args);
  }
}
```

`nacos.config.data-ids` 中的配置在启动时自动加载，早于 `server.port` 等服务配置的读取，所以 Nacos 中的配置同样可以设置端口。

```properties
app.name=user-service
server.port=8081
server.shutdown.hook=true
nacos.server-addr=127.0.0.1:8848
nacos.namespace=dev
nacos.username=nacos
nacos.password=nacos
nacos.config.data-ids=common.yml,user-service.yml
```

完整说明见 tio-boot 文档的 cloud 章节。
