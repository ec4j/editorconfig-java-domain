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
package org.ec4j.java.domain.tck.spi;

import java.nio.file.Path;

/**
 * A simple source formatter interface.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @since 0.0.1
 */
public interface Formatter {

    /**
     * Reads the file under the given {@code inputPath} and returns the formatted source.
     *
     * @param inputPath the {@link Path} of a file to format
     * @return the formatted source
     * @since 0.0.1
     */
    String format(Path inputPath);
}
