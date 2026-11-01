package nexus.io.tio.boot.cloud.demo.consumer;

import nexus.io.tio.boot.TioApplication;
import nexus.io.tio.boot.cloud.demo.consumer.config.ConsumerConfig;

public class ConsumerApp {
  public static void main(String[] args) {
    TioApplication.run(ConsumerApp.class, new ConsumerConfig(), args);
  }
}
