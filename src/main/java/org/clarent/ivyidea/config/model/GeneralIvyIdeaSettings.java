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

package org.clarent.ivyidea.config.model;

import com.intellij.openapi.roots.DependencyScope;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.clarent.ivyidea.logging.IvyLogLevel;

/**
 * @author Guy Mahieu
 */
public class GeneralIvyIdeaSettings {

    private boolean useCustomIvySettings = true;
    private String ivySettingsFile = "";
    private boolean validateIvyFiles = false;
    private boolean resolveTransitively = true;
    private boolean resolveCacheOnly = false;
    private boolean resolveInBackground = false;
    private boolean alwaysAttachSources = true;
    private boolean alwaysAttachJavadocs = true;
    private boolean detectDependenciesOnOtherModules = true;
    private boolean detectDependenciesOnOtherModulesOfSameVersion = false;
    private String ivyLogLevelThreshold = IvyLogLevel.None.name();
    private Map<String, DependencyScope> dependencyScopes = new HashMap<>();
    private ArtifactTypeSettings artifactTypeSettings = new ArtifactTypeSettings();
    private PropertiesSettings propertiesSettings = new PropertiesSettings();

    public GeneralIvyIdeaSettings() {
    }

    public GeneralIvyIdeaSettings(GeneralIvyIdeaSettings copy) {
        useCustomIvySettings = copy.useCustomIvySettings;
        ivySettingsFile = copy.ivySettingsFile;
        validateIvyFiles = copy.validateIvyFiles;
        resolveTransitively = copy.resolveTransitively;
        resolveCacheOnly = copy.resolveCacheOnly;
        resolveInBackground = copy.resolveInBackground;
        alwaysAttachSources = copy.alwaysAttachSources;
        alwaysAttachJavadocs = copy.alwaysAttachJavadocs;
        detectDependenciesOnOtherModules = copy.detectDependenciesOnOtherModules;
        detectDependenciesOnOtherModulesOfSameVersion = copy.detectDependenciesOnOtherModulesOfSameVersion;
        ivyLogLevelThreshold = copy.ivyLogLevelThreshold;
        dependencyScopes = new HashMap<>(copy.dependencyScopes);
        artifactTypeSettings = new ArtifactTypeSettings(copy.artifactTypeSettings);
        propertiesSettings = new PropertiesSettings(copy.propertiesSettings);
    }

    public void updateWith(GeneralIvyIdeaSettings copy) {
        setUseCustomIvySettings(copy.useCustomIvySettings);
        setIvySettingsFile(copy.ivySettingsFile);
        setValidateIvyFiles(copy.validateIvyFiles);
        setResolveTransitively(copy.resolveTransitively);
        setResolveCacheOnly(copy.resolveCacheOnly);
        setResolveInBackground(copy.resolveInBackground);
        setAlwaysAttachSources(copy.alwaysAttachSources);
        setAlwaysAttachJavadocs(copy.alwaysAttachJavadocs);
        setDetectDependenciesOnOtherModules(copy.detectDependenciesOnOtherModules);
        setDetectDependenciesOnOtherModulesOfSameVersion(copy.detectDependenciesOnOtherModulesOfSameVersion);
        setIvyLogLevelThreshold(copy.ivyLogLevelThreshold);
        setDependencyScopes(new HashMap<>(copy.dependencyScopes));
        setArtifactTypeSettings(new ArtifactTypeSettings(copy.artifactTypeSettings));
        setPropertiesSettings(new PropertiesSettings(copy.propertiesSettings));
    }

    public String getIvySettingsFile() {
        return ivySettingsFile;
    }

    public void setIvySettingsFile(String ivySettingsFile) {
        this.ivySettingsFile = ivySettingsFile;
    }

    public boolean isValidateIvyFiles() {
        return validateIvyFiles;
    }

    public void setValidateIvyFiles(boolean validateIvyFiles) {
        this.validateIvyFiles = validateIvyFiles;
    }

    public boolean isResolveTransitively() {
        return resolveTransitively;
    }

    public void setResolveTransitively(boolean resolveTransitively) {
        this.resolveTransitively = resolveTransitively;
    }

    public boolean isResolveCacheOnly() {
        return resolveCacheOnly;
    }

    public void setResolveCacheOnly(boolean resolveCacheOnly) {
        this.resolveCacheOnly = resolveCacheOnly;
    }

    public boolean isResolveInBackground() {
        return resolveInBackground;
    }

    public void setResolveInBackground(boolean resolveInBackground) {
        this.resolveInBackground = resolveInBackground;
    }

    public boolean isAlwaysAttachSources() {
        return alwaysAttachSources;
    }

    public void setAlwaysAttachSources(boolean alwaysAttachSources) {
        this.alwaysAttachSources = alwaysAttachSources;
    }

    public boolean isAlwaysAttachJavadocs() {
        return alwaysAttachJavadocs;
    }

    public void setAlwaysAttachJavadocs(boolean alwaysAttachJavadocs) {
        this.alwaysAttachJavadocs = alwaysAttachJavadocs;
    }

    public boolean isUseCustomIvySettings() {
        return useCustomIvySettings;
    }

    public void setUseCustomIvySettings(boolean useCustomIvySettings) {
        this.useCustomIvySettings = useCustomIvySettings;
    }

    public PropertiesSettings getPropertiesSettings() {
        return propertiesSettings;
    }

    public void setPropertiesSettings(PropertiesSettings propertiesSettings) {
        this.propertiesSettings = propertiesSettings;
    }

    public boolean isDetectDependenciesOnOtherModules() {
        return detectDependenciesOnOtherModules;
    }

    public void setDetectDependenciesOnOtherModules(boolean detectDependenciesOnOtherModules) {
        this.detectDependenciesOnOtherModules = detectDependenciesOnOtherModules;
    }

    public boolean isDetectDependenciesOnOtherModulesOfSameVersion() {
        return detectDependenciesOnOtherModulesOfSameVersion;
    }

    public void setDetectDependenciesOnOtherModulesOfSameVersion(boolean detectDependenciesOnOtherModulesOfSameVersion) {
        this.detectDependenciesOnOtherModulesOfSameVersion = detectDependenciesOnOtherModulesOfSameVersion;
    }

    public String getIvyLogLevelThreshold() {
        return ivyLogLevelThreshold;
    }

    public void setIvyLogLevelThreshold(String ivyLogLevelThreshold) {
        this.ivyLogLevelThreshold = ivyLogLevelThreshold;
    }

    public ArtifactTypeSettings getArtifactTypeSettings() {
        return artifactTypeSettings;
    }

    public void setArtifactTypeSettings(ArtifactTypeSettings artifactTypeSettings) {
        this.artifactTypeSettings = artifactTypeSettings;
    }

    public Map<String, DependencyScope> getDependencyScopes() {
        return dependencyScopes;
    }

    public void setDependencyScopes(Map<String, DependencyScope> dependencyScopes) {
        this.dependencyScopes = new HashMap<>(dependencyScopes);
    }

    public void updateResolveOptions(ResolveOptions options) {
        options.setValidate(isValidateIvyFiles());
        options.setTransitive(isResolveTransitively());
        options.setUseCacheOnly(isResolveCacheOnly());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GeneralIvyIdeaSettings)) {
            return false;
        }
        GeneralIvyIdeaSettings that = (GeneralIvyIdeaSettings) o;
        return useCustomIvySettings == that.useCustomIvySettings &&
               validateIvyFiles == that.validateIvyFiles &&
               resolveTransitively == that.resolveTransitively &&
               resolveCacheOnly == that.resolveCacheOnly &&
               resolveInBackground == that.resolveInBackground &&
               alwaysAttachSources == that.alwaysAttachSources &&
               alwaysAttachJavadocs == that.alwaysAttachJavadocs &&
               detectDependenciesOnOtherModules == that.detectDependenciesOnOtherModules &&
               detectDependenciesOnOtherModulesOfSameVersion == that.detectDependenciesOnOtherModulesOfSameVersion &&
               Objects.equals(ivySettingsFile, that.ivySettingsFile) &&
               Objects.equals(ivyLogLevelThreshold, that.ivyLogLevelThreshold) &&
               Objects.equals(dependencyScopes, that.dependencyScopes) &&
               Objects.equals(artifactTypeSettings, that.artifactTypeSettings) &&
               Objects.equals(propertiesSettings, that.propertiesSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(useCustomIvySettings,
                            ivySettingsFile,
                            validateIvyFiles,
                            resolveTransitively,
                            resolveCacheOnly,
                            resolveInBackground,
                            alwaysAttachSources,
                            alwaysAttachJavadocs,
                            detectDependenciesOnOtherModules,
                            detectDependenciesOnOtherModulesOfSameVersion,
                            ivyLogLevelThreshold,
                            dependencyScopes,
                            artifactTypeSettings,
                            propertiesSettings);
    }
}
