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

package org.clarent.ivyidea.intellij.framework.ivy;

import com.intellij.framework.FrameworkTypeEx;
import com.intellij.framework.addSupport.FrameworkSupportInModuleProvider;
import org.clarent.ivyidea.intellij.ui.IvyIdeaIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Xeonkryptos
 * @since 26.08.2020
 */
public class IvyFrameworkType extends FrameworkTypeEx {

    public IvyFrameworkType() {
        super("IvyIDEA");
    }

    @NotNull
    @Override
    public FrameworkSupportInModuleProvider createProvider() {
        return new IvyFrameworkSupportProvider();
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @NotNull
    @Override
    public String getPresentableName() {
        return "IvyIDEA";
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return IvyIdeaIcons.MAIN_ICON_MEDIUM;
    }
}
