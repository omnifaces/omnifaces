/*
 * Copyright OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.tools.ToolProvider;

/**
 * Creates class loaders which see a class that the class loader of OmniFaces itself does not see. This mimics an application whose classes are loaded by a
 * child of the class loader which loaded OmniFaces, such as in Quarkus dev mode or with OmniFaces in a shared library of the server.
 */
public final class WebappClassLoaders {

    private WebappClassLoaders() {
        throw new AssertionError();
    }

    /**
     * Compiles the given source into the given directory and returns a class loader which sees the compiled class. Its parent is the class loader of OmniFaces,
     * which does not see the compiled class. The source is compiled against the test class path, so it may refer to OmniFaces and its dependencies.
     *
     * @param directory The directory to compile into, usually a JUnit <code>@TempDir</code>.
     * @param className The fully qualified name of the class declared by the given source.
     * @param source The Java source of the class.
     * @return The class loader which sees the compiled class.
     * @throws IOException When the source cannot be written.
     */
    public static URLClassLoader createWebappClassLoader(Path directory, String className, String source) throws IOException {
        var sourceFile = directory.resolve("src").resolve(className.replace('.', '/') + ".java");
        var classesDirectory = directory.resolve("classes");
        Files.createDirectories(sourceFile.getParent());
        Files.createDirectories(classesDirectory);
        Files.writeString(sourceFile, source);

        var classPath = System.getProperty("java.class.path");

        if (ToolProvider.getSystemJavaCompiler().run(null, null, null, "-cp", classPath, "-d", classesDirectory.toString(), sourceFile.toString()) != 0) {
            throw new IllegalStateException("Cannot compile " + className);
        }

        return new URLClassLoader("webapp", new URL[] { classesDirectory.toUri().toURL() }, WebappClassLoaders.class.getClassLoader());
    }

}
