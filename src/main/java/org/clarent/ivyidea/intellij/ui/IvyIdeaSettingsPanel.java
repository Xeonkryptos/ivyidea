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

import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.UserActivityWatcher;
import org.clarent.ivyidea.config.model.IvyIdeaApplicationSettings;
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

public class IvyIdeaSettingsPanel {

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
    private EditorTextField txtIvyApplicationTemplateEditor;
    private EditorTextField txtIvyProjectTemplateEditor;
    private JPanel pnlIvyFiles;
    private JPanel pnlArtefactTypes;
    private IvyIdeaApplicationSettings applicationSettingsState;
    private IvyIdeaProjectSettings projectSettingsState;
    private OrderedFileList orderedFileList;
    private final Project project;

    public IvyIdeaSettingsPanel(Project project, IvyIdeaApplicationSettings state, IvyIdeaProjectSettings projectState) {
        this.project = project;
        this.applicationSettingsState = state;
        this.projectSettingsState = projectState;

        txtIvySettingsFile.addBrowseFolderListener("Select Ivy Settings File",
                null,
                project,
                new FileChooserDescriptor(true, false, false, false, false, false));

        wireActivityWatchers();
        detectDependenciesOnOtherModules.addChangeListener(e -> {
            if (!detectDependenciesOnOtherModules.isSelected()) {
                detectDependenciesOnOtherModulesOfSameVersion.setSelected(false);
            }
        });
        detectDependenciesOnOtherModulesOfSameVersion.addChangeListener(e -> {
            if (detectDependenciesOnOtherModulesOfSameVersion.isSelected()) {
                detectDependenciesOnOtherModules.setSelected(true);
            }
        });
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
        if (applicationSettingsState == null) {
            applicationSettingsState = new IvyIdeaApplicationSettings();
        }
        if (projectSettingsState == null) {
            projectSettingsState = new IvyIdeaProjectSettings();
        }
        applicationSettingsState.setIvySettingsFile(txtIvySettingsFile.getText());
        applicationSettingsState.setValidateIvyFiles(chkValidateIvyFiles.isSelected());
        applicationSettingsState.setResolveTransitively(chkResolveTransitively.isSelected());
        applicationSettingsState.setResolveCacheOnly(chkUseCacheOnly.isSelected());
        applicationSettingsState.setResolveInBackground(chkBackground.isSelected());
        applicationSettingsState.setAlwaysAttachSources(autoAttachSources.isSelected());
        applicationSettingsState.setAlwaysAttachJavadocs(autoAttachJavadocs.isSelected());
        applicationSettingsState.setUseCustomIvySettings(useYourOwnIvySettingsRadioButton.isSelected());
        applicationSettingsState.setDetectDependenciesOnOtherModules(detectDependenciesOnOtherModules.isSelected());
        applicationSettingsState.setDetectDependenciesOnOtherModulesOfSameVersion(detectDependenciesOnOtherModulesOfSameVersion.isSelected());
        final PropertiesSettings propertiesSettings = new PropertiesSettings();
        propertiesSettings.setPropertyFiles(getPropertiesFiles());
        applicationSettingsState.setPropertiesSettings(propertiesSettings);
        final Object selectedLogLevel = ivyLogLevelComboBox.getSelectedItem();
        applicationSettingsState.setIvyLogLevelThreshold(selectedLogLevel == null ? IvyLogLevel.None.name() : selectedLogLevel.toString());
        applicationSettingsState.getArtifactTypeSettings().setTypesForCategory(Classes, txtClassesArtifactTypes.getText());
        applicationSettingsState.getArtifactTypeSettings().setTypesForCategory(Sources, txtSourcesArtifactTypes.getText());
        applicationSettingsState.getArtifactTypeSettings().setTypesForCategory(Javadoc, txtJavadocArtifactTypes.getText());

        Map<String, DependencyScope> dependencyScopes = new HashMap<>();
        String scopeRuntimeText = txtDependencyScopeRuntime.getText();
        String scopeProvidedText = txtDependencyScopeProvided.getText();
        String scopeTestText = txtDependencyScopeTest.getText();
        addDependencyScopeConfigurations(scopeRuntimeText, DependencyScope.RUNTIME, dependencyScopes);
        addDependencyScopeConfigurations(scopeProvidedText, DependencyScope.PROVIDED, dependencyScopes);
        addDependencyScopeConfigurations(scopeTestText, DependencyScope.TEST, dependencyScopes);
        applicationSettingsState.setDependencyScopes(dependencyScopes);

        String ivyApplicationTemplateContent = txtIvyApplicationTemplateEditor.getText();
        applicationSettingsState.setIvyTemplateContent(ivyApplicationTemplateContent);

        String ivyProjectTemplateContent = txtIvyProjectTemplateEditor.getText();
        projectSettingsState.setIvyTemplateContent(ivyProjectTemplateContent);
    }

    public void reset() {
        IvyIdeaApplicationSettings applicationConfig = applicationSettingsState;
        if (applicationConfig == null) {
            applicationConfig = new IvyIdeaApplicationSettings();
        }
        IvyIdeaProjectSettings projectConfig = projectSettingsState;
        if (projectConfig == null) {
            projectConfig = new IvyIdeaProjectSettings();
        }
        txtIvySettingsFile.setText(applicationConfig.getIvySettingsFile());
        chkValidateIvyFiles.setSelected(applicationConfig.isValidateIvyFiles());
        chkResolveTransitively.setSelected(applicationConfig.isResolveTransitively());
        chkUseCacheOnly.setSelected(applicationConfig.isResolveCacheOnly());
        chkBackground.setSelected(applicationConfig.isResolveInBackground());
        autoAttachSources.setSelected(applicationConfig.isAlwaysAttachSources());
        autoAttachJavadocs.setSelected(applicationConfig.isAlwaysAttachJavadocs());
        useYourOwnIvySettingsRadioButton.setSelected(applicationConfig.isUseCustomIvySettings());
        detectDependenciesOnOtherModules.setSelected(applicationConfig.isDetectDependenciesOnOtherModules());
        detectDependenciesOnOtherModulesOfSameVersion.setSelected(applicationConfig.isDetectDependenciesOnOtherModulesOfSameVersion());
        setPropertiesFiles(applicationConfig.getPropertiesSettings().getPropertyFiles());
        ivyLogLevelComboBox.setSelectedItem(IvyLogLevel.fromName(applicationConfig.getIvyLogLevelThreshold()));
        txtSourcesArtifactTypes.setText(applicationConfig.getArtifactTypeSettings().getTypesStringForCategory(Sources));
        txtClassesArtifactTypes.setText(applicationConfig.getArtifactTypeSettings().getTypesStringForCategory(Classes));
        txtJavadocArtifactTypes.setText(applicationConfig.getArtifactTypeSettings().getTypesStringForCategory(Javadoc));

        StringBuilder textScopeRuntime = new StringBuilder();
        StringBuilder textScopeProvided = new StringBuilder();
        StringBuilder textScopeTest = new StringBuilder();
        for (Map.Entry<String, DependencyScope> entry : applicationConfig.getDependencyScopes().entrySet()) {
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

        String ivyApplicationTemplateContent = applicationConfig.getIvyTemplateContent();
        txtIvyApplicationTemplateEditor.setText(ivyApplicationTemplateContent);

        String ivyProjectTemplateContent = projectConfig.getIvyTemplateContent();
        txtIvyProjectTemplateEditor.setText(ivyProjectTemplateContent);
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
        txtIvyApplicationTemplateEditor = new EditorTextField(project, XmlFileType.INSTANCE);
        txtIvyProjectTemplateEditor = new EditorTextField(project, XmlFileType.INSTANCE);
    }
}
