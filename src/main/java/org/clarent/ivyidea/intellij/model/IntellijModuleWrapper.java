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

import com.google.common.collect.Streams;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.Library.ModifiableModel;
import com.intellij.openapi.roots.libraries.LibraryTable;
import org.clarent.ivyidea.resolve.dependency.ExternalDependency;
import org.clarent.ivyidea.resolve.dependency.InternalDependency;
import org.clarent.ivyidea.resolve.dependency.ResolvedDependency;

import java.util.*;

public class IntellijModuleWrapper implements AutoCloseable {

    private final ModifiableRootModel intellijModule;
    private final LibraryModels libraryModels;

    private List<Runnable> dependencyManipulationActions;

    public static IntellijModuleWrapper forModule(Module module) {
        ModifiableRootModel modifiableModel = null;
        try {
            modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
            return new IntellijModuleWrapper(modifiableModel);
        } catch (RuntimeException e) {
            if (modifiableModel != null) {
                modifiableModel.dispose();
            }
            throw e;
        }
    }

    private IntellijModuleWrapper(ModifiableRootModel intellijModule) {
        this.intellijModule = intellijModule;
        this.libraryModels = new LibraryModels(intellijModule);
    }

    public void updateDependencies(Collection<ExternalDependency> resolvedExternalDependencies, Collection<InternalDependency> resolvedInternalDependencies) {
        dependencyManipulationActions = new ArrayList<>();

        Streams.concat(resolvedExternalDependencies.stream(), resolvedInternalDependencies.stream())
                .forEach(resolvedDependency -> resolvedDependency.addTo(this));

        removeDependenciesNotInList(resolvedExternalDependencies, resolvedInternalDependencies);

        dependencyManipulationActions.add(this::close);
        Runnable[] dependencyManipulationActionsArray = dependencyManipulationActions.toArray(new Runnable[0]);

        ApplicationManager.getApplication().invokeLater(() -> {
            for (Runnable runnable : dependencyManipulationActionsArray) {
                ApplicationManager.getApplication().runWriteAction(runnable);
            }
        });
    }

    @Override
    public void close() {
        libraryModels.close();
        if (intellijModule.isChanged()) {
            intellijModule.commit();
        } else {
            intellijModule.dispose();
        }
    }

    public String getModuleName() {
        return intellijModule.getModule().getName();
    }

    public void addModuleDependency(Module module, DependencyScope dependencyScope) {
        dependencyManipulationActions.add(() -> {
            ModuleOrderEntry moduleOrderEntry = intellijModule.addModuleOrderEntry(module);
            if (dependencyScope != null) {
                moduleOrderEntry.setScope(dependencyScope);
            }
        });
    }

    public void updateModuleDependencyScope(Module module, DependencyScope dependencyScope) {
        dependencyManipulationActions.add(() -> {
            ModuleOrderEntry moduleOrderEntry = intellijModule.findModuleOrderEntry(module);
            if (moduleOrderEntry != null && dependencyScope != null) {
                moduleOrderEntry.setScope(dependencyScope);
            }
        });
    }

    public void addExternalDependency(ExternalDependency externalDependency, DependencyScope dependencyScope) {
        dependencyManipulationActions.add(() -> {
            Library library = libraryModels.getForExternalDependency(externalDependency);
            library.getModifiableModel().addRoot(externalDependency.getUrlForLibraryRoot(), externalDependency.getType());
            updateExternalDependencyScope(library, dependencyScope);
        });
    }

    public void updateExternalDependencyScope(ExternalDependency externalDependency, DependencyScope dependencyScope) {
        dependencyManipulationActions.add(() -> {
            Library library = libraryModels.getForExternalDependency(externalDependency);
            updateExternalDependencyScope(library, dependencyScope);
        });
    }

    private void updateExternalDependencyScope(Library library, DependencyScope dependencyScope) {
        LibraryOrderEntry libraryOrderEntry = intellijModule.findLibraryOrderEntry(library);
        if (libraryOrderEntry != null && dependencyScope != null) {
            libraryOrderEntry.setScope(dependencyScope);
        }
    }

    public boolean alreadyHasDependencyOnModule(Module module) {
        final Module[] existingDependencies = intellijModule.getModuleDependencies();
        for (Module existingDependency : existingDependencies) {
            if (existingDependency.getName().equals(module.getName())) {
                return true;
            }
        }
        return false;
    }

    public boolean alreadyHasDependencyOnLibrary(ExternalDependency externalDependency) {
        ModifiableModel libraryModel = libraryModels.getForExternalDependency(externalDependency).getModifiableModel();
        for (String url : libraryModel.getUrls(externalDependency.getType())) {
            if (externalDependency.isSameDependency(url)) {
                return true;
            }
        }
        return false;
    }

    public void removeDependenciesNotInList(Collection<ExternalDependency> externalDependenciesToKeep, Collection<InternalDependency> internalDependenciesToKeep) {
        // remove resolved libraries that are no longer used
        Set<String> librariesInUse = new HashSet<>(externalDependenciesToKeep.size());
        for (ExternalDependency externalDependency : externalDependenciesToKeep) {
            librariesInUse.add(libraryModels.getLibraryName(externalDependency));
        }

        final LibraryTable libraryTable = intellijModule.getModuleLibraryTable();
        for (Library library : libraryTable.getLibraries()) {
            final String libraryName = library.getName();
            if (!librariesInUse.contains(libraryName)) {
                dependencyManipulationActions.add(() -> libraryTable.removeLibrary(library));
            }
        }

        Set<String> internalModulesToKeep = new HashSet<>(internalDependenciesToKeep.size());
        for (ResolvedDependency resolvedDependency : internalDependenciesToKeep) {
            String moduleName = ((InternalDependency) resolvedDependency).getModuleName();
            internalModulesToKeep.add(moduleName);
        }
        for (Module moduleDependency : intellijModule.getModuleDependencies()) {
            if (!internalModulesToKeep.contains(moduleDependency.getName())) {
                ModuleOrderEntry moduleOrderEntry = intellijModule.findModuleOrderEntry(moduleDependency);
                if (moduleOrderEntry != null) {
                    dependencyManipulationActions.add(() -> intellijModule.removeOrderEntry(moduleOrderEntry));
                }
            }
        }
    }
}
