package es.us.isa.restest.bot.oracle;

import agora.beet.main.GenerateInstrumentation;
import es.us.isa.botica.bot.BaseBot;
import es.us.isa.botica.bot.OrderHandler;
import es.us.isa.restest.runners.RESTestLoader;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OracleIdentifierBot extends BaseBot {
  private static final String DAIKON_JAVA_PATH = System.getenv("DAIKON_JAVA_PATH");
  private static final String DAIKON_PATH = System.getenv("DAIKON_EXEC_PATH");
  private static final File DAIKON_OUTPUT_FILE = new File("invariants.csv");

  private static final Logger log = LoggerFactory.getLogger(OracleIdentifierBot.class);

  private int minTestCases;
  private final Map<String, Integer> accumulatedTestCases = new HashMap<>(); // TODO: parallelize

  @Override
  public void configure() {
    this.minTestCases =
        Optional.ofNullable(System.getenv("ORACLE_BOT_MIN_TEST_CASES"))
            .map(Integer::valueOf)
            .orElse(50);
  }

  @OrderHandler("analyze_invariants")
  public void analyzeInvariants(JSONObject message) throws IOException, InterruptedException {
    String restestConfigPath = message.getString("userConfigPath");
    String batchId = message.getString("batchId");
    int batchSize = message.getInt("batchSize");

    int totalTestCases = this.accumulatedTestCases.getOrDefault(restestConfigPath, 0) + batchSize;

    if (totalTestCases < 0) {
      // test oracles already identified
      return;
    }

    RESTestLoader loader = new RESTestLoader(restestConfigPath);
    Path testDataDirectory =
        Path.of(loader.readProperty("data.tests.dir"), loader.getExperimentName());

    File testCasesFile =
        testDataDirectory
            .resolve(
                "%s_%s.csv".formatted(loader.readProperty("data.tests.testcases.file"), batchId))
            .toFile();

    File testResultsFile =
        testDataDirectory
            .resolve(
                "%s_%s.csv".formatted(loader.readProperty("data.tests.testresults.file"), batchId))
            .toFile();

    Path beetInputFilePath = testDataDirectory.resolve(loader.getExperimentName() + ".csv");

    log.info("Adding test results to the Beet input file...");
    BeetInputFileGenerator.convertFromRestest(testCasesFile, testResultsFile, beetInputFilePath);

    if (totalTestCases >= this.minTestCases) {
      log.info("Reached minimum amount of test cases to identify invariants!");
      this.identifyTestOracles(loader, testCasesFile, beetInputFilePath);
      this.accumulatedTestCases.put(restestConfigPath, -1);
      return;
    }

    this.accumulatedTestCases.put(restestConfigPath, totalTestCases);
  }

  private void identifyTestOracles(
      RESTestLoader restestLoader, File testCasesFile, Path beetInputFilePath)
      throws IOException, InterruptedException {
    log.info("Running Beet for {}...", restestLoader.getExperimentName());
    runBeet(restestLoader.getSpec().getPath(), beetInputFilePath.toString());
    log.info("Running Daikon...");
    runDaikon(testCasesFile);

    String invariants = Files.readString(DAIKON_OUTPUT_FILE.toPath());
    log.info("Invariants detected successfully: \n{}", invariants);

    this.notifyUser(
        "Identified some invariants for the %s API!\n\n%s"
            .formatted(restestLoader.getExperimentName(), invariants));
  }

  private static void runBeet(String... args) {
    GenerateInstrumentation.main(args);
  }

  private static void runDaikon(File testCasesFile) throws IOException, InterruptedException {
    List<String> command =
        List.of(
            DAIKON_JAVA_PATH,
            "-cp",
            DAIKON_PATH,
            "daikon.Daikon",
            BeetHelper.getDeclsFilePath(testCasesFile),
            BeetHelper.getDtraceFilePath(testCasesFile));

    Process process =
        new ProcessBuilder(command)
            .redirectOutput(DAIKON_OUTPUT_FILE)
            .redirectError(Redirect.INHERIT)
            .start();

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException("Daikon failed with exit code" + exitCode);
    }
  }

  private void notifyUser(String message) {
    this.publishOrder(
        "telegram_bot", "broadcast_message", String.format("[%s] %s", this.getBotId(), message));
  }
}
