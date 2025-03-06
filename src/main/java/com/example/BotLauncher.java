package com.example;

import es.us.isa.botica.bot.BotApplicationRunner;

public class BotLauncher {
  public static void main(String[] args) {
    BotApplicationRunner.run(new Bot(), args);
  }
}
