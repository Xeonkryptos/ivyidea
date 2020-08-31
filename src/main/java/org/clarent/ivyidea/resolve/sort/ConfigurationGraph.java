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

package org.clarent.ivyidea.resolve.sort;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Xeonkryptos
 * @since 23.08.2020
 */
public class ConfigurationGraph {

    private final List<ConfigurationNode> nodes;

    public ConfigurationGraph() {
        this.nodes = new ArrayList<>();
    }

    public ConfigurationGraph(List<ConfigurationNode> nodes) {
        this.nodes = nodes;
    }

    public void addNode(ConfigurationNode e) {
        this.nodes.add(e);
    }

    public List<ConfigurationNode> getNodes() {
        return nodes;
    }

    public ConfigurationNode getNode(String searchConfiguration) {
        for (ConfigurationNode node : this.getNodes()) {
            if (node.getConfiguration().equals(searchConfiguration)) {
                return node;
            }
        }
        return null;
    }

    public int getSize() {
        return this.nodes.size();
    }
}
