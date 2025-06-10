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
import java.util.List;
import java.util.Optional;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Identifies test oracles (invariants) from a collection of test case executions.
 *
 * <p>This bot operates in a stateless manner to support parallel execution. It uses the file system
 * as the source of truth:
 *
 * <ul>
 *   <li>The number of accumulated test cases is determined by counting the lines of the generated
 *       Beet input file.
 *   <li>A marker file ({@code .invariants_identified}) is created in the experiment's data
 *       directory upon successful invariant identification to prevent redundant analysis.
 * </ul>
 */
public class OracleIdentifierBot extends BaseBot {
  private static final String DAIKON_JAVA_PATH = System.getenv("DAIKON_JAVA_PATH");
  private static final String DAIKON_PATH = System.getenv("DAIKON_EXEC_PATH");
  private static final String INVARIANTS_FILE_NAME = "invariants.csv";
  private static final String INVARIANTS_IDENTIFIED_MARKER = ".invariants_identified";

  private static final Logger log = LoggerFactory.getLogger(OracleIdentifierBot.class);

  private int minTestCases;

  @Override
  public void configure() {
    this.minTestCases =
        Optional.ofNullable(System.getenv("ORACLE_BOT_MIN_TEST_CASES"))
            .map(Integer::valueOf)
            .orElse(50);
  }

  @OrderHandler("analyze_invariants")
  public void analyzeInvariants(JSONObject message) throws IOException {
    String restestConfigPath = message.getString("userConfigPath");
    String batchId = message.getString("batchId");

    RESTestLoader loader = new RESTestLoader(restestConfigPath);
    Path testDataDirectory =
        Path.of(loader.readProperty("data.tests.dir"), loader.getExperimentName());

    Path markerFile = testDataDirectory.resolve(INVARIANTS_IDENTIFIED_MARKER);
    if (Files.exists(markerFile)) {
      log.info(
          "Invariants already identified for {}, skipping analysis.", loader.getExperimentName());
      return;
    }

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

    long totalTestCases = countLines(beetInputFilePath);
    log.info("Total accumulated test cases for {}: {}", loader.getExperimentName(), totalTestCases);

    try {
      if (totalTestCases >= this.minTestCases) {
        log.info("Reached minimum amount of test cases to identify invariants!");
        Files.createFile(markerFile);
        this.identifyTestOracles(loader, testDataDirectory, beetInputFilePath);
      }
    } catch (Exception e) {
      this.notifyError(
          "There was an error while identifying test oracles for %s: %s"
              .formatted(loader.getExperimentName(), e.toString()));
    }
  }

  private void identifyTestOracles(
      RESTestLoader restestLoader, Path testDataDirectory, Path beetInputFilePath)
      throws IOException, InterruptedException {
    log.info("Running Beet for {}...", restestLoader.getExperimentName());
    runBeet(restestLoader.getSpec().getPath(), beetInputFilePath.toString());

    Path daikonOutputFilePath = testDataDirectory.resolve(INVARIANTS_FILE_NAME);
    log.info("Running Daikon...");
    runDaikon(beetInputFilePath.toFile(), daikonOutputFilePath);

    String invariants = Files.readString(daikonOutputFilePath);
    log.info("Invariants detected successfully: \n{}", invariants);

    this.notifyInvariants(
        daikonOutputFilePath,
        "Some invariants in the %s API were found.".formatted(restestLoader.getExperimentName()));
  }

  private static void runBeet(String... args) {
    GenerateInstrumentation.main(args);
  }

  private static void runDaikon(File beetInputFile, Path daikonOutputFile)
      throws IOException, InterruptedException {
    List<String> command =
        List.of(
            DAIKON_JAVA_PATH,
            "-cp",
            DAIKON_PATH,
            "daikon.Daikon",
            BeetHelper.getDeclsFilePath(beetInputFile),
            BeetHelper.getDtraceFilePath(beetInputFile));

    Process process =
        new ProcessBuilder(command)
            .redirectOutput(daikonOutputFile.toFile())
            .redirectError(Redirect.INHERIT)
            .start();

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new RuntimeException("Daikon failed with exit code " + exitCode);
    }
  }

  private void notifyInvariants(Path invariantsFilePath, String caption) {
    this.publishOrder(
        "telegram_bot",
        "send_document",
        new JSONObject()
            .put("recipient", "broadcast")
            .put("type", "document")
            .put("localPath", invariantsFilePath.toString())
            .put("caption", String.format("[%s] %s", this.getBotId(), caption))
            .toString());
  }

  private void notifyError(String content) {
    this.publishOrder(
        "telegram_bot",
        "send_message",
        new JSONObject().put("recipient", "broadcast").put("content", content).toString());
  }

  private long countLines(Path path) throws IOException {
    if (!Files.exists(path)) {
      return 0L;
    }
    try (var lines = Files.lines(path)) {
      return lines.count() - 1;
    }
  }
}
