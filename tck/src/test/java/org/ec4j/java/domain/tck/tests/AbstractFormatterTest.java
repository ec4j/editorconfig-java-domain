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
package org.ec4j.java.domain.tck.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.ec4j.java.domain.tck.spi.Formatter;
import org.junit.Assert;
import org.junit.Before;

/**
 * A superclass for tests generated by {@code generate-test-classes.groovy}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public abstract class AbstractFormatterTest {

    protected Formatter formatter;

    @Before
    public void before() {
        Iterator<Formatter> it = ServiceLoader.load(Formatter.class).iterator();
        if (!it.hasNext()) {
            Assert.fail("No service provider for " + Formatter.class.getName() + " found in class path.");
        }
        formatter = it.next();
    }

    /**
     * A helper method to call {@link Formatter#format(Path)} and compare the result with the expected output.
     *
     * @param inputPathString the path to format
     * @param expectedPathString the path containing the expected formatter output
     * @throws IOException if any of the inputs cannot be read
     */
    protected void assertFormat(String inputPathString, String expectedPathString) throws IOException {
        final Path inputPath = Paths.get(inputPathString);
        assertExists(inputPath);
        final Path expectedPath = Paths.get(expectedPathString);
        assertExists(expectedPath);
        final String actual = formatter.format(inputPath);
        final String expected = new String(Files.readAllBytes(expectedPath), StandardCharsets.UTF_8);
        Assert.assertEquals(expected, actual);
    }

    /**
     * Asserts that the given {@link Path} exists.
     * @param path the {@link Path} to check
     */
    protected static void assertExists(Path path) {
        Assert.assertTrue("Path does not exist: " + path, Files.exists(path));
    }

}
