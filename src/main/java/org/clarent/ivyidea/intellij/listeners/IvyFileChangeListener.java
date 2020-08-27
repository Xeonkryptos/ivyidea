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

package org.clarent.ivyidea.intellij.listeners;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.clarent.ivyidea.ivy.IvyUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
        updatableModules.clear();

        Project[] openProjects = projectManager.getOpenProjects();
        ProjectFileIndex[] fileIndices = new ProjectFileIndex[openProjects.length];
        for (int i = 0; i < openProjects.length; i++) {
            Project openProject = openProjects[i];
            ProjectRootManager projectRootManager = ProjectRootManager.getInstance(openProject);
            fileIndices[i] = projectRootManager.getFileIndex();
        }
        events.stream()
                .filter(event -> event.isValid() && event.isFromSave())
                .filter(event -> event instanceof VFileContentChangeEvent)
                .map(event -> ((VFileContentChangeEvent) event).getFile())
                .map(virtualFile -> {
                    for (ProjectFileIndex fileIndex : fileIndices) {
                        if (fileIndex.isInContent(virtualFile)) {
                            return fileIndex.getModuleForFile(virtualFile);
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .filter(IvyUtil::hasIvyFile)
                .forEach(updatableModules::add);
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        for (Module updatableModule : updatableModules) {
            Map<String, Object> dataContextData = new HashMap<>();
            dataContextData.put(LangDataKeys.MODULE.getName(), updatableModule);
            dataContextData.put(LangDataKeys.PROJECT.getName(), updatableModule.getProject());
            DataContext dataContext = SimpleDataContext.getSimpleContext(dataContextData, null);
            Presentation presentation = new Presentation();
            presentation.setText(updatableModule.getName());
            am.getAction("IvyIDEA.UpdateSingleModuleDependencies")
                    .actionPerformed(new AnActionEvent(null,
                            dataContext,
                            ActionPlaces.TOOLBAR,
                            presentation,
                            ActionManager.getInstance(),
                            0));
        }
    }
}
