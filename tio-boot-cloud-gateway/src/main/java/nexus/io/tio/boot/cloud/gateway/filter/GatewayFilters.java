package nexus.io.tio.boot.cloud.gateway.filter;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Built-in route filters, created from definitions such as
 * {@code StripPrefix=1}:
 * <ul>
 * <li>{@code StripPrefix=n}: removes the first n path segments</li>
 * <li>{@code PrefixPath=/api}: adds a prefix to the path</li>
 * <li>{@code RewritePath=/im/(?<rest>.*), /${rest}}: rewrites the path with a
 * regular expression</li>
 * <li>{@code SetPath=/fixed}: replaces the path</li>
 * <li>{@code AddRequestHeader=name, value} and
 * {@code SetRequestHeader=name, value}</li>
 * <li>{@code RemoveRequestHeader=name}</li>
 * <li>{@code AddResponseHeader=name, value}</li>
 * </ul>
 */
public class GatewayFilters {

  /**
   * @param definition {@code Name=arg1, arg2}
   * @return the filter
   * @throws IllegalArgumentException for unknown filters or bad arguments
   */
  public static GatewayFilter create(String definition) {
    String[] parts = definition.split("=", 2);
    String name = parts[0].trim();
    String args = parts.length > 1 ? parts[1].trim() : "";
    switch (name) {
    case "StripPrefix":
      return stripPrefix(Integer.parseInt(args));
    case "PrefixPath":
      return prefixPath(args);
    case "RewritePath": {
      String[] values = splitTwo(definition, args);
      return rewritePath(values[0], values[1]);
    }
    case "SetPath":
      return (exchange, chain) -> {
        exchange.setPath(args);
        return chain.filter(exchange);
      };
    case "AddRequestHeader":
    case "SetRequestHeader": {
      String[] values = splitTwo(definition, args);
      return (exchange, chain) -> {
        exchange.setRequestHeader(values[0], values[1]);
        return chain.filter(exchange);
      };
    }
    case "RemoveRequestHeader":
      return (exchange, chain) -> {
        exchange.removeRequestHeader(args);
        return chain.filter(exchange);
      };
    case "AddResponseHeader": {
      String[] values = splitTwo(definition, args);
      return (exchange, chain) -> {
        exchange.setResponseHeader(values[0], values[1]);
        return chain.filter(exchange);
      };
    }
    default:
      throw new IllegalArgumentException("Unknown gateway filter: " + definition);
    }
  }

  public static GatewayFilter stripPrefix(int parts) {
    return (exchange, chain) -> {
      String path = exchange.getPath();
      String[] segments = path.split("/", -1);
      StringBuilder stripped = new StringBuilder();
      StringBuilder removed = new StringBuilder();
      int skipped = 0;
      for (int i = 1; i < segments.length; i++) {
        if (skipped < parts) {
          removed.append('/').append(segments[i]);
          skipped++;
        } else {
          stripped.append('/').append(segments[i]);
        }
      }
      exchange.setPath(stripped.length() == 0 ? "/" : stripped.toString());
      if (removed.length() > 0 && exchange.getRequestHeader("x-forwarded-prefix") == null) {
        exchange.setRequestHeader("x-forwarded-prefix", removed.toString());
      }
      return chain.filter(exchange);
    };
  }

  public static GatewayFilter prefixPath(String prefix) {
    return (exchange, chain) -> {
      exchange.setPath(prefix + exchange.getPath());
      return chain.filter(exchange);
    };
  }

  public static GatewayFilter rewritePath(String regex, String replacement) {
    Pattern pattern = Pattern.compile(regex);
    // "$\{name}" is accepted as well, since "${name}" is often escaped in configuration files
    String javaReplacement = replacement.replace("$\\", "$");
    return (exchange, chain) -> {
      exchange.setPath(pattern.matcher(exchange.getPath()).replaceAll(javaReplacement));
      return chain.filter(exchange);
    };
  }

  /**
   * Builds the filters of a route in order.
   */
  public static void addAll(List<String> definitions, List<GatewayFilter> target) {
    for (String definition : definitions) {
      target.add(create(definition));
    }
  }

  private static String[] splitTwo(String definition, String args) {
    int comma = args.indexOf(',');
    if (comma < 0) {
      throw new IllegalArgumentException("Expected two arguments: " + definition);
    }
    return new String[] { args.substring(0, comma).trim(), args.substring(comma + 1).trim() };
  }
}
