package nexus.io.tio.boot.cloud.gateway;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

public class GatewayCircuitBreakerTest {
  private final AtomicLong now = new AtomicLong(1000);
  private final GatewayCircuitBreaker breaker = new GatewayCircuitBreaker(2, 100, now::get);

  @Test
  public void opensAfterConsecutiveFailures() {
    assertTrue(breaker.tryAcquire());
    breaker.onFailure();
    assertTrue(breaker.tryAcquire());
    breaker.onFailure();
    assertTrue(breaker.isOpen());
    assertFalse(breaker.tryAcquire());
  }

  @Test
  public void successResetsTheCount() {
    breaker.onFailure();
    breaker.onSuccess();
    breaker.onFailure();
    assertTrue(breaker.tryAcquire());
  }

  @Test
  public void allowsOneTrialAfterTheOpenTime() {
    breaker.onFailure();
    breaker.onFailure();
    now.addAndGet(100);
    assertTrue(breaker.tryAcquire());
    assertFalse(breaker.tryAcquire());
    breaker.onFailure();
    assertFalse(breaker.tryAcquire());
    now.addAndGet(100);
    assertTrue(breaker.tryAcquire());
    breaker.onSuccess();
    assertFalse(breaker.isOpen());
    assertTrue(breaker.tryAcquire());
    assertTrue(breaker.tryAcquire());
  }
}
