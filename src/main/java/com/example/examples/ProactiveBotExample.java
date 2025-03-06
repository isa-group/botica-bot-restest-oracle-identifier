package com.example.examples;

import es.us.isa.botica.bot.AbstractBotApplication;
import java.util.UUID;

public class ProactiveBotExample extends AbstractBotApplication {
  /**
   * Executes the proactive action for this bot. This method will run periodically following the
   * initial delay and period specified in the botica configuration file for this bot.
   */
  @Override
  public void executeAction() {
    // Perform a long blocking task
    String longBlockingTaskResult = this.runLongBlockingTask();

    // Publish the result using the key and order specified in the configuration file for this bot
    publishOrder(longBlockingTaskResult);

    // You can also publish a message with a custom key and order
    publishOrder("my_key", "my_order", longBlockingTaskResult);
  }

  /**
   * Simulates a long-running blocking task.
   *
   * @return a random string
   */
  private String runLongBlockingTask() {
    try {
      Thread.sleep(5000L);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return UUID.randomUUID().toString();
  }
}
