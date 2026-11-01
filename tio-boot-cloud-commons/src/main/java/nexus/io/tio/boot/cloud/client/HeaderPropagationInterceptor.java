package nexus.io.tio.boot.cloud.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Copies headers of the request being handled to the outgoing call, so the
 * called service sees the same token, tenant and trace id. The header list is
 * read from {@code cloud.client.propagate-headers} (comma separated), default
 * {@code token,authorization,x-trace-id}.
 */
public class HeaderPropagationInterceptor implements ServiceRequestInterceptor {
  public static final String KEY = "cloud.client.propagate-headers";
  public static final String DEFAULT_HEADERS = "token,authorization,x-trace-id";

  @Override
  public void apply(String serviceId, ServiceRequest request) {
    HttpRequest current = TioRequestContext.getRequest();
    if (current == null) {
      return;
    }
    for (String name : headerNames()) {
      String value = current.getHeader(name);
      if (value != null) {
        request.headerIfAbsent(name, value);
      }
    }
  }

  private static List<String> headerNames() {
    String configured = EnvUtils.get(KEY, DEFAULT_HEADERS);
    List<String> names = new ArrayList<>();
    for (String name : configured.split(",")) {
      if (!name.trim().isEmpty()) {
        names.add(name.trim().toLowerCase(Locale.ROOT));
      }
    }
    return names;
  }
}
