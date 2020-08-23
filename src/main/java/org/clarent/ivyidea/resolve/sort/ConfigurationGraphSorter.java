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

import java.util.*;

/**
 * @author Xeonkryptos
 * @since 23.08.2020
 */
public class ConfigurationGraphSorter {

    public static List<String> topoSort(ConfigurationGraph g) {
        // Fetching the number of nodes in the graph
        int V = g.getSize();

        // List where we'll be storing the topological order
        List<String> order = new ArrayList<>();

        // Map which indicates if a node is visited (has been processed by the algorithm)
        Map<String, Boolean> visited = new HashMap<>();
        for (ConfigurationNode tmp : g.getNodes())
            visited.put(tmp.getConfiguration(), false);

        // We go through the nodes using black magic
        for (ConfigurationNode tmp : g.getNodes()) {
            if (!visited.get(tmp.getConfiguration())) blackMagic(g, tmp.getConfiguration(), visited, order);
        }
        return order;
    }

    private static void blackMagic(ConfigurationGraph g, String v, Map<String, Boolean> visited, List<String> order) {
        // Mark the current node as visited
        visited.replace(v, true);

        // We reuse the algorithm on all adjacent nodes to the current node
        for (String neighborId : g.getNode(v).getNeighbors()) {
            if (!visited.get(neighborId)) blackMagic(g, neighborId, visited, order);
        }

        // Put the current node in the array
        order.add(v);
    }
}
