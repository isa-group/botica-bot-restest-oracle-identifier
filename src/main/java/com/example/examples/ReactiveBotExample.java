package com.example.examples;

import es.us.isa.botica.bot.AbstractBotApplication;
import java.util.UUID;

public class ReactiveBotExample extends AbstractBotApplication {
  /**
   * Handles the reactive action for this bot. This method is triggered when an order is received.
   *
   * @param incomingMessage the message received as part of the order
   */
  @Override
  public void onOrderReceived(String incomingMessage) {
    // Perform a long blocking task
    String longBlockingTaskResult = this.runLongBlockingTask(incomingMessage);

    // Publish the result using the key and order specified in the configuration file for this bot
    publishOrder(longBlockingTaskResult);

    // You can also publish a message with a custom key and order
    publishOrder("my_key", "my_order", longBlockingTaskResult);
  }

  /**
   * Simulates a long-running blocking task.
   *
   * @param base a string to perform the task
   * @return a concatenation of the given string and a random string
   */
  private String runLongBlockingTask(String base) {
    try {
      Thread.sleep(5000L);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return base + UUID.randomUUID();
  }
}
