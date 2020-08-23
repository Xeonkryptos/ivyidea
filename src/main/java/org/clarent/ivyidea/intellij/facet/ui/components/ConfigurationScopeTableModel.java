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

package org.clarent.ivyidea.intellij.facet.ui.components;

import com.intellij.openapi.roots.DependencyScope;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.clarent.ivyidea.util.StringUtils;

import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * @author Guy Mahieu
 */
public class ConfigurationScopeTableModel extends AbstractTableModel {

    private static final int COLUMN_NAME = 0;
    private static final int COLUMN_DESCRIPTION = 1;
    private static final int COLUMN_DEPENDENCY_SCOPE = 2;

    private final List<Configuration> data;
    private final Map<String, DependencyScope> dependencyScopes;

    private boolean editable = true;

    public ConfigurationScopeTableModel() {
        this.data = Collections.emptyList();
        this.dependencyScopes = Collections.emptyMap();
    }

    public ConfigurationScopeTableModel(Collection<Configuration> data, Map<String, DependencyScope> dependencyScopes) {
        this.data = new ArrayList<>(data);
        this.dependencyScopes = new HashMap<>();

        for (Configuration configuration : data) {
            String configurationName = configuration.getName();
            if (dependencyScopes.containsKey(configurationName)) {
                DependencyScope dependencyScope = dependencyScopes.get(configurationName);
                this.dependencyScopes.put(configurationName, dependencyScope);
            }
        }
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return editable && columnIndex == COLUMN_DEPENDENCY_SCOPE;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == COLUMN_DEPENDENCY_SCOPE && aValue instanceof String) {
            Configuration configuration = getConfigurationAt(rowIndex);
            String configurationName = configuration.getName();
            if (StringUtils.isNotBlank((String) aValue)) {
                DependencyScope dependencyScope = DependencyScope.valueOf(((String) aValue).toUpperCase());
                dependencyScopes.put(configurationName, dependencyScope);
            } else {
                dependencyScopes.put(configurationName, null);
            }
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final Configuration configuration = getConfigurationAt(rowIndex);
        String configurationName = configuration.getName();
        if (columnIndex == COLUMN_NAME) {
            return configurationName;
        }
        if (columnIndex == COLUMN_DESCRIPTION) {
            return configuration.getDescription();
        }
        if (columnIndex == COLUMN_DEPENDENCY_SCOPE) {
            DependencyScope dependencyScope = dependencyScopes.get(configurationName);
            return dependencyScope != null ? dependencyScope.toString() : "";
        }
        return null;
    }

    public Configuration getConfigurationAt(int rowIndex) {
        return data.get(rowIndex);
    }

    public Map<String, DependencyScope> getDependencyScopes() {
        return Collections.unmodifiableMap(dependencyScopes);
    }
}
