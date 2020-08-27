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

package org.clarent.ivyidea.intellij;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.clarent.ivyidea.config.model.IvyIdeaApplicationSettings;
import org.jetbrains.annotations.NotNull;

/**
 * @author Guy Mahieu
 */
@State(
        name = IvyIdeaApplicationService.COMPONENT_NAME,
        storages = {@Storage(StoragePathMacros.NON_ROAMABLE_FILE)}
)
public class IvyIdeaApplicationService implements PersistentStateComponent<IvyIdeaApplicationSettings> {

    public static final String COMPONENT_NAME = "IvyIDEA.ApplicationSettings";

    private final IvyIdeaApplicationSettings internalState;

    public IvyIdeaApplicationService() {
        this.internalState = new IvyIdeaApplicationSettings();
    }

    @NotNull
    public IvyIdeaApplicationSettings getState() {
        return internalState;
    }

    public void loadState(@NotNull IvyIdeaApplicationSettings state) {
        XmlSerializerUtil.copyBean(state, this.getState());
    }
}
