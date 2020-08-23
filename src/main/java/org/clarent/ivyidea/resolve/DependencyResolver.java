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

package org.clarent.ivyidea.resolve;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ConfigurationResolveReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.IvyNode;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.clarent.ivyidea.config.IvyIdeaConfigHelper;
import org.clarent.ivyidea.config.model.ArtifactTypeSettings;
import org.clarent.ivyidea.exception.IvyFileReadException;
import org.clarent.ivyidea.exception.IvySettingsFileReadException;
import org.clarent.ivyidea.exception.IvySettingsNotFoundException;
import org.clarent.ivyidea.ivy.IvyManager;
import org.clarent.ivyidea.ivy.IvyUtil;
import org.clarent.ivyidea.resolve.dependency.*;
import org.clarent.ivyidea.resolve.problem.ResolveProblem;
import org.clarent.ivyidea.resolve.sort.ConfigurationGraph;
import org.clarent.ivyidea.resolve.sort.ConfigurationGraphSorter;
import org.clarent.ivyidea.resolve.sort.ConfigurationNode;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author Guy Mahieu
 */

class DependencyResolver {

    private static final Logger LOGGER = Logger.getLogger(DependencyResolver.class.getName());

    private final List<ResolveProblem> resolveProblems;
    private final Set<ExternalDependency> resolvedExternalDependencies;
    private final Set<InternalDependency> resolvedInternalDependencies;

    private final Module module;
    private final IvyManager ivyManager;

    private final Map<String, DependencyScope> dependencyScopes;

    public DependencyResolver(Module module, IvyManager ivyManager) {
        this.module = module;
        this.ivyManager = ivyManager;

        resolveProblems = new ArrayList<>();
        resolvedExternalDependencies = new HashSet<>();
        resolvedInternalDependencies = new HashSet<>();
        dependencyScopes = IvyIdeaConfigHelper.getDependencyScopes(module);
    }

    public List<ResolveProblem> getResolveProblems() {
        return Collections.unmodifiableList(resolveProblems);
    }

    public Set<ExternalDependency> getResolvedExternalDependencies() {
        return Collections.unmodifiableSet(resolvedExternalDependencies);
    }

    public Set<InternalDependency> getResolvedInternalDependencies() {
        return Collections.unmodifiableSet(resolvedInternalDependencies);
    }

    public void resolve() throws IvySettingsNotFoundException, IvyFileReadException, IvySettingsFileReadException {
        final File ivyFile = IvyUtil.getIvyFile(module);
        if (ivyFile == null) {
            throw new IvyFileReadException(null, module.getName(), null);
        }

        final Ivy ivy = ivyManager.getIvy(module);
        try {
            Set<Configuration> configurations = IvyUtil.loadConfigurations(ivyFile.getAbsolutePath(), ivy);

            List<String> sortedConfigurations = Collections.emptyList();
            if (configurations != null) {
                /*
                 * Configurations extends from each other leading to a configuration graph and a multi-mapping of
                 * dependencies to different configurations. Thus, you'll get duplicates of the same dependency for
                 * different configurations, also it is defined only once within the ivy.xml.
                 * That's nothing new and generally, nothing problematic. But, if you want to map the dependencies to
                 * different dependency scopes managed by IntelliJ IDEA, you have to take the lowest configuration the
                 * dependency is mapped to and the corresponding dependency scope mapping for this configuration to get
                 * the correct result.
                 * Sadly, Ivy doesn't provide you something like that in its reports or anywhere else. Therefore, a
                 * topology sort is used to sort the available configurations to get the lowest one at index 0 and so on.
                 * This order you finally use to receive the resolved dependencies for every configuration. The first
                 * occurrence of a dependency defines the dependency scope based on the configuration. Other occurrences
                 * on other/later configurations are simply ignored. There can only be one scope mapping and mostly
                 * they are extended from each other. So, there are usually no losses. Even if there are "losses" because
                 * of a missing extends, you'll see a combination of both configurations in a build script or something.
                 * Nothing important, the IDE needs to know or do something against or for it, anyway.
                 */
                ConfigurationGraph graph = new ConfigurationGraph();
                for (Configuration configuration : configurations) {
                    graph.addNode(new ConfigurationNode(configuration));
                }
                sortedConfigurations = ConfigurationGraphSorter.topoSort(graph);
            }

            ResolveOptions resolveOptions = IvyIdeaConfigHelper.createResolveOptions(module);
            final ResolveReport resolveReport = ivy.resolve(ivyFile.toURI().toURL(), resolveOptions);
            extractDependencies(ivy, resolveReport, new IntellijModuleDependencies(module, ivyManager), sortedConfigurations);
        } catch (ParseException | IOException e) {
            throw new IvyFileReadException(ivyFile.getAbsolutePath(), module.getName(), e);
        }
    }

    // TODO: This method performs way too much tasks -- refactor it!
    protected void extractDependencies(Ivy ivy, ResolveReport resolveReport, IntellijModuleDependencies moduleDependencies, List<String> sortedConfigurations) {
        final String[] resolvedConfigurations = resolveReport.getConfigurations();
        sortedConfigurations.retainAll(Arrays.asList(resolvedConfigurations));
        if (sortedConfigurations.isEmpty()) {
            sortedConfigurations = new ArrayList<>(Arrays.asList(resolvedConfigurations));
        }
        for (String resolvedConfiguration : sortedConfigurations) {
            ConfigurationResolveReport configurationReport = resolveReport.getConfigurationReport(resolvedConfiguration);

            DependencyScope targetDependencyScope = dependencyScopes.getOrDefault(resolvedConfiguration,
                    DependencyScope.COMPILE);
            // TODO: Refactor this a bit
            registerProblems(configurationReport, moduleDependencies, targetDependencyScope);

            Set<ModuleRevisionId> dependencies = configurationReport.getModuleRevisionIds();
            for (ModuleRevisionId dependency : dependencies) {
                if (isModuleToBeHandledAsInternalDependency(moduleDependencies, dependency)) {
                    // If the user has chosen to detect dependencies on internal modules we add a module dependency rather
                    // than a dependency on an external library.
                    Module moduleDependency = moduleDependencies.getModuleDependency(dependency.getModuleId());
                    resolvedInternalDependencies.add(new InternalDependency(moduleDependency, targetDependencyScope));
                } else {
                    final Project project = moduleDependencies.getModule().getProject();
                    final ArtifactDownloadReport[] artifactDownloadReports = configurationReport.getDownloadReports(
                            dependency);
                    for (ArtifactDownloadReport artifactDownloadReport : artifactDownloadReports) {
                        final Artifact artifact = artifactDownloadReport.getArtifact();
                        final File artifactFile = artifactDownloadReport.getLocalFile();
                        addExternalDependency(artifact, artifactFile, project, targetDependencyScope);
                    }

                    // If activated manually download any missing javadoc or source dependencies,
                    // in case they weren't selected by the Ivy configuration.
                    // This means that dependencies in ivy.xml don't need to explicitly include configurations
                    // for javadoc or sources, just to ensure that the plugin can see them. The plugin will
                    // get all javadocs and sources it can find for each dependency.
                    final boolean attachSources = IvyIdeaConfigHelper.alwaysAttachSources(project);
                    final boolean attachJavadocs = IvyIdeaConfigHelper.alwaysAttachJavadocs(project);
                    if (attachSources || attachJavadocs) {
                        final IvyNode node = configurationReport.getDependency(dependency);
                        final ModuleDescriptor md = node.getDescriptor();
                        final Artifact[] artifacts = md.getAllArtifacts();
                        for (Artifact artifact : artifacts) {
                            // TODO: if sources are found, don't bother attaching javadoc?
                            // That way, IDEA will generate the javadoc and resolve links to other javadocs
                            if ((attachSources && isSource(project, artifact)) || (attachJavadocs && isJavadoc(project,
                                    artifact))) {
                                if (resolveReport.getArtifacts().contains(artifact)) {
                                    continue; // already resolved, ignore.
                                }

                                // try to download
                                ArtifactDownloadReport adr = ivy.getResolveEngine()
                                        .download(artifact, new DownloadOptions());
                                addExternalDependency(artifact, adr.getLocalFile(), project, null);
                            }
                        }
                    }
                }
            }
        }
    }

    private void addExternalDependency(Artifact artifact, File artifactFile, Project project, DependencyScope dependencyScope) {
        ExternalDependency externalDependency = ExternalDependencyFactory.getInstance()
                .createExternalDependency(artifact, artifactFile, project, dependencyScope);
        if (externalDependency == null) {
            resolveProblems.add(new ResolveProblem(artifact.getModuleRevisionId().toString(),
                    "Unrecognized artifact type: " + artifact.getType() + ", will not add this as a dependency in IntelliJ.",
                    null));
            LOGGER.warning("Artifact of unrecognized type " + artifact.getType() + " found, *not* adding as a dependency.");
        } else if (externalDependency.isMissing()) {
            resolveProblems.add(new ResolveProblem(artifact.getModuleRevisionId().toString(),
                    "File not found: " + externalDependency.getLocalFile().getAbsolutePath()));
        } else {
            resolvedExternalDependencies.add(externalDependency);
        }
    }

    private boolean isSource(Project project, Artifact artifact) {
        return ArtifactTypeSettings.DependencyCategory.Sources == ExternalDependencyFactory.determineCategory(project,
                artifact);
    }

    private boolean isJavadoc(Project project, Artifact artifact) {
        return ArtifactTypeSettings.DependencyCategory.Javadoc == ExternalDependencyFactory.determineCategory(project,
                artifact);
    }

    private void registerProblems(ConfigurationResolveReport configurationReport, IntellijModuleDependencies moduleDependencies, DependencyScope dependencyScope) {
        for (IvyNode unresolvedDependency : configurationReport.getUnresolvedDependencies()) {
            if (isModuleToBeHandledAsInternalDependency(moduleDependencies, unresolvedDependency.getId())) {
                // centralize  this!
                Module moduleDependency = moduleDependencies.getModuleDependency(unresolvedDependency.getModuleId());
                resolvedInternalDependencies.add(new InternalDependency(moduleDependency, dependencyScope));
            } else {
                resolveProblems.add(new ResolveProblem(unresolvedDependency.getId().toString(),
                        unresolvedDependency.getProblemMessage(),
                        unresolvedDependency.getProblem()));
                LOGGER.info("DEPENDENCY PROBLEM: " + unresolvedDependency.getId() + ": " + unresolvedDependency.getProblemMessage());
            }
        }
    }

    private boolean isModuleToBeHandledAsInternalDependency(IntellijModuleDependencies moduleDependencies, ModuleRevisionId dependency) {
        boolean detectDependenciesOnOtherModulesWhileResolving = IvyIdeaConfigHelper.detectDependenciesOnOtherModulesWhileResolving(
                moduleDependencies.getModule().getProject());
        boolean detectDependenciesOnOtherModulesOfSameVersionWhileResolving = IvyIdeaConfigHelper.detectDependenciesOnOtherModulesWhileResolvingOfSameVersion(
                moduleDependencies.getModule().getProject());
        return detectDependenciesOnOtherModulesWhileResolving && moduleDependencies.isInternalIntellijModuleDependency(
                dependency.getModuleId()) && (!detectDependenciesOnOtherModulesOfSameVersionWhileResolving || moduleDependencies
                .isInternalIntellijModuleDependencyWithSameRevision(dependency));
    }
}
