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

package org.clarent.ivyidea.intellij.listeners.eclipse;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Xeonkryptos
 * @since 26.08.2020
 */
public class EclipseFileCreationListener implements BulkFileListener {

    private final ProjectManager projectManager = ProjectManager.getInstance();

    private Pair<Project, ProjectFileIndex>[] fileIndices;
    private List<VFileCreateEvent> eclipseFileCreationEvents;

    @Override
    @SuppressWarnings("unchecked")
    public void before(@NotNull List<? extends VFileEvent> events) {
        Project[] openProjects = projectManager.getOpenProjects();
        fileIndices = new Pair[openProjects.length];
        for (int i = 0; i < openProjects.length; i++) {
            Project openProject = openProjects[i];
            ProjectRootManager projectRootManager = ProjectRootManager.getInstance(openProject);
            fileIndices[i] = new Pair<>(openProject, projectRootManager.getFileIndex());
        }
        eclipseFileCreationEvents = events.stream()
                .filter(VFileEvent::isValid)
                .filter(event -> event instanceof VFileCreateEvent)
                .map(event -> (VFileCreateEvent) event)
                .filter(event -> !event.isDirectory())
                .filter(event -> ".classpath".equals(event.getChildName()) || ".project".equals(event.getChildName()))
                .collect(Collectors.toList());
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        eclipseFileCreationEvents.stream().map(VFileEvent::getFile).filter(Objects::nonNull).map(virtualFile -> {
            for (Pair<Project, ProjectFileIndex> pair : fileIndices) {
                if (pair.second.isInContent(virtualFile)) {
                    for (Module module : ModuleManager.getInstance(pair.first).getModules()) {
                        ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                        ModuleFileIndex moduleFileIndex = moduleRootManager.getFileIndex();
                        if (moduleFileIndex.isInContent(virtualFile)) {
                            return new Pair<>(new Pair<>(pair.first, module), virtualFile);
                        }
                    }
                    return new Pair<>(new Pair<Project, Module>(pair.first, null), virtualFile);
                }
            }
            return null;
        }).filter(Objects::nonNull).forEach(pair -> {
            if (".project".equals(pair.second.getName()) && pair.second.isInLocalFileSystem()) {
                updateProjectFile(pair.first.first, pair.second);
            } else if (pair.second.isInLocalFileSystem()) {
                updateClasspathFile(pair.first.first, pair.first.second, pair.second);
            }
        });
    }

    private void updateClasspathFile(Project project, Module module, VirtualFile virtualClasspathFile) {
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        XmlFile psiClasspathFile = (XmlFile) PsiManager.getInstance(project).findFile(virtualClasspathFile);

        XmlTag rootTag;
        if (psiClasspathFile != null && (rootTag = psiClasspathFile.getRootTag()) != null) {
            XmlTag[] classPathEntryTags = rootTag.findSubTags("classpathentry");
            boolean notFound = true;
            for (XmlTag classPathEntryTag : classPathEntryTags) {
                XmlAttribute kindAttribute = classPathEntryTag.getAttribute("kind");
                XmlAttribute pathAttribute = classPathEntryTag.getAttribute("path");

                String pathAttributeValue;
                if (kindAttribute != null && kindAttribute.textMatches("con") && pathAttribute != null && (pathAttributeValue = pathAttribute
                        .getValue()) != null && pathAttributeValue.startsWith(
                        "org.apache.ivyde.eclipse.cpcontainer.IVYDE_CONTAINER")) {
                    notFound = false;
                }
            }
            if (notFound) {
                XmlTag classPathEntryTag = rootTag.createChildTag("classpathentry", null, null, false);
                classPathEntryTag.setAttribute("kind", "con");
                classPathEntryTag.setAttribute("path",
                        "org.apache.ivyde.eclipse.cpcontainer.IVYDE_CONTAINER/?project=" + module.getName() + "$amp;ivyXmlPath=ivy.xml$amp;confs=*");
                WriteCommandAction.runWriteCommandAction(project,
                        (Runnable) () -> rootTag.addSubTag(classPathEntryTag, false));
            }
        }
    }

    private void updateProjectFile(Project project, VirtualFile virtualProjectFile) {
        XmlFile psiProjectFile = (XmlFile) PsiManager.getInstance(project).findFile(virtualProjectFile);
        XmlTag rootTag;
        if (psiProjectFile != null && (rootTag = psiProjectFile.getRootTag()) != null) {
            XmlTag naturesTag = rootTag.findFirstSubTag("natures");
            if (naturesTag != null) {
                XmlTag[] natureTags = naturesTag.findSubTags("nature");

                boolean notFound = true;
                for (XmlTag natureTag : natureTags) {
                    if (natureTag.textMatches("org.apache.ivyde.eclipse.ivynature")) {
                        notFound = false;
                        break;
                    }
                }
                if (notFound) {
                    XmlTag natureChildTag = naturesTag.createChildTag("nature",
                            null,
                            "org.apache.ivyde.eclipse.ivynature",
                            false);
                    WriteCommandAction.runWriteCommandAction(project,
                            (Runnable) () -> naturesTag.addSubTag(natureChildTag, false));
                }
            }
        }
    }
}
