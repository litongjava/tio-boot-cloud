package nexus.io.tio.boot.cloud.gateway.predicate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import nexus.io.tio.http.common.utils.HttpIpUtils;

/**
 * Built-in predicates, created from definitions such as {@code Path=/im/**}:
 * <ul>
 * <li>{@code Path=/im/**, /api/im/**}: any of the path patterns</li>
 * <li>{@code Method=GET, POST}</li>
 * <li>{@code Header=name} or {@code Header=name, regex}</li>
 * <li>{@code Query=name} or {@code Query=name, regex}</li>
 * <li>{@code Host=api.example.com, *.example.com}</li>
 * <li>{@code RemoteAddr=127.0.0.1, 10.0.}: client IP equals or starts with
 * one of the values</li>
 * </ul>
 */
public class RoutePredicates {

  public static RoutePredicate create(String definition) {
    String[] parts = definition.split("=", 2);
    String name = parts[0].trim();
    String args = parts.length > 1 ? parts[1].trim() : "";
    switch (name) {
    case "Path":
      return path(split(args));
    case "Method": {
      Set<String> methods = new HashSet<>();
      for (String method : split(args)) {
        methods.add(method.toUpperCase(Locale.ROOT));
      }
      return exchange -> methods.contains(exchange.getMethod());
    }
    case "Header": {
      List<String> values = splitTwoOptional(args);
      String header = values.get(0).toLowerCase(Locale.ROOT);
      Pattern regex = values.size() > 1 ? Pattern.compile(values.get(1)) : null;
      return exchange -> {
        String value = exchange.getRequestHeader(header);
        return value != null && (regex == null || regex.matcher(value).matches());
      };
    }
    case "Query": {
      List<String> values = splitTwoOptional(args);
      String param = values.get(0);
      Pattern regex = values.size() > 1 ? Pattern.compile(values.get(1)) : null;
      return exchange -> {
        String value = exchange.getRequest().getParam(param);
        return value != null && (regex == null || regex.matcher(value).matches());
      };
    }
    case "Host": {
      List<Pattern> hosts = new ArrayList<>();
      for (String host : split(args)) {
        hosts.add(Pattern.compile("^" + host.replace(".", "\\.").replace("*", "[^.]+") + "(:\\d+)?$",
            Pattern.CASE_INSENSITIVE));
      }
      return exchange -> {
        String host = exchange.getRequestHeader("host");
        if (host == null) {
          return false;
        }
        for (Pattern pattern : hosts) {
          if (pattern.matcher(host).matches()) {
            return true;
          }
        }
        return false;
      };
    }
    case "RemoteAddr": {
      List<String> prefixes = split(args);
      return exchange -> {
        String ip = HttpIpUtils.getRealIp(exchange.getRequest());
        for (String prefix : prefixes) {
          if (ip != null && ip.startsWith(prefix)) {
            return true;
          }
        }
        return false;
      };
    }
    default:
      throw new IllegalArgumentException("Unknown route predicate: " + definition);
    }
  }

  public static RoutePredicate path(List<String> patterns) {
    List<Pattern> compiled = new ArrayList<>();
    for (String pattern : patterns) {
      compiled.add(PathPatterns.compile(pattern));
    }
    return exchange -> {
      String path = exchange.getRequest().getRequestLine().getPath();
      for (Pattern pattern : compiled) {
        if (pattern.matcher(path).matches()) {
          return true;
        }
      }
      return false;
    };
  }

  private static List<String> split(String args) {
    List<String> result = new ArrayList<>();
    for (String item : args.split(",")) {
      if (!item.trim().isEmpty()) {
        result.add(item.trim());
      }
    }
    return result;
  }

  private static List<String> splitTwoOptional(String args) {
    List<String> result = new ArrayList<>();
    int comma = args.indexOf(',');
    if (comma < 0) {
      result.add(args.trim());
    } else {
      result.add(args.substring(0, comma).trim());
      result.add(args.substring(comma + 1).trim());
    }
    return result;
  }
}
