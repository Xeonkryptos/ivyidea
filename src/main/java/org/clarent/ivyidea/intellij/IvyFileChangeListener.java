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

package org.clarent.ivyidea.intellij;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Xeonkryptos
 * @since 23.08.2020
 */
public class IvyFileChangeListener implements BulkFileListener {

    private final ProjectManager projectManager = ProjectManager.getInstance();
    private final ActionManager am = ActionManager.getInstance();

    private final Set<Module> updatableModules = new HashSet<>();

    @Override
    public void before(@NotNull List<? extends VFileEvent> events) {
        Set<VirtualFile> ivyFiles = events.stream()
                .filter(event -> event.getPath().endsWith("ivy.xml"))
                .filter(event -> event instanceof VFileContentChangeEvent)
                .map(event -> ((VFileContentChangeEvent) event).getFile())
                .collect(Collectors.toSet());
        if (!ivyFiles.isEmpty()) {
            for (Project openProject : projectManager.getOpenProjects()) {
                ProjectRootManager projectRootManager = ProjectRootManager.getInstance(openProject);
                ProjectFileIndex fileIndex = projectRootManager.getFileIndex();

                Iterator<VirtualFile> iterator = ivyFiles.iterator();
                while (iterator.hasNext()) {
                    VirtualFile virtualFile = iterator.next();
                    if (fileIndex.isInContent(virtualFile)) {
                        Module moduleForFile = fileIndex.getModuleForFile(virtualFile);
                        updatableModules.add(moduleForFile);
                        iterator.remove();
                    }
                }
            }
        }
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        for (Module updatableModule : updatableModules) {
            Map<String, Object> dataContextData = new HashMap<>();
            dataContextData.put(LangDataKeys.MODULE.getName(), updatableModule);
            dataContextData.put(LangDataKeys.PROJECT.getName(), updatableModule.getProject());
            DataContext dataContext = SimpleDataContext.getSimpleContext(dataContextData, null);
            am.getAction("IvyIDEA.UpdateSingleModuleDependencies")
                    .actionPerformed(new AnActionEvent(null,
                            dataContext,
                            ActionPlaces.TOOLBAR,
                            new Presentation(),
                            ActionManager.getInstance(),
                            0));
        }
    }
}
