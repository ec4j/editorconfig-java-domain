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
package org.ec4j.java.domain.jdt;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import org.ec4j.core.Cache;
import org.ec4j.core.Cache.Caches;
import org.ec4j.core.EditorConfigLoader;
import org.ec4j.core.Resource.Resources;
import org.ec4j.core.ResourceProperties;
import org.ec4j.core.ResourcePropertiesService;
import org.ec4j.core.model.PropertyType;
import org.ec4j.core.model.PropertyType.IndentStyleValue;
import org.ec4j.java.domain.tck.spi.Formatter;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jdt.internal.compiler.env.IModule;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

/**
 * A {@link Formatter} based on {@link DefaultCodeFormatter} of Eclipse Java Develepment Tools (JDT).
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class JdtFormatter implements Formatter {
    private static final Integer DEFAULT_JAVA_INDENT_SIZE = Integer.valueOf(4);

    /**
     * Translated the given EditorConfig {@link ResourceProperties} to a {@link Map} of JDT Formatter's properties.
     *
     * @param properties the {@link ResourceProperties} to transforms
     * @return a new {@link Map}
     */
    private static Map<String, String> toJdtFormatterOptions(ResourceProperties properties) {
        Map<String, String> result = new TreeMap<>();
        final IndentStyleValue indentStyle = properties.getValue(PropertyType.indent_style, IndentStyleValue.space,
                false);
        switch (indentStyle) {
        case tab:
        case space:
            result.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, indentStyle.name());
            break;
        default:
            throw new IllegalStateException(
                    String.format("Unexpected %s: [%s]", IndentStyleValue.class.getName(), indentStyle));
        }

        final Integer indentSize = properties.getValue(PropertyType.indent_size, DEFAULT_JAVA_INDENT_SIZE, false);
        result.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, indentSize.toString());

        final Integer maxLineLength = properties.getValue(PropertyType.max_line_length, Integer.MAX_VALUE, true);
        result.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, maxLineLength.toString());

        return result;
    }

    private final ResourcePropertiesService resourcePropertiesService;

    public JdtFormatter() {
        final Cache myCache = Caches.permanent();
        EditorConfigLoader myLoader = EditorConfigLoader.default_();
        resourcePropertiesService = ResourcePropertiesService.builder()
                .cache(myCache)
                .loader(myLoader)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public String format(Path sourcePath) {
        try {
            final ResourceProperties properties = resourcePropertiesService.queryProperties(Resources.ofPath(sourcePath, StandardCharsets.UTF_8));
            final Charset charset = Charset.forName(properties.getValue(PropertyType.charset, "utf-8", true));
            final String source = new String(Files.readAllBytes(sourcePath), charset);

            final Map<String, String> options = toJdtFormatterOptions(properties);
            final DefaultCodeFormatter formatter = new DefaultCodeFormatter(options);
            final int kind = (sourcePath.getFileName().toString().equals(IModule.MODULE_INFO_JAVA)
                    ? CodeFormatter.K_MODULE_INFO
                    : CodeFormatter.K_COMPILATION_UNIT) | CodeFormatter.F_INCLUDE_COMMENTS;
            final PropertyType.EndOfLineValue eol = properties.getValue(PropertyType.end_of_line, null, true);
            final TextEdit edit = formatter.format(kind, source, 0, source.length(), 0, eol.getEndOfLineString());

            final IDocument doc = new Document(source);
            edit.apply(doc, TextEdit.UPDATE_REGIONS);
            return doc.get();
        } catch (MalformedTreeException | IOException | BadLocationException e) {
            throw new RuntimeException(String.format("Could not format [%s]", sourcePath), e);
        }
    }

}
