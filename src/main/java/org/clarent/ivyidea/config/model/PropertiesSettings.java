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

package org.clarent.ivyidea.config.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Guy Mahieu
 */
public class PropertiesSettings {
    
    private List<String> propertyFiles = new ArrayList<>();

    public PropertiesSettings() {}

    public PropertiesSettings(PropertiesSettings copy) {
        this.propertyFiles.addAll(copy.propertyFiles);
    }

    public List<String> getPropertyFiles() {
        return propertyFiles;
    }

    public void setPropertyFiles(List<String> propertyFiles) {
        this.propertyFiles = new ArrayList<>(propertyFiles);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PropertiesSettings)) {
            return false;
        }
        PropertiesSettings that = (PropertiesSettings) o;
        return Objects.equals(propertyFiles, that.propertyFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyFiles);
    }
}