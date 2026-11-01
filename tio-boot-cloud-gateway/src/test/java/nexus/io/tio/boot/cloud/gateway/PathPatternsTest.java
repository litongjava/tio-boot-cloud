package nexus.io.tio.boot.cloud.gateway;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Test;

import nexus.io.tio.boot.cloud.gateway.predicate.PathPatterns;

public class PathPatternsTest {

  @Test
  public void trailingDoubleStarMatchesPrefixAndBase() {
    Pattern p = PathPatterns.compile("/api/im/**");
    assertTrue(p.matcher("/api/im").matches());
    assertTrue(p.matcher("/api/im/").matches());
    assertTrue(p.matcher("/api/im/chat/msg/page").matches());
    assertFalse(p.matcher("/api/imx").matches());
    assertFalse(p.matcher("/api/oauth/login").matches());
  }

  @Test
  public void singleStarAndVariablesStayInOneSegment() {
    Pattern p = PathPatterns.compile("/user/{id}/*.json");
    assertTrue(p.matcher("/user/7/profile.json").matches());
    assertFalse(p.matcher("/user/7/a/profile.json").matches());
  }
}
