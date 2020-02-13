/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.jvm.tasks.api.internal

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor
import org.gradle.internal.normalization.java.ApiClassExtractor
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import org.objectweb.asm.ClassReader
import spock.lang.Shared
import spock.lang.Specification

import javax.tools.DiagnosticCollector
import javax.tools.JavaCompiler
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject
import javax.tools.ToolProvider
import java.lang.reflect.Field
import java.lang.reflect.Method

class ApiClassExtractorTestSupport extends Specification {

    private static class JavaSourceFromString extends SimpleJavaFileObject {

        private final String code

        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///${ApiClassExtractorTestSupport.toFileName(name)}"), JavaFileObject.Kind.SOURCE)
            this.code = code
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            code
        }
    }

    @CompileStatic
    private static class ApiClassLoader extends URLClassLoader {

        ApiClassLoader() {
            super(new URL[0], systemClassLoader.parent)
        }

        Class<?> loadClassFromBytes(byte[] bytes) {
            defineClass(null, bytes, 0, bytes.length)
        }
    }

    @CompileStatic
    public static class ApiContainer {
        private final ApiClassLoader apiClassLoader = new ApiClassLoader()
        private final ApiClassExtractor apiClassExtractor

        public final Map<String, GeneratedClass> classes

        public ApiContainer(List<String> packages, Map<String, GeneratedClass> classes) {
            this.apiClassExtractor = new ApiClassExtractor(packages.toSet())
            this.classes = classes
        }

        protected Class<?> extractAndLoadApiClassFrom(GeneratedClass clazz) {
            apiClassLoader.loadClassFromBytes(apiClassExtractor.extractApiClassFrom(new ClassReader(clazz.bytes)))
        }

        protected byte[] extractApiClassFrom(GeneratedClass clazz) {
            apiClassExtractor.extractApiClassFrom(new ClassReader(clazz.bytes))
        }

        protected boolean shouldExtractApiClassFrom(GeneratedClass clazz) {
            apiClassExtractor.shouldExtractApiClassFrom(new ClassReader(clazz.bytes))
        }
    }

    @TupleConstructor
    @CompileStatic
    public static class GeneratedClass {
        final byte[] bytes
        final Class<?> clazz
    }

    @CompileStatic
    static String toFileName(String name, boolean clazz=false) {
        "${name.replace('.', '/')}.${clazz?'class':'java'}"
    }

    @Shared
    public JavaCompiler compiler = ToolProvider.systemJavaCompiler

    @Rule
    public final TestNameTestDirectoryProvider temporaryFolder = new TestNameTestDirectoryProvider()

    protected ApiContainer toApi(Map<String, String> sources) {
        toApi('1.7', [], sources)
    }

    protected ApiContainer toApi(String targetVersion, Map<String, String> sources) {
        toApi(targetVersion, [], sources)
    }

    protected ApiContainer toApi(List<String> packages, Map<String, String> sources) {
        toApi('1.7', packages, sources)
    }

    protected ApiContainer toApi(String targetVersion, List<String> packages,  Map<String, String> sources) {
        def dir = temporaryFolder.createDir('out')
        def fileManager = compiler.getStandardFileManager(null, null, null)
        def diagnostics = new DiagnosticCollector<JavaFileObject>()
        def task = compiler.getTask(
            new OutputStreamWriter(new ByteArrayOutputStream()),
            fileManager,
            diagnostics,
            ['-d', dir.absolutePath, '-source', targetVersion, '-target', targetVersion],
            [],
            sources.collect { fqn, src -> new JavaSourceFromString(fqn, src) })
        fileManager.close()
        if (task.call()) {
            def classLoader = new URLClassLoader([dir.toURI().toURL()] as URL[], ClassLoader.systemClassLoader.parent)
            // Load the class from the classloader by name....
            def entries = [:].withDefault { String cn ->
                def f = new File(dir, toFileName(cn, true))
                if (f.exists()) {
                    return new GeneratedClass(f.bytes, classLoader.loadClass(cn))
                }
                throw new AssertionError("Cannot find class $cn. Test is very likely not written correctly.")
            }
            return new ApiContainer(packages, entries)
        }

        StringBuilder sb = new StringBuilder("Error in compilation of test sources:\n")
        diagnostics.diagnostics.each {
            sb.append("In $it\n")
        }

        throw new RuntimeException(sb.toString())
    }

    @CompileStatic
    protected GeneratedClass toClass(String fqn, String script) {
        toApi([(fqn): script]).classes[fqn]
    }

    protected void noSuchMethod(Class c, String name, Class... argTypes) {
        try {
            c.getDeclaredMethod(name, argTypes)
        } catch (NoSuchMethodException ex) {
            return
        }
        throw new AssertionError("Should not have found method $name(${Arrays.toString(argTypes)}) on class $c")
    }

    protected Method hasMethod(Class c, String name, Class... argTypes) {
        try {
            c.getDeclaredMethod(name, argTypes)
        } catch (NoSuchMethodException ex) {
            throw new AssertionError("Should have found method $name(${Arrays.toString(argTypes)}) on class $c")
        }
    }

    protected void noSuchField(Class c, String name, Class type) {
        try {
            def f = c.getDeclaredField(name)
            if (f.type != type) {
                throw new AssertionError("Field $name was found on class $c but " +
                    "with a different type: ${f.type} instead of $type")
            }
        } catch (NoSuchFieldException ex) {
            return
        }
        throw new AssertionError("Should not have found field $name of type $type on class $c")
    }

    protected Field hasField(Class c, String name, Class type) {
        try {
            def f = c.getDeclaredField(name)
            if (f.type != type) {
                throw new AssertionError("Field $name was found on class $c but " +
                    "with a different type: ${f.type} instead of $type")
            }
            return f
        } catch (NoSuchFieldException ex) {
            throw new AssertionError("Should have found field $name on class $c")
        }
    }

}
