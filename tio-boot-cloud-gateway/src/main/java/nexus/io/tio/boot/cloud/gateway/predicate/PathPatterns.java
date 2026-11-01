package nexus.io.tio.boot.cloud.gateway.predicate;

import java.util.regex.Pattern;

/**
 * Compiles path patterns: {@code *} matches within one segment, {@code **}
 * matches any number of segments, {@code {name}} matches one segment. A
 * trailing {@code /**} also matches the path without it, so {@code /im/**}
 * matches {@code /im}, {@code /im/} and {@code /im/a/b}.
 */
public class PathPatterns {

  public static Pattern compile(String pattern) {
    StringBuilder regex = new StringBuilder("^");
    String body = pattern;
    boolean tail = false;
    if (body.endsWith("/**")) {
      body = body.substring(0, body.length() - 3);
      tail = true;
    }
    int i = 0;
    while (i < body.length()) {
      char c = body.charAt(i);
      if (c == '*') {
        if (i + 1 < body.length() && body.charAt(i + 1) == '*') {
          regex.append(".*");
          i += 2;
          continue;
        }
        regex.append("[^/]*");
      } else if (c == '{') {
        int end = body.indexOf('}', i);
        if (end < 0) {
          throw new IllegalArgumentException("Unclosed variable in " + pattern);
        }
        regex.append("[^/]+");
        i = end + 1;
        continue;
      } else {
        regex.append(Pattern.quote(String.valueOf(c)));
      }
      i++;
    }
    if (tail) {
      regex.append("(/.*)?");
    }
    regex.append("$");
    return Pattern.compile(regex.toString());
  }
}
