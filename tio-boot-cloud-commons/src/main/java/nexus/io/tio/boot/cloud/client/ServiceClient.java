package nexus.io.tio.boot.cloud.client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.tio.boot.cloud.CloudContext;
import nexus.io.tio.boot.cloud.discovery.ServiceInstance;
import nexus.io.tio.boot.cloud.loadbalancer.LoadBalancerRequest;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.http.OkHttpClientPool;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Calls other services by name. Each call looks up the instances of the
 * service, lets the load balancer pick one and sends the request over HTTP.
 * When an instance cannot be reached the call moves on to another instance.
 *
 * <pre>
 * ServiceResponse response = ServiceClient.me().call("user-service", ServiceRequest.get("/api/user/1"));
 * User user = response.json(User.class);
 * </pre>
 *
 * Configuration:
 * <ul>
 * <li>{@code cloud.client.max-attempts}: instances to try, default 2</li>
 * <li>{@code cloud.client.read-timeout}: read timeout in seconds, default 60</li>
 * <li>{@code cloud.client.connect-timeout}: connect timeout in seconds, default 3</li>
 * </ul>
 */
public class ServiceClient {
  private static final Logger log = LoggerFactory.getLogger(ServiceClient.class);

  private static volatile ServiceClient me;

  private final List<ServiceRequestInterceptor> interceptors = new CopyOnWriteArrayList<>();
  private final OkHttpClient httpClient;
  private final int maxAttempts;

  public static ServiceClient me() {
    if (me == null) {
      synchronized (ServiceClient.class) {
        if (me == null) {
          ServiceClient client = new ServiceClient();
          client.addInterceptor(new HeaderPropagationInterceptor());
          me = client;
        }
      }
    }
    return me;
  }

  public ServiceClient() {
    this(OkHttpClientPool.getHttpClient().newBuilder()
        .connectTimeout(EnvUtils.getInt("cloud.client.connect-timeout", 3), TimeUnit.SECONDS)
        .readTimeout(EnvUtils.getInt("cloud.client.read-timeout", 60), TimeUnit.SECONDS)
        .build(), EnvUtils.getInt("cloud.client.max-attempts", 2));
  }

  public ServiceClient(OkHttpClient httpClient, int maxAttempts) {
    this.httpClient = httpClient;
    this.maxAttempts = Math.max(1, maxAttempts);
  }

  public ServiceClient addInterceptor(ServiceRequestInterceptor interceptor) {
    interceptors.add(interceptor);
    return this;
  }

  public ServiceResponse get(String serviceId, String path) {
    return call(serviceId, ServiceRequest.get(path));
  }

  public ServiceResponse postJson(String serviceId, String path, Object body) {
    return call(serviceId, ServiceRequest.post(path).json(body));
  }

  /**
   * Sends the request to an instance of the service.
   *
   * @param serviceId service name
   * @param request   method, path, headers and body
   * @return the response, whatever its status
   * @throws ServiceCallException when no instance is available or none could
   *                              be reached
   */
  public ServiceResponse call(String serviceId, ServiceRequest request) {
    for (ServiceRequestInterceptor interceptor : interceptors) {
      interceptor.apply(serviceId, request);
    }
    LoadBalancerRequest lbRequest = new LoadBalancerRequest(lowerCase(request.getHeaders()));
    List<ServiceInstance> tried = new ArrayList<>();
    IOException last = null;
    for (int attempt = 0; attempt < maxAttempts; attempt++) {
      ServiceInstance instance = CloudContext.choose(serviceId, lbRequest, tried);
      if (instance == null) {
        break;
      }
      tried.add(instance);
      try {
        return execute(instance, request);
      } catch (IOException e) {
        last = e;
        if (!isRetryable(e, request)) {
          break;
        }
        log.warn("call {} {} on {} failed, trying another instance: {}", serviceId, request.getPath(), instance,
            e.getMessage());
      }
    }
    if (last != null) {
      throw new ServiceCallException("call " + serviceId + " " + request.getPath() + " failed: " + last.getMessage(),
          last);
    }
    throw new ServiceCallException("no available instance of " + serviceId);
  }

  private ServiceResponse execute(ServiceInstance instance, ServiceRequest request) throws IOException {
    Request.Builder builder = new Request.Builder().url(instance.getUri() + request.getPath());
    Headers.Builder headers = new Headers.Builder();
    for (Map.Entry<String, String> e : request.getHeaders().entrySet()) {
      headers.addUnsafeNonAscii(e.getKey(), e.getValue());
    }
    builder.headers(headers.build());
    RequestBody body = null;
    if (request.getBody() != null) {
      MediaType mediaType = request.getContentType() == null ? null : MediaType.parse(request.getContentType());
      body = RequestBody.create(request.getBody(), mediaType);
    } else if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())
        || "PATCH".equals(request.getMethod())) {
      body = RequestBody.create(new byte[0], null);
    }
    builder.method(request.getMethod(), body);

    try (Response response = httpClient.newCall(builder.build()).execute()) {
      Map<String, String> responseHeaders = new HashMap<>();
      for (String name : response.headers().names()) {
        responseHeaders.put(name.toLowerCase(Locale.ROOT), response.header(name));
      }
      ResponseBody responseBody = response.body();
      byte[] bytes = responseBody == null ? null : responseBody.bytes();
      return new ServiceResponse(response.code(), responseHeaders, bytes, instance);
    }
  }

  private static boolean isRetryable(IOException e, ServiceRequest request) {
    if (e instanceof ConnectException || e instanceof NoRouteToHostException || e instanceof UnknownHostException) {
      return true;
    }
    return request.isIdempotent();
  }

  private static Map<String, String> lowerCase(Map<String, String> headers) {
    Map<String, String> result = new HashMap<>();
    for (Map.Entry<String, String> e : headers.entrySet()) {
      result.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
    }
    return result;
  }
}
