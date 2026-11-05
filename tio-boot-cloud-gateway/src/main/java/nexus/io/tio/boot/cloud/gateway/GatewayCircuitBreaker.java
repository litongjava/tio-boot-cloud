package nexus.io.tio.boot.cloud.gateway;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Circuit breaker of one route. After {@code failureThreshold} consecutive
 * failures requests are refused for {@code openMillis}. The first request after
 * that is a trial: success closes the breaker, failure opens it again.
 */
public class GatewayCircuitBreaker {
  private final int failureThreshold;
  private final long openMillis;
  private final LongSupplier clock;
  private final AtomicInteger failures = new AtomicInteger();
  private final AtomicBoolean trialRunning = new AtomicBoolean();
  private volatile long openUntil;

  public GatewayCircuitBreaker(int failureThreshold, long openMillis) {
    this(failureThreshold, openMillis, System::currentTimeMillis);
  }

  GatewayCircuitBreaker(int failureThreshold, long openMillis, LongSupplier clock) {
    this.failureThreshold = failureThreshold;
    this.openMillis = openMillis;
    this.clock = clock;
  }

  /**
   * @return whether the request may go to the upstream
   */
  public boolean tryAcquire() {
    if (openUntil == 0) {
      return true;
    }
    if (clock.getAsLong() < openUntil) {
      return false;
    }
    return trialRunning.compareAndSet(false, true);
  }

  public void onSuccess() {
    failures.set(0);
    openUntil = 0;
    trialRunning.set(false);
  }

  public void onFailure() {
    if (failures.incrementAndGet() >= failureThreshold) {
      openUntil = clock.getAsLong() + openMillis;
    }
    trialRunning.set(false);
  }

  public boolean isOpen() {
    return openUntil != 0;
  }
}
