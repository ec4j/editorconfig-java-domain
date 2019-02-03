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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

final Path baseDir = Paths.get(properties.get("baseDir", ".")).toAbsolutePath()
println "baseDir = " + baseDir
final Path templatePath = baseDir.resolve("src/build/template/TestClass.template.java")
println "templatePath = " + templatePath
final String template = new String(Files.readAllBytes(templatePath), "utf-8")
final String package_ = "org.ec4j.java.domain.tck.tests"
final Path testResourcesDir = baseDir.resolve("src/test/resources")
final Path generatedClassesDir = Paths.get(properties.get("generated.tck.tests.dir")).resolve(package_.replace('.', '/'))

Files.createDirectories(generatedClassesDir)

/* A Map from class names to lists of expected Java files. One test method will be generated for each expected Java file. */
final Map<String, List<Path>> testClasses = new TreeMap<>();

/* Populate testClasses */
testResourcesDir.eachFileRecurse(groovy.io.FileType.FILES) { path ->
    path = baseDir.relativize(path)
    final String fileName = path.getFileName().toString()
    if (fileName.endsWith(".expected.java")) {
        final String className = toTestClassName(path)
        List<Path> javaFiles = testClasses.get(className)
        if (javaFiles == null) {
            testClasses.put(className, javaFiles = new ArrayList<Path>())
        }
        javaFiles.add(path);
    }
}

assert !testClasses.isEmpty()

/* Generate the test classes out of testClasses Map */
testClasses.each { className, javaFiles ->
    final StringBuilder testMethods = new StringBuilder()
    javaFiles.each { expectedJavaFile ->
        final String fileName = expectedJavaFile.getFileName().toString();
        final Path javaFile = expectedJavaFile.getParent().resolve(fileName.replace(".expected", ""))
        final String testMethodName = fileName.replace(".expected.java", "").uncapitalize()
        testMethods.append("    @Test\n") //
        testMethods.append("    public void ${testMethodName}() throws IOException {\n") //
        testMethods.append("        assertFormat(\"${javaFile.toString().replace('\\', '/')}\", \"${expectedJavaFile.toString().replace('\\', '/')}\");\n")
        testMethods.append("    }\n")
    }
    String source = template;
    source = source.replace('${package}', package_)
    source = source.replace('${className}', className)
    source = source.replace('${testMethods}', testMethods.toString())

    final Path classFilePath = generatedClassesDir.resolve(className + ".java")
    Files.write(classFilePath, source.getBytes("utf-8"))
}

String toTestClassName(Path expectedJavaPath) {
    final List<String> segments = new ArrayList<>()
    Path dir = expectedJavaPath.getParent()
    /* Climb up the path up to the grouping directory, e.g. src/test/resources/basic */
    while (dir.getNameCount() > 4) {
        segments.add(dir.getFileName().toString())
        dir = dir.getParent()
    }
    final String snakeCased = segments.reverse().join("_")
    return snakeCased.replaceAll(/_\w/){ it[1].toUpperCase() }.capitalize() + "Test"
}
