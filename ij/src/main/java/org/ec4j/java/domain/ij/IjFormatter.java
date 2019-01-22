/**
 * Copyright (c) 2018 EditorConfig Java Domain
 * project contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ec4j.java.domain.ij;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.ec4j.java.domain.tck.spi.Formatter;

/**
 * A {@link Formatter} based on IJ {@code format.[sh|bat]} utilities.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 0.0.1
 */
public class IjFormatter implements Formatter {
    private static final String IJ_BIN_DIR_PROPERTY = "ij.bin.dir";
    private static final String IJ_TMP_WORK_DIR_PROPERTY = "ij.tmp.work.dir";
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
    private final Path formatScriptPath;
    private final Path workDirPath;

    public IjFormatter() {
        final String formatScriptName = IS_WINDOWS ? "format.bat" : "format.sh";
        final String ijBin = System.getProperty(IJ_BIN_DIR_PROPERTY);
        if (ijBin == null || ijBin.isEmpty()) {
            throw new IllegalStateException(String.format("Cannot instantiate %s: the system property \"%s\" is not set. It should point at a directory containing IntelliJ's %s script.", IjFormatter.class.getName(), IJ_BIN_DIR_PROPERTY, formatScriptName));
        }
        final Path ijBinPath = Paths.get(ijBin);
        if (!Files.exists(ijBinPath)) {
            throw new IllegalStateException(String.format("Cannot instantiate %s: the system property \"%s\" points at a non-existent directory [%s]", IjFormatter.class.getName(), IJ_BIN_DIR_PROPERTY, ijBinPath));
        }
        formatScriptPath = ijBinPath.resolve(formatScriptName);
        if (!Files.exists(formatScriptPath)) {
            throw new IllegalStateException(String.format("Cannot instantiate %s: the system property \"%s\" points at directory [%s] which does not contain the %s script", IjFormatter.class.getName(), ijBinPath, formatScriptName));
        }

        try {
            final String ijWork = System.getProperty(IJ_TMP_WORK_DIR_PROPERTY);
            if (ijWork == null || ijWork.isEmpty()) {
                workDirPath = Files.createTempDirectory(IjFormatter.class.getSimpleName());
            } else {
                workDirPath = Paths.get(ijWork);
                if (!Files.exists(workDirPath)) {
                    Files.createDirectories(workDirPath);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /** {@inheritDoc} */
    @Override
    public String format(Path sourcePath) {

        /* We have to copy the file to a temporary directory because format.[sh|bat] formats in place */
        final Path tmpSourcePath = workDirPath.resolve(sourcePath.isAbsolute() ? sourcePath.getFileName() : sourcePath);
        final List<String> command = new ArrayList<>();
        command.add(formatScriptPath.normalize().toString());
        command.add(tmpSourcePath.normalize().toString());
        try {
            Files.createDirectories(tmpSourcePath.getParent());
            Files.copy(sourcePath, tmpSourcePath);
            final Path editorConfigPath = sourcePath.getParent().resolve(".editorconfig");
            if (!Files.exists(editorConfigPath)) {
                throw new IllegalStateException(String.format("File \"%s\" must exist", editorConfigPath));
            }
            Files.copy(editorConfigPath, tmpSourcePath.getParent().resolve(".editorconfig"));

            final ProcessBuilder pb = new ProcessBuilder();
            pb.command(command);
            final Map<String, String> env = pb.environment();
            env.put("NOPAUSE", "Y");
            String javaHome = System.getProperty("java.home");
            if (javaHome.endsWith("jre")) {
                javaHome = javaHome.substring(0, javaHome.length() - 4);
            }
            env.put("JAVA_HOME", javaHome);
            Process process = pb.start();
            StreamGobbler stdOut = new StreamGobbler(process.getInputStream());
            stdOut.start();
            StreamGobbler stdErr = new StreamGobbler(process.getErrorStream());
            stdErr.start();
            int exitCode = process.waitFor();
            stdOut.join();
            stdErr.join();

            if (exitCode != 0) {
                throw new RuntimeException(String.format("Command %s returned %d\nstdout:\n%s\nstderr%s", command, exitCode, stdOut.getString(), stdErr.getString()));
            }

            return new String(Files.readAllBytes(tmpSourcePath), StandardCharsets.UTF_8);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(String.format("Command %s failed", command), e);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Command %s failed", command), e);
        }
    }

    /**
     * The usual friend of {@link Process#getInputStream()} / {@link Process#getErrorStream()}.
     */
    private static class StreamGobbler extends Thread {
        private final StringBuilder buffer = new StringBuilder();
        private IOException exception;
        private final InputStream in;

        private StreamGobbler(InputStream in) {
            this.in = in;
        }

        public String getString() throws IOException {
            if (exception != null) {
                throw exception;
            } else {
                return buffer.toString();
            }
        }

        @Override
        public void run() {
            try (InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                int ch;
                while ((ch = r.read()) >= 0) {
                    buffer.append((char) ch);
                }
            } catch (IOException e) {
                exception = e;
            }
        }
    }
}
