package es.us.isa.restest.bot.oracle;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Utility methods to work with Beet outputs */
public final class BeetHelper {
  // from GenerateInstrumentation class
  private static final String DAIKON_DECLS_FILE_NAME = "declsFile.decls";
  private static final String DAIKON_DTRACE_FILE_NAME = "dtraceFile.dtrace";

  private BeetHelper() {}

  public static String getDeclsFilePath(File testCasesFile) {
    return getOutputPath(DAIKON_DECLS_FILE_NAME, testCasesFile.toString());
  }

  public static String getDtraceFilePath(File testCasesFile) {
    return getOutputPath(DAIKON_DTRACE_FILE_NAME, testCasesFile.toString());
  }

  // from GenerateInstrumentation class
  private static String getOutputPath(String filename, String folder) {
    Path path = Paths.get(folder);
    Path dir = path.getParent();
    Path fn = path.getFileSystem().getPath(filename);
    Path target = dir == null ? fn : dir.resolve(fn);
    return target.toString();
  }
}
