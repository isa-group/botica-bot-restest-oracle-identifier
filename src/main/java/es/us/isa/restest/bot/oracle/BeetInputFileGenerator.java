package es.us.isa.restest.bot.oracle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;

public final class BeetInputFileGenerator {
  private BeetInputFileGenerator() {}

  public static void convertFromRestest(
      File testCasesFile, File testResultsFile, Path outputFilePath) {
    try {
      List<Map<String, String>> testCases = loadTestCases(testCasesFile);
      List<Map<String, String>> testResults = loadTestResults(testResultsFile);
      List<Map<String, String>> mergedData = mergeTestData(testCases, testResults);
      saveToCsv(mergedData, outputFilePath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<Map<String, String>> loadTestCases(File file) throws IOException {
    List<Map<String, String>> testCases = loadCSV(file);

    String[] relevantColumns = {
      "testCaseId",
      "queryParameters",
      "operationId",
      "path",
      "httpMethod",
      "headerParameters",
      "pathParameters",
      "formParameters",
      "bodyParameter"
    };

    return filterColumns(testCases, relevantColumns);
  }

  private static List<Map<String, String>> loadTestResults(File file) throws IOException {
    List<Map<String, String>> testResults = loadCSV(file);

    String[] relevantColumns =
        new String[] {"testResultId", "statusCode", "passed", "failReason", "responseBody"};

    List<Map<String, String>> filteredResults = filterColumns(testResults, relevantColumns);

    for (Map<String, String> row : filteredResults) {
      String testResultId = row.get("testResultId");
      if (testResultId != null) {
        row.put("testCaseId", testResultId);
        row.remove("testResultId");
      }
    }

    return filteredResults;
  }

  private static List<Map<String, String>> loadCSV(File file) throws IOException {
    List<Map<String, String>> data = new ArrayList<>();
    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .get();

    try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
        CSVParser csvParser = CSVParser.builder().setReader(reader).setFormat(csvFormat).get()) {
      List<String> headers = csvParser.getHeaderNames();

      for (CSVRecord csvRecord : csvParser) {
        Map<String, String> row = new HashMap<>();
        for (String header : headers) {
          if (csvRecord.isMapped(header)) {
            row.put(header, csvRecord.get(header));
          } else {
            row.put(header, "");
          }
        }
        data.add(row);
      }
    }
    return data;
  }

  private static List<Map<String, String>> filterColumns(
      List<Map<String, String>> data, String[] columnsToKeep) {
    List<Map<String, String>> filtered = new ArrayList<>();

    for (Map<String, String> row : data) {
      Map<String, String> filteredRow = new HashMap<>();

      for (String column : columnsToKeep) {
        if (row.containsKey(column)) {
          filteredRow.put(column, row.get(column));
        }
      }

      filtered.add(filteredRow);
    }

    return filtered;
  }

  private static List<Map<String, String>> mergeTestData(
      List<Map<String, String>> testCases, List<Map<String, String>> testResults) {
    Map<String, Map<String, String>> testResultsMap = new HashMap<>();
    for (Map<String, String> result : testResults) {
      String testCaseId = result.get("testCaseId");
      if (testCaseId != null) {
        testResultsMap.put(testCaseId, result);
      }
    }

    List<Map<String, String>> mergedData = new ArrayList<>();
    for (Map<String, String> testCase : testCases) {
      String testCaseId = testCase.get("testCaseId");
      if (testCaseId != null && testResultsMap.containsKey(testCaseId)) {
        Map<String, String> mergedRow = new HashMap<>(testCase);
        mergedRow.putAll(testResultsMap.get(testCaseId));
        mergedData.add(mergedRow);
      }
    }

    return mergedData;
  }

  public static void saveToCsv(List<Map<String, String>> data, Path filePath) throws IOException {
    if (data == null || data.isEmpty()) {
      if (Files.notExists(filePath)) {
        Files.createFile(filePath);
      }
      return;
    }

    List<String> columns = new ArrayList<>(data.get(0).keySet());
    String[] headers = columns.toArray(new String[0]);
    boolean append = Files.exists(filePath) && Files.size(filePath) > 0;
    CSVFormat csvFormat =
        CSVFormat.DEFAULT
            .builder()
            .setHeader(append ? null : headers)
            .setQuoteMode(QuoteMode.MINIMAL)
            .get();

    try (BufferedWriter writer =
            Files.newBufferedWriter(
                filePath,
                StandardCharsets.UTF_8,
                append ? StandardOpenOption.APPEND : StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);
        CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
      for (Map<String, String> row : data) {
        List<String> recordValues = new ArrayList<>();
        for (String column : columns) {
          recordValues.add(row.getOrDefault(column, ""));
        }
        csvPrinter.printRecord(recordValues);
      }
      csvPrinter.flush();
    }
  }
}
