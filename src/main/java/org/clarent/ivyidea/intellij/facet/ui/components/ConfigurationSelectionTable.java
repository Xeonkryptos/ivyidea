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

import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.table.JBTable;
import org.apache.ivy.core.module.descriptor.Configuration;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Set;

/**
 * Table to allow the user to configure the configurations that need to be resolved
 * from within IntelliJ IDEA.
 *
 * @author Guy Mahieu
 */

public class ConfigurationSelectionTable extends JBTable {

    private boolean editable = true;

    public ConfigurationSelectionTable() {
        super(new ConfigurationSelectionTableModel());
        initComponents();
    }

    public void setModel(@NotNull TableModel dataModel) {
        super.setModel(dataModel);
        ((ConfigurationSelectionTableModel) dataModel).setEditable(editable);
        initComponents();
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        ((ConfigurationSelectionTableModel) dataModel).setEditable(editable);

        initComponents();
        revalidate();
        repaint();
    }

    public Set<Configuration> getSelectedConfigurations() {
        return ((ConfigurationSelectionTableModel) getModel()).getSelectedConfigurations();
    }

    private void initComponents() {
        setRowSelectionAllowed(false);
        setColumnSelectionAllowed(false);

        getColumnModel().getColumn(0).setPreferredWidth(30);
        getColumnModel().getColumn(0).setMaxWidth(30);
        getColumnModel().getColumn(1).setPreferredWidth(120);
        getColumnModel().getColumn(2).setPreferredWidth(400);

        getColumnModel().getColumn(0).setHeaderValue("");
        getColumnModel().getColumn(1).setHeaderValue("Name");
        getColumnModel().getColumn(2).setHeaderValue("Description");

        // Render checkbox disabled if table is disabled
        getColumnModel().getColumn(0).setCellRenderer(new BooleanTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                rendererComponent.setEnabled(editable);
                return rendererComponent;
            }
        });
        getColumnModel().getColumn(0).setCellEditor(new BooleanTableCellEditor());

        // Register custom renderer to draw deprecated configs in 'strikethrough'
        getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {

            private Font regularFont;
            private Font strikethroughFont;

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final ConfigurationSelectionTableModel tableModel = (ConfigurationSelectionTableModel) table.getModel();
                final Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
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
                    setToolTipText("Depracated: " + configuration.getDeprecated());
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
        getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                rendererComponent.setEnabled(editable);
                return rendererComponent;
            }
        });
    }
}
