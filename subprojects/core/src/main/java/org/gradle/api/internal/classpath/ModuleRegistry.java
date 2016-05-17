/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.api.internal.classpath;

import org.gradle.internal.classpath.ClassPath;

/**
 * A registry of dynamically loadable modules.
 */
public interface ModuleRegistry {
    /**
     * Locates an external module by name. An external module is one for which there is no meta-data available. Assumed to be packaged as a single jar file, and to have no runtime dependencies.
     *
     * @return the module. Does not return null.
     */
    Module getExternalModule(String name) throws UnknownModuleException;

    /**
     * Locates a module by name.
     *
     * @return the module. Does not return null.
     */
    Module getModule(String name) throws UnknownModuleException;

    /**
     * Locates a Gradle library by name. Does not search the classpath outside of the gradle installation.
     *
     * @return the module. Does not return null.
     */
    Module getGradleModule(String name) throws UnknownModuleException;

    /**
     * Returns the classpath used to search for modules, in addition to default locations in the Gradle distribution (if available). May be empty.
     */
    ClassPath getAdditionalClassPath();
}
