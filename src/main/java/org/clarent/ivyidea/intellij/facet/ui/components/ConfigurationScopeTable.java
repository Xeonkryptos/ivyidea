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

import com.google.common.collect.Streams;
import com.intellij.openapi.roots.DependencyScope;
import com.intellij.ui.ComboBoxTableCellRenderer;
import com.intellij.ui.dualView.TableCellRendererWrapper;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ListWithSelection;
import com.intellij.util.ui.ComboBoxCellEditor;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Table to allow the user to configure the configurations that need to be resolved
 * from within IntelliJ IDEA.
 *
 * @author Guy Mahieu
 */

public class ConfigurationScopeTable extends JBTable {

    private boolean editable = true;

    public ConfigurationScopeTable() {
        super(new ConfigurationScopeTableModel());
        initComponents();
    }

    public void setModel(@NotNull TableModel dataModel) {
        super.setModel(dataModel);
        ((ConfigurationScopeTableModel) dataModel).setEditable(editable);
        initComponents();
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        ((ConfigurationScopeTableModel) dataModel).setEditable(editable);

        initComponents();
        revalidate();
        repaint();
    }

    private void initComponents() {
        setRowSelectionAllowed(false);
        setColumnSelectionAllowed(false);

        getColumnModel().getColumn(0).setPreferredWidth(120);
        getColumnModel().getColumn(1).setPreferredWidth(200);
        getColumnModel().getColumn(2).setPreferredWidth(200);

        getColumnModel().getColumn(0).setHeaderValue("Name");
        getColumnModel().getColumn(1).setHeaderValue("Description");
        getColumnModel().getColumn(2).setHeaderValue("Dependency scope");

        // Register custom renderer to draw deprecated configs in 'strikethrough'
        getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {

            private Font regularFont;
            private Font strikethroughFont;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final ConfigurationScopeTableModel tableModel = (ConfigurationScopeTableModel) table.getModel();
                final Component rendererComponent = super.getTableCellRendererComponent(table,
                        value,
                        isSelected,
                        hasFocus,
                        row,
                        column);
                if (regularFont == null) {
                    regularFont = rendererComponent.getFont();
                }
                final Configuration configuration = tableModel.getConfigurationAt(row);
                if (configuration.getDeprecated() != null) {
                    if (strikethroughFont == null) {
                        final HashMap<TextAttribute, Object> attribs = new HashMap<>();
                        attribs.put(TextAttribute.STRIKETHROUGH, Boolean.TRUE);
                        strikethroughFont = regularFont.deriveFont(attribs);
                    }
                    setToolTipText("Deprecated: " + configuration.getDeprecated());
                    rendererComponent.setFont(strikethroughFont);
                } else {
                    setToolTipText(null);
                    rendererComponent.setFont(regularFont);
                }
                rendererComponent.setEnabled(editable);
                return rendererComponent;
            }
        });

        // Render description disabled if table is disabled
        getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component rendererComponent = super.getTableCellRendererComponent(table,
                        value,
                        isSelected,
                        hasFocus,
                        row,
                        column);
                rendererComponent.setEnabled(editable);
                return rendererComponent;
            }
        });
        final List<String> selectableDependencyScopes = Streams.concat(Stream.of(""), Arrays.stream(DependencyScope.values()).map(DependencyScope::toString)).collect(Collectors.toList());
        getColumnModel().getColumn(2).setCellEditor(new ComboBoxCellEditor() {
            @Override
            protected List<String> getComboBoxItems() {
                return selectableDependencyScopes;
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                return super.getTableCellEditorComponent(table, value != null ? value.toString() : "", isSelected, row, column);
            }
        });
        getColumnModel().getColumn(2).setCellRenderer(new TableCellRendererWrapper() {
            @NotNull
            @Override
            public TableCellRenderer getBaseRenderer() {
                return ComboBoxTableCellRenderer.INSTANCE;
            }

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component rendererComponent = ComboBoxTableCellRenderer.INSTANCE.getTableCellRendererComponent(
                        table,
                        new ListWithSelection<>(selectableDependencyScopes, value),
                        isSelected,
                        hasFocus,
                        row,
                        column);
                rendererComponent.setEnabled(editable);
                return rendererComponent;
            }
        });
    }

    public Map<String, DependencyScope> getDependencyScopes() {
        return ((ConfigurationScopeTableModel) getModel()).getDependencyScopes();
    }
}
