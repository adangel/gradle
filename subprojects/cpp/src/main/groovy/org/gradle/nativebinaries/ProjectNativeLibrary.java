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
package org.gradle.nativebinaries;

import org.gradle.api.Incubating;

/**
 * A library component that is built by a gradle project.
 */
@Incubating
public interface ProjectNativeLibrary extends ProjectNativeComponent, TargetedNativeComponent {
    /**
     * Converts this library to a native library requirement that uses the shared library variant. This is the default.
     */
    NativeLibraryRequirement getShared();

    /**
     * Converts this library to a native library requirement that uses the static library variant.
     */
    NativeLibraryRequirement getStatic();

    /**
     * Converts this library to a native library requirement that uses the api library linkage.
     */
    NativeLibraryRequirement getApi();
}