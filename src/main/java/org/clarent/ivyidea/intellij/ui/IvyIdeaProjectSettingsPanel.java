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

package org.clarent.ivyidea.intellij.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.UserActivityWatcher;
import org.clarent.ivyidea.config.model.IvyIdeaProjectSettings;
import org.clarent.ivyidea.config.model.PropertiesSettings;
import org.clarent.ivyidea.config.ui.orderedfilelist.OrderedFileList;
import org.clarent.ivyidea.logging.IvyLogLevel;
import org.clarent.ivyidea.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.clarent.ivyidea.config.model.ArtifactTypeSettings.DependencyCategory.*;

/**
 * @author Guy Mahieu
 */

public class IvyIdeaProjectSettingsPanel {

    private boolean modified;
    private TextFieldWithBrowseButton txtIvySettingsFile;
    private JPanel projectSettingsPanel;
    private JCheckBox chkValidateIvyFiles;
    private JTabbedPane tabbedPane1;
    private JLabel lblIvySettingsErrorMessage;
    private JRadioButton useIvyDefaultRadioButton;
    private JRadioButton useYourOwnIvySettingsRadioButton;
    private JPanel pnlPropertiesFiles;
    private JComboBox<IvyLogLevel> ivyLogLevelComboBox;
    private JPanel pnlIvyLogging;
    private JPanel pnlLibraryNaming;
    private JTextField txtClassesArtifactTypes;
    private JTextField txtSourcesArtifactTypes;
    private JTextField txtJavadocArtifactTypes;
    private JCheckBox chkResolveTransitively;
    private JCheckBox chkUseCacheOnly;
    private JCheckBox chkBackground;
    private JCheckBox autoAttachSources;
    private JCheckBox autoAttachJavadocs;
    private JCheckBox detectDependenciesOnOtherModules;
    private JCheckBox detectDependenciesOnOtherModulesOfSameVersion;
    private JTextField txtDependencyScopeRuntime;
    private JTextField txtDependencyScopeProvided;
    private JTextField txtDependencyScopeTest;
    private JPanel pnlIvyFiles;
    private JPanel pnlArtefactTypes;
    private IvyIdeaProjectSettings internalState;
    private OrderedFileList orderedFileList;
    private final Project project;

    public IvyIdeaProjectSettingsPanel(Project project, IvyIdeaProjectSettings state) {
        this.project = project;
        this.internalState = state;

        txtIvySettingsFile.addBrowseFolderListener("Select Ivy Settings File",
                null,
                project,
                new FileChooserDescriptor(true, false, false, false, false, false));

        wireActivityWatchers();
        wireIvySettingsRadioButtons();
    }

    private void wireIvySettingsRadioButtons() {
        useYourOwnIvySettingsRadioButton.addChangeListener(e -> txtIvySettingsFile.setEnabled(
                useYourOwnIvySettingsRadioButton.isSelected()));
    }

    private void wireActivityWatchers() {
        UserActivityWatcher watcher = new UserActivityWatcher();
        watcher.addUserActivityListener(() -> modified = true);
        watcher.register(projectSettingsPanel);
    }

    public JComponent createComponent() {
        return projectSettingsPanel;
    }

    public boolean isModified() {
        return modified;
    }

    private List<String> getPropertiesFiles() {
        return orderedFileList.getFileNames();
    }

    private void setPropertiesFiles(List<String> fileNames) {
        orderedFileList.setFileNames(fileNames);
    }

    public void apply() {
        if (internalState == null) {
            internalState = new IvyIdeaProjectSettings();
        }
        internalState.setIvySettingsFile(txtIvySettingsFile.getText());
        internalState.setValidateIvyFiles(chkValidateIvyFiles.isSelected());
        internalState.setResolveTransitively(chkResolveTransitively.isSelected());
        internalState.setResolveCacheOnly(chkUseCacheOnly.isSelected());
        internalState.setResolveInBackground(chkBackground.isSelected());
        internalState.setAlwaysAttachSources(autoAttachSources.isSelected());
        internalState.setAlwaysAttachJavadocs(autoAttachJavadocs.isSelected());
        internalState.setUseCustomIvySettings(useYourOwnIvySettingsRadioButton.isSelected());
        internalState.setDetectDependenciesOnOtherModules(detectDependenciesOnOtherModules.isSelected());
        internalState.setDetectDependenciesOnOtherModulesOfSameVersion(detectDependenciesOnOtherModulesOfSameVersion.isSelected());
        final PropertiesSettings propertiesSettings = new PropertiesSettings();
        propertiesSettings.setPropertyFiles(getPropertiesFiles());
        internalState.setPropertiesSettings(propertiesSettings);
        final Object selectedLogLevel = ivyLogLevelComboBox.getSelectedItem();
        internalState.setIvyLogLevelThreshold(selectedLogLevel == null ? IvyLogLevel.None.name() : selectedLogLevel.toString());
        internalState.getArtifactTypeSettings().setTypesForCategory(Classes, txtClassesArtifactTypes.getText());
        internalState.getArtifactTypeSettings().setTypesForCategory(Sources, txtSourcesArtifactTypes.getText());
        internalState.getArtifactTypeSettings().setTypesForCategory(Javadoc, txtJavadocArtifactTypes.getText());

        Map<String, DependencyScope> dependencyScopes = new HashMap<>();
        String scopeRuntimeText = txtDependencyScopeRuntime.getText();
        String scopeProvidedText = txtDependencyScopeProvided.getText();
        String scopeTestText = txtDependencyScopeTest.getText();
        addDependencyScopeConfigurations(scopeRuntimeText, DependencyScope.RUNTIME, dependencyScopes);
        addDependencyScopeConfigurations(scopeProvidedText, DependencyScope.PROVIDED, dependencyScopes);
        addDependencyScopeConfigurations(scopeTestText, DependencyScope.TEST, dependencyScopes);
        internalState.setDependencyScopes(dependencyScopes);
    }

    public void reset() {
        IvyIdeaProjectSettings config = internalState;
        if (config == null) {
            config = new IvyIdeaProjectSettings();
        }
        txtIvySettingsFile.setText(config.getIvySettingsFile());
        chkValidateIvyFiles.setSelected(config.isValidateIvyFiles());
        chkResolveTransitively.setSelected(config.isResolveTransitively());
        chkUseCacheOnly.setSelected(config.isResolveCacheOnly());
        chkBackground.setSelected(config.isResolveInBackground());
        autoAttachSources.setSelected(config.isAlwaysAttachSources());
        autoAttachJavadocs.setSelected(config.isAlwaysAttachJavadocs());
        useYourOwnIvySettingsRadioButton.setSelected(config.isUseCustomIvySettings());
        detectDependenciesOnOtherModules.setSelected(config.isDetectDependenciesOnOtherModules());
        detectDependenciesOnOtherModulesOfSameVersion.setSelected(config.isDetectDependenciesOnOtherModulesOfSameVersion());
        setPropertiesFiles(config.getPropertiesSettings().getPropertyFiles());
        ivyLogLevelComboBox.setSelectedItem(IvyLogLevel.fromName(config.getIvyLogLevelThreshold()));
        txtSourcesArtifactTypes.setText(config.getArtifactTypeSettings().getTypesStringForCategory(Sources));
        txtClassesArtifactTypes.setText(config.getArtifactTypeSettings().getTypesStringForCategory(Classes));
        txtJavadocArtifactTypes.setText(config.getArtifactTypeSettings().getTypesStringForCategory(Javadoc));

        StringBuilder textScopeRuntime = new StringBuilder();
        StringBuilder textScopeProvided = new StringBuilder();
        StringBuilder textScopeTest = new StringBuilder();
        for (Map.Entry<String, DependencyScope> entry : internalState.getDependencyScopes().entrySet()) {
            String configurationName = entry.getKey();
            DependencyScope dependencyScope = entry.getValue();
            StringBuilder currentBuilder;
            switch (dependencyScope) {
                case RUNTIME:
                    currentBuilder = textScopeRuntime;
                    break;
                case PROVIDED:
                    currentBuilder = textScopeProvided;
                    break;
                case TEST:
                    currentBuilder = textScopeTest;
                    break;
                default:
                    continue;
            }
            currentBuilder.append(configurationName).append(", ");
        }
        updateDependencyScopeTextField(textScopeRuntime, txtDependencyScopeRuntime);
        updateDependencyScopeTextField(textScopeProvided, txtDependencyScopeProvided);
        updateDependencyScopeTextField(textScopeTest, txtDependencyScopeTest);
    }

    private void addDependencyScopeConfigurations(String scopeConfigText, DependencyScope dependencyScope, Map<String, DependencyScope> dependencyScopes) {
        if (!StringUtils.isBlank(scopeConfigText)) {
            String[] configurations = scopeConfigText.split("\\s*,\\s*");
            for (String configuration : configurations) {
                dependencyScopes.put(configuration, dependencyScope);
            }
        }
    }

    private void updateDependencyScopeTextField(StringBuilder textScopeBuilder, JTextField txtDependencyScope) {
        if (textScopeBuilder.length() > 0) {
            textScopeBuilder.setLength(textScopeBuilder.length() - 2);
        }
        txtDependencyScope.setText(textScopeBuilder.toString());
    }

    private void createUIComponents() {
        pnlPropertiesFiles = new JPanel(new BorderLayout());
        orderedFileList = new OrderedFileList(project);
        pnlPropertiesFiles.add(orderedFileList.getRootPanel(), BorderLayout.CENTER);
        ivyLogLevelComboBox = new ComboBox<>(IvyLogLevel.values());
    }
}
