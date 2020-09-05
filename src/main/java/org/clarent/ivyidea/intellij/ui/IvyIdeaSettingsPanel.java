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
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.UserActivityWatcher;
import com.intellij.uiDesigner.core.GridConstraints;
import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.clarent.ivyidea.config.model.GeneralIvyIdeaSettings;
import org.clarent.ivyidea.config.model.IvyIdeaApplicationSettings;
import org.clarent.ivyidea.config.model.IvyIdeaProjectSettings;
import org.clarent.ivyidea.config.model.PropertiesSettings;
import org.clarent.ivyidea.config.ui.orderedfilelist.OrderedFileList;
import org.clarent.ivyidea.intellij.IvyIdeaApplicationService;
import org.clarent.ivyidea.intellij.IvyIdeaProjectService;
import org.clarent.ivyidea.logging.IvyLogLevel;
import org.clarent.ivyidea.util.StringUtils;

import static org.clarent.ivyidea.config.model.ArtifactTypeSettings.DependencyCategory.Classes;
import static org.clarent.ivyidea.config.model.ArtifactTypeSettings.DependencyCategory.Javadoc;
import static org.clarent.ivyidea.config.model.ArtifactTypeSettings.DependencyCategory.Sources;

/**
 * @author Guy Mahieu
 */

public class IvyIdeaSettingsPanel {

    private final EditorTextField txtIvyApplicationTemplateEditor;
    private final EditorTextField txtIvyProjectTemplateEditor;
    private GeneralIvyIdeaSettings uiCurrentSettingsState;
    private final GeneralIvyIdeaSettings uiInternalApplicationSettingsState = new GeneralIvyIdeaSettings();
    private final GeneralIvyIdeaSettings uiInternalProjectSettingsState = new GeneralIvyIdeaSettings();
    private final IvyIdeaApplicationSettings applicationSettingsState;
    private final IvyIdeaProjectSettings projectSettingsState;
    private final OrderedFileList orderedFileList;
    private final UserActivityWatcher watcher = new UserActivityWatcher();

    private TextFieldWithBrowseButton txtIvySettingsFile;
    private JPanel projectSettingsPanel;
    private JCheckBox chkValidateIvyFiles;
    private JRadioButton useYourOwnIvySettingsRadioButton;
    private JPanel pnlPropertiesFiles;
    private JComboBox<IvyLogLevel> ivyLogLevelComboBox;
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
    private JRadioButton useApplicationSettingsRadioButton;
    private JRadioButton useProjectSettingsRadioButton;
    private JPanel ivyProjectTemplatePanel;
    private JPanel ivyApplicationTemplatePanel;
    private JRadioButton useIvyDefaultSettingsRadioButton;

    private boolean resettingStates = false;
    private boolean applyingStates = false;

    public IvyIdeaSettingsPanel(Project project) {
        applicationSettingsState = ServiceManager.getService(IvyIdeaApplicationService.class).getState();
        projectSettingsState = ServiceManager.getService(project, IvyIdeaProjectService.class).getState();

        uiInternalApplicationSettingsState.updateWith(applicationSettingsState.getGeneralIvyIdeaSettings());
        uiInternalProjectSettingsState.updateWith(projectSettingsState.getGeneralIvyIdeaSettings());

        if (projectSettingsState.isUseApplicationSettings()) {
            uiCurrentSettingsState = uiInternalApplicationSettingsState;
        } else {
            uiCurrentSettingsState = uiInternalProjectSettingsState;
        }

        txtIvyApplicationTemplateEditor = new EditorTextField(project, XmlFileType.INSTANCE);
        txtIvyApplicationTemplateEditor.setOneLineMode(false);
        txtIvyProjectTemplateEditor = new EditorTextField(project, XmlFileType.INSTANCE);
        txtIvyProjectTemplateEditor.setOneLineMode(false);

        ivyApplicationTemplatePanel.add(txtIvyApplicationTemplateEditor, new GridConstraints(2, 1, 1, 1, 0, 3, 3, 3, null, null, null, 0, false));
        ivyProjectTemplatePanel.add(txtIvyProjectTemplateEditor, new GridConstraints(2, 1, 1, 1, 0, 3, 3, 3, null, null, null, 0, false));

        txtIvySettingsFile.addBrowseFolderListener("Select Ivy Settings File", null, project, new FileChooserDescriptor(true, false, false, false, false, false));

        orderedFileList = new OrderedFileList(project);
        pnlPropertiesFiles.add(orderedFileList.getRootPanel(), BorderLayout.CENTER);

        wireActivityWatchers();
        detectDependenciesOnOtherModules.addActionListener(e -> {
            if (!detectDependenciesOnOtherModules.isSelected()) {
                detectDependenciesOnOtherModulesOfSameVersion.setSelected(false);
            }
        });
        detectDependenciesOnOtherModulesOfSameVersion.addActionListener(e -> {
            if (detectDependenciesOnOtherModulesOfSameVersion.isSelected()) {
                detectDependenciesOnOtherModules.setSelected(true);
            }
        });
        wireSettingsRadioButtons();
        resetUiState();
    }

    private void wireSettingsRadioButtons() {
        useYourOwnIvySettingsRadioButton.addItemListener(e -> txtIvySettingsFile.setEnabled(useYourOwnIvySettingsRadioButton.isSelected()));
        useApplicationSettingsRadioButton.addItemListener(e -> {
            if (useApplicationSettingsRadioButton.isSelected()) {
                applyInternalUiState();
                uiCurrentSettingsState = uiInternalApplicationSettingsState;
                resetUiState();
            }
        });
        useProjectSettingsRadioButton.addItemListener(e -> {
            if (useProjectSettingsRadioButton.isSelected()) {
                applyInternalUiState();
                uiCurrentSettingsState = uiInternalProjectSettingsState;
                resetUiState();
            }
        });
    }

    private void wireActivityWatchers() {
        watcher.register(projectSettingsPanel);
    }

    public JComponent createComponent() {
        return projectSettingsPanel;
    }

    public boolean isModified() {
        commitWatcherState();
        return watcher.isModified();
    }

    private List<String> getPropertiesFiles() {
        return orderedFileList.getFileNames();
    }

    private void setPropertiesFiles(List<String> fileNames) {
        orderedFileList.setFileNames(fileNames);
    }

    public void apply() {
        if (resettingStates) {
            return;
        }
        applyInternalUiState();

        applyingStates = true;
        applicationSettingsState.updateWith(uiInternalApplicationSettingsState);
        projectSettingsState.updateWith(uiInternalProjectSettingsState);
        applyingStates = false;
    }

    private void applyInternalUiState() {
        if (resettingStates) {
            return;
        }
        applyingStates = true;
        projectSettingsState.setUseApplicationSettings(useApplicationSettingsRadioButton.isSelected());
        uiCurrentSettingsState.setIvySettingsFile(txtIvySettingsFile.getText());
        uiCurrentSettingsState.setValidateIvyFiles(chkValidateIvyFiles.isSelected());
        uiCurrentSettingsState.setResolveTransitively(chkResolveTransitively.isSelected());
        uiCurrentSettingsState.setResolveCacheOnly(chkUseCacheOnly.isSelected());
        uiCurrentSettingsState.setResolveInBackground(chkBackground.isSelected());
        uiCurrentSettingsState.setAlwaysAttachSources(autoAttachSources.isSelected());
        uiCurrentSettingsState.setAlwaysAttachJavadocs(autoAttachJavadocs.isSelected());
        uiCurrentSettingsState.setUseCustomIvySettings(useYourOwnIvySettingsRadioButton.isSelected());
        uiCurrentSettingsState.setDetectDependenciesOnOtherModules(detectDependenciesOnOtherModules.isSelected());
        uiCurrentSettingsState.setDetectDependenciesOnOtherModulesOfSameVersion(detectDependenciesOnOtherModulesOfSameVersion.isSelected());
        final PropertiesSettings propertiesSettings = new PropertiesSettings();
        propertiesSettings.setPropertyFiles(getPropertiesFiles());
        uiCurrentSettingsState.setPropertiesSettings(propertiesSettings);
        final Object selectedLogLevel = ivyLogLevelComboBox.getSelectedItem();
        uiCurrentSettingsState.setIvyLogLevelThreshold(selectedLogLevel == null ? IvyLogLevel.None.name() : selectedLogLevel.toString());
        uiCurrentSettingsState.getArtifactTypeSettings().setTypesForCategory(Classes, txtClassesArtifactTypes.getText());
        uiCurrentSettingsState.getArtifactTypeSettings().setTypesForCategory(Sources, txtSourcesArtifactTypes.getText());
        uiCurrentSettingsState.getArtifactTypeSettings().setTypesForCategory(Javadoc, txtJavadocArtifactTypes.getText());

        Map<String, DependencyScope> dependencyScopes = new HashMap<>();
        String scopeRuntimeText = txtDependencyScopeRuntime.getText();
        String scopeProvidedText = txtDependencyScopeProvided.getText();
        String scopeTestText = txtDependencyScopeTest.getText();
        addDependencyScopeConfigurations(scopeRuntimeText, DependencyScope.RUNTIME, dependencyScopes);
        addDependencyScopeConfigurations(scopeProvidedText, DependencyScope.PROVIDED, dependencyScopes);
        addDependencyScopeConfigurations(scopeTestText, DependencyScope.TEST, dependencyScopes);
        uiCurrentSettingsState.setDependencyScopes(dependencyScopes);

        String ivyApplicationTemplateContent = txtIvyApplicationTemplateEditor.getText();
        applicationSettingsState.setIvyTemplateContent(ivyApplicationTemplateContent);

        String ivyProjectTemplateContent = txtIvyProjectTemplateEditor.getText();
        projectSettingsState.setIvyTemplateContent(ivyProjectTemplateContent);
        applyingStates = false;
    }

    public void reset() {
        if (applyingStates) {
            return;
        }
        resettingStates = true;
        uiInternalApplicationSettingsState.updateWith(applicationSettingsState.getGeneralIvyIdeaSettings());
        uiInternalProjectSettingsState.updateWith(projectSettingsState.getGeneralIvyIdeaSettings());
        resetUiState();
        resettingStates = false;
    }

    private void resetUiState() {
        if (applyingStates) {
            return;
        }
        resettingStates = true;
        if (projectSettingsState.isUseApplicationSettings()) {
            useApplicationSettingsRadioButton.setSelected(true);
        } else {
            useProjectSettingsRadioButton.setSelected(true);
        }
        String ivyApplicationTemplateContent = applicationSettingsState.getIvyTemplateContent();
        txtIvyApplicationTemplateEditor.setText(ivyApplicationTemplateContent);

        String ivyProjectTemplateContent = projectSettingsState.getIvyTemplateContent();
        txtIvyProjectTemplateEditor.setText(ivyProjectTemplateContent);

        txtIvySettingsFile.setText(uiCurrentSettingsState.getIvySettingsFile());
        chkValidateIvyFiles.setSelected(uiCurrentSettingsState.isValidateIvyFiles());
        chkResolveTransitively.setSelected(uiCurrentSettingsState.isResolveTransitively());
        chkUseCacheOnly.setSelected(uiCurrentSettingsState.isResolveCacheOnly());
        chkBackground.setSelected(uiCurrentSettingsState.isResolveInBackground());
        autoAttachSources.setSelected(uiCurrentSettingsState.isAlwaysAttachSources());
        autoAttachJavadocs.setSelected(uiCurrentSettingsState.isAlwaysAttachJavadocs());
        if (uiCurrentSettingsState.isUseCustomIvySettings()) {
            useYourOwnIvySettingsRadioButton.setSelected(true);
        } else {
            useIvyDefaultSettingsRadioButton.setSelected(true);
        }
        detectDependenciesOnOtherModules.setSelected(uiCurrentSettingsState.isDetectDependenciesOnOtherModules());
        detectDependenciesOnOtherModulesOfSameVersion.setSelected(uiCurrentSettingsState.isDetectDependenciesOnOtherModulesOfSameVersion());
        setPropertiesFiles(uiCurrentSettingsState.getPropertiesSettings().getPropertyFiles());
        ivyLogLevelComboBox.setSelectedItem(IvyLogLevel.fromName(uiCurrentSettingsState.getIvyLogLevelThreshold()));
        txtSourcesArtifactTypes.setText(uiCurrentSettingsState.getArtifactTypeSettings().getTypesStringForCategory(Sources));
        txtClassesArtifactTypes.setText(uiCurrentSettingsState.getArtifactTypeSettings().getTypesStringForCategory(Classes));
        txtJavadocArtifactTypes.setText(uiCurrentSettingsState.getArtifactTypeSettings().getTypesStringForCategory(Javadoc));

        StringBuilder textScopeRuntime = new StringBuilder();
        StringBuilder textScopeProvided = new StringBuilder();
        StringBuilder textScopeTest = new StringBuilder();
        for (Map.Entry<String, DependencyScope> entry : uiCurrentSettingsState.getDependencyScopes().entrySet()) {
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
        resettingStates = false;
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

    private void commitWatcherState() {
        if (watcher.isModified() &&
            uiInternalApplicationSettingsState.equals(applicationSettingsState.getGeneralIvyIdeaSettings()) &&
            uiInternalProjectSettingsState.equals(projectSettingsState.getGeneralIvyIdeaSettings()) &&
            Objects.equals(applicationSettingsState.getIvyTemplateContent(), txtIvyApplicationTemplateEditor.getText()) &&
            Objects.equals(projectSettingsState.getIvyTemplateContent(), txtIvyProjectTemplateEditor.getText()) &&
            projectSettingsState.isUseApplicationSettings() == useApplicationSettingsRadioButton.isSelected()) {
            watcher.commit();
        }
    }

    private void createUIComponents() {
        pnlPropertiesFiles = new JPanel(new BorderLayout());
        ivyLogLevelComboBox = new ComboBox<>(IvyLogLevel.values());
    }
}
