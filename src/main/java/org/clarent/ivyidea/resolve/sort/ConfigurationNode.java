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

import org.apache.ivy.core.module.descriptor.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Xeonkryptos
 * @since 23.08.2020
 */
public class ConfigurationNode {

    private final String configuration;
    private final List<String> neighbors;

    public ConfigurationNode(Configuration configuration) {
        this.configuration = configuration.getName();
        this.neighbors = new ArrayList<>();
        for (String extend : configuration.getExtends()) {
            addNeighbor(extend);
        }
    }

    public String getConfiguration() {
        return configuration;
    }

    public void addNeighbor(String e) {
        this.neighbors.add(e);
    }

    public List<String> getNeighbors() {
        return neighbors;
    }
}
