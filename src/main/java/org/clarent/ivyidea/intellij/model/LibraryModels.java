/*
 * Copyright 2010 Guy Mahieu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.clarent.ivyidea.intellij.model;

import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Disposer;
import org.clarent.ivyidea.resolve.dependency.ExternalDependency;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

class LibraryModels implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(LibraryModels.class.getName());    

    private final Map<String, Library> libraryModels = new ConcurrentHashMap<>();

    private final ModifiableRootModel intellijModule;

    LibraryModels(ModifiableRootModel intellijModule) {
        this.intellijModule = intellijModule;
    }

    public Library getForExternalDependency(final ExternalDependency externalDependency) {
        String libraryName = getLibraryName(externalDependency);
        return libraryModels.computeIfAbsent(libraryName, _libraryName -> getIvyIdeaLibrary(intellijModule, _libraryName));
    }

    public String getLibraryName(ExternalDependency externalDependency) {
        return "Ivy: " + externalDependency.getArtifactName();
    }

    private Library getIvyIdeaLibrary(ModifiableRootModel modifiableRootModel, final String libraryName) {
        final LibraryTable libraryTable = modifiableRootModel.getModuleLibraryTable();
        final Library library = libraryTable.getLibraryByName(libraryName);
        if (library == null) {
            LOGGER.info("Internal library not found for module " + modifiableRootModel.getModule().getModuleFilePath() + ", creating with name " + libraryName + "...");
            return libraryTable.createLibrary(libraryName);
        }
        return library;
    }

    public void removeDependency(OrderRootType type, String dependencyUrl) {
        LOGGER.info("Removing no longer needed dependency of type " + type + ": " + dependencyUrl);
        for (Library library : libraryModels.values()) {
            library.getModifiableModel().removeRoot(dependencyUrl, type);
        }
    }

    @Override
    public void close() {
        for (Library library : libraryModels.values()) {
            Library.ModifiableModel libraryModel = library.getModifiableModel();
            if (libraryModel.isChanged()) {
                libraryModel.commit();
            } else {
                Disposer.dispose(libraryModel);
            }
        }
    }
}
