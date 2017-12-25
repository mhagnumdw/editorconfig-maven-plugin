/**
 * Copyright (c) ${project.inceptionYear} EditorConfig Maven Plugin
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
package org.ec4j.maven.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.ec4j.core.ResourceProperties;
import org.ec4j.maven.check.ViolationCollector;
import org.ec4j.maven.format.EditableDocument;
import org.ec4j.maven.format.FormattingHandler;
import org.ec4j.maven.validator.XmlValidatorTest;
import org.junit.Assert;

public class ValidatorTestUtils {

    public static void assertParse(Validator validator, EditableDocument doc, String expectedText,
            ResourceProperties props, Violation... expected) throws IOException, MojoExecutionException {

        ViolationCollector collector = new ViolationCollector(false);
        collector.startFiles();
        collector.startFile(doc);
        validator.process(doc, props, collector);
        collector.endFile();
        collector.endFiles();

        Map<Resource, List<Violation>> violations = collector.getViolations();
        List<Violation> actual = violations.get(doc);

        if (expected.length == 0) {
            Assert.assertNull("" + expected.length + " violations expected, found " + actual, actual);
        } else {
            Assert.assertNotNull("" + expected.length + " violations expected, found none", actual);
        }
        Assert.assertEquals(Arrays.asList(expected), actual);

        FormattingHandler formatter = new FormattingHandler(false, false);
        formatter.startFiles();
        formatter.startFile(doc);
        validator.process(doc, props, formatter);
        formatter.endFile();
        formatter.endFiles();

        Assert.assertEquals(expectedText, doc.asString());

    }

    public static EditableDocument createDocument(String text, String fileExtension) throws IOException {
        Path file = File.createTempFile(XmlValidatorTest.class.getSimpleName(), fileExtension).toPath();
        EditableDocument doc = new EditableDocument(file, StandardCharsets.UTF_8, text);
        return doc;
    }

}
