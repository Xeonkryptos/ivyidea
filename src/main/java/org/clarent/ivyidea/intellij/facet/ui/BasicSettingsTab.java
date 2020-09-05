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

package org.clarent.ivyidea.intellij.facet.ui;

import com.intellij.facet.Facet;
import com.intellij.facet.ui.FacetEditorContext;
import com.intellij.facet.ui.FacetEditorTab;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.UserActivityWatcher;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.DocumentEvent;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.apache.ivy.core.settings.IvySettings;
import org.clarent.ivyidea.config.IvyIdeaConfigHelper;
import org.clarent.ivyidea.exception.IvySettingsFileReadException;
import org.clarent.ivyidea.exception.IvySettingsNotFoundException;
import org.clarent.ivyidea.intellij.facet.config.IvyIdeaFacetConfiguration;
import org.clarent.ivyidea.intellij.facet.ui.components.ConfigurationScopeTable;
import org.clarent.ivyidea.intellij.facet.ui.components.ConfigurationScopeTableModel;
import org.clarent.ivyidea.intellij.facet.ui.components.ConfigurationSelectionTable;
import org.clarent.ivyidea.intellij.facet.ui.components.ConfigurationSelectionTableModel;
import org.clarent.ivyidea.ivy.IvyUtil;
import org.clarent.ivyidea.util.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Guy Mahieu
 */

public class BasicSettingsTab extends FacetEditorTab {

    private static final Logger LOGGER = Logger.getLogger(BasicSettingsTab.class.getName());

    private com.intellij.openapi.ui.TextFieldWithBrowseButton txtIvyFile;
    private JPanel pnlRoot;
    private JCheckBox chkOverrideProjectIvySettings;
    private TextFieldWithBrowseButton txtIvySettingsFile;
    private JCheckBox chkOnlyResolveSpecificConfigs;
    private ConfigurationSelectionTable tblConfigurationSelection;
    private ConfigurationScopeTable tblConfigurationScope;
    private JLabel lblIvyFileMessage;
    private JRadioButton rbnUseDefaultIvySettings;
    private JRadioButton rbnUseCustomIvySettings;
    private final FacetEditorContext editorContext;
    private final PropertiesSettingsTab propertiesSettingsTab;
    private boolean modified;
    private boolean foundConfigsBefore = false;

    private Set<Configuration> selectedConfigurations = new HashSet<>();
    private Map<String, DependencyScope> dependencyScopes = new HashMap<>();

    public BasicSettingsTab(@NotNull FacetEditorContext editorContext, @NotNull PropertiesSettingsTab propertiesSettingsTab) {
        this.editorContext = editorContext;
        this.propertiesSettingsTab = propertiesSettingsTab;
        this.propertiesSettingsTab.reset();

        UserActivityWatcher watcher = new UserActivityWatcher();
        watcher.addUserActivityListener(() -> modified = true);
        watcher.register(pnlRoot);

        txtIvyFile.addBrowseFolderListener("Select ivy file", "", editorContext.getProject(), new FileChooserDescriptor(true, false, false, false, false, false));
        txtIvySettingsFile.addBrowseFolderListener("Select ivy settings file", "", editorContext.getProject(), new FileChooserDescriptor(true, false, false, false, false, false));

        txtIvyFile.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
            public void textChanged(DocumentEvent e) {
                reloadIvyFile();
            }
        });
        chkOverrideProjectIvySettings.addChangeListener(e -> updateIvySettingsUIState());

        chkOnlyResolveSpecificConfigs.addChangeListener(e -> updateConfigurationsTable());

        rbnUseCustomIvySettings.addChangeListener(e -> updateIvySettingsFileTextfield());
    }

    private void updateUI() {
        updateIvySettingsFileTextfield();
        updateConfigurationsTable();
        updateIvySettingsUIState();
        reloadIvyFile();
    }

    private void updateIvySettingsFileTextfield() {
        txtIvySettingsFile.setEnabled(chkOverrideProjectIvySettings.isSelected() && rbnUseCustomIvySettings.isSelected());
    }

    private void updateConfigurationsTable() {
        tblConfigurationSelection.setEditable(chkOnlyResolveSpecificConfigs.isSelected());
    }

    private void updateIvySettingsUIState() {
        rbnUseCustomIvySettings.setEnabled(chkOverrideProjectIvySettings.isSelected());
        rbnUseDefaultIvySettings.setEnabled(chkOverrideProjectIvySettings.isSelected());
        updateIvySettingsFileTextfield();
    }

    @Override
    public void onTabEntering() {
        reloadIvyFile();
    }

    public void reloadIvyFile() {
        final Set<Configuration> allConfigurations;
        try {
            allConfigurations = loadConfigurations();
            chkOnlyResolveSpecificConfigs.setEnabled(allConfigurations != null);
            if (allConfigurations != null) {
                LOGGER.info("Detected configs in file " + txtIvyFile.getText() + ": " + allConfigurations.toString());
                tblConfigurationSelection.setModel(new ConfigurationSelectionTableModel(allConfigurations, getNames(selectedConfigurations)));
                tblConfigurationScope.setModel(new ConfigurationScopeTableModel(allConfigurations, dependencyScopes));
                lblIvyFileMessage.setText("");
                foundConfigsBefore = true;
            } else {
                File ivyFile = new File(txtIvyFile.getText());
                if (ivyFile.isDirectory() || !ivyFile.exists()) {
                    lblIvyFileMessage.setText("Please enter the name of an existing ivy file.");
                } else {
                    lblIvyFileMessage.setText("Warning: No configurations could be found in the given ivy file");
                }
                if (foundConfigsBefore) {
                    selectedConfigurations = tblConfigurationSelection.getSelectedConfigurations();
                    dependencyScopes = tblConfigurationScope.getDependencyScopes();
                }
                tblConfigurationSelection.setModel(new ConfigurationSelectionTableModel());
                tblConfigurationScope.setModel(new ConfigurationScopeTableModel());
                foundConfigsBefore = false;
            }
        } catch (ParseException e1) {
            // TODO: provide link to error display dialog with full exception
            lblIvyFileMessage.setText("Error parsing the file. If you use properties or specific ivy settings, configure those first.");
        } catch (IvySettingsNotFoundException e) {
            lblIvyFileMessage.setText("Could not find the settings file. Configure the settings file here or in the project settings first.");
        } catch (IvySettingsFileReadException e) {
            lblIvyFileMessage.setText("Error parsing the settings file. If you use properties, configure those first.");
        }
    }

    private Set<Configuration> loadConfigurations() throws IvySettingsNotFoundException, IvySettingsFileReadException, ParseException {
        return IvyUtil.loadConfigurations(txtIvyFile.getText(), createIvyEngineForCurrentSettingsInUI());
    }

    @NotNull
    private Ivy createIvyEngineForCurrentSettingsInUI() throws IvySettingsNotFoundException, IvySettingsFileReadException {
        final Module module = this.editorContext.getModule();
        Project project = module.getProject();
        final IvySettings
                ivySettings =
                IvyIdeaConfigHelper.createConfiguredIvySettings(module, this.getIvySettingsFileNameForCurrentSettingsInUI(project), getPropertiesForCurrentSettingsInUI(project));
        return IvyUtil.createConfiguredIvyEngine(module, ivySettings);
    }

    @Nullable
    private String getIvySettingsFileNameForCurrentSettingsInUI(Project project) throws IvySettingsNotFoundException {
        if (chkOverrideProjectIvySettings.isSelected()) {
            if (rbnUseCustomIvySettings.isSelected()) {
                return txtIvySettingsFile.getTextField().getText();
            }
        } else {
            return IvyIdeaConfigHelper.getIvySettingsFile(project);
        }
        return null;
    }

    private Properties getPropertiesForCurrentSettingsInUI(Project project) throws IvySettingsNotFoundException, IvySettingsFileReadException {
        final List<String> propertiesFiles = new ArrayList<>(propertiesSettingsTab.getFileNames());
        // TODO: only include the project properties files if this option is chosen on the screen.
        //          for now this is not configurable yet - so it always is true
        boolean includeProjectProperties = true;
        //noinspection ConstantConditions
        if (includeProjectProperties) {
            propertiesFiles.addAll(IvyIdeaConfigHelper.getPropertiesFiles(project));
        }
        return IvyIdeaConfigHelper.loadProperties(editorContext.getModule(), propertiesFiles);
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "General";
    }

    @NotNull
    @Override
    public JComponent createComponent() {
        return pnlRoot;
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    @Override
    public void apply() {
        final Facet<?> facet = editorContext.getFacet();
        IvyIdeaFacetConfiguration configuration = (IvyIdeaFacetConfiguration) facet.getConfiguration();
        configuration.setUseProjectSettings(!chkOverrideProjectIvySettings.isSelected());
        configuration.setUseCustomIvySettings(rbnUseCustomIvySettings.isSelected());
        configuration.setIvySettingsFile(txtIvySettingsFile.getText());
        configuration.setOnlyResolveSelectedConfigs(chkOnlyResolveSpecificConfigs.isSelected());
        configuration.setConfigsToResolve(getNames(tblConfigurationSelection.getSelectedConfigurations()));
        configuration.setDependencyScopes(tblConfigurationScope.getDependencyScopes());
        configuration.setIvyFile(txtIvyFile.getText());
    }

    @NotNull
    private static Set<String> getNames(@NotNull Set<Configuration> selectedConfigurations) {
        Set<String> result = new TreeSet<>();
        for (Configuration selectedConfiguration : selectedConfigurations) {
            result.add(selectedConfiguration.getName());
        }
        return result;
    }

    @Override
    public void reset() {
        final Facet<?> facet = editorContext.getFacet();
        IvyIdeaFacetConfiguration configuration = (IvyIdeaFacetConfiguration) facet.getConfiguration();
        txtIvyFile.setText(configuration.getIvyFile());
        chkOverrideProjectIvySettings.setSelected(!configuration.isUseProjectSettings());
        txtIvySettingsFile.setText(configuration.getIvySettingsFile());
        chkOnlyResolveSpecificConfigs.setSelected(configuration.isOnlyResolveSelectedConfigs());
        rbnUseCustomIvySettings.setSelected(configuration.isUseCustomIvySettings());
        rbnUseDefaultIvySettings.setSelected(!configuration.isUseCustomIvySettings());
        Set<Configuration> allConfigurations;
        try {
            allConfigurations = loadConfigurations();
        } catch (ParseException | IvySettingsNotFoundException | IvySettingsFileReadException e) {
            allConfigurations = null;
        }
        if (StringUtils.isNotBlank(configuration.getIvyFile())) {
            if (allConfigurations != null) {
                tblConfigurationSelection.setModel(new ConfigurationSelectionTableModel(allConfigurations, configuration.getConfigsToResolve()));
                tblConfigurationScope.setModel(new ConfigurationScopeTableModel(allConfigurations, configuration.getDependencyScopes()));
            } else {
                tblConfigurationSelection.setModel(new ConfigurationSelectionTableModel());
                tblConfigurationScope.setModel(new ConfigurationScopeTableModel());
            }
            selectedConfigurations = tblConfigurationSelection.getSelectedConfigurations();
            dependencyScopes = tblConfigurationScope.getDependencyScopes();
            updateConfigurationsTable();
        } else {
            tblConfigurationSelection.setModel(new ConfigurationSelectionTableModel());
            tblConfigurationScope.setModel(new ConfigurationScopeTableModel());
            selectedConfigurations = new HashSet<>();
            dependencyScopes = new HashMap<>();
            tblConfigurationSelection.setEditable(false);
            tblConfigurationScope.setEditable(false);
        }
        updateUI();
    }

    @Override
    public void disposeUIResources() {
    }

    private void createUIComponents() {
    }

}
