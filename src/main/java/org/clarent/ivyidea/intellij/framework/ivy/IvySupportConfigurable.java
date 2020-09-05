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

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.framework.addSupport.FrameworkSupportInModuleConfigurable;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import org.clarent.ivyidea.config.IvyIdeaConfigHelper;
import org.clarent.ivyidea.intellij.facet.IvyIdeaFacet;
import org.clarent.ivyidea.intellij.facet.IvyIdeaFacetType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Xeonkryptos
 * @since 26.08.2020
 */
public class IvySupportConfigurable extends FrameworkSupportInModuleConfigurable {

    private static final Logger LOGGER = Logger.getLogger(IvySupportConfigurable.class.getName());

    @Nullable
    @Override
    public JComponent createComponent() {
        return null;
    }

    @Override
    public void addSupport(@NotNull Module module, @NotNull ModifiableRootModel rootModel, @NotNull ModifiableModelsProvider modifiableModelsProvider) {
        Path moduleBaseDir = Paths.get(module.getModuleFilePath());
        Path ivyTargetFile = moduleBaseDir.resolveSibling("ivy.xml");

        Map<String, Object> templateAttributes = new HashMap<>();
        templateAttributes.put("MODULE_NAME", module.getName());

        String ivyTemplateContent = IvyIdeaConfigHelper.getIvyTemplateContent(module.getProject());
        if (ivyTemplateContent != null) {
            try (BufferedWriter writer = Files.newBufferedWriter(ivyTargetFile)) {
                String finalizedProjectFileContent = FileTemplateUtil.mergeTemplate(templateAttributes, ivyTemplateContent, true);
                writer.write(finalizedProjectFileContent);

                FacetManager facetManager = FacetManager.getInstance(module);
                ModifiableFacetModel modifiableModel = facetManager.createModifiableModel();
                IvyIdeaFacet ivyIdeaFacet = facetManager.createFacet(IvyIdeaFacetType.getInstance(), IvyIdeaFacetType.STRING_ID, null);
                ivyIdeaFacet.getConfiguration().setIvyFile(ivyTargetFile.toAbsolutePath().toString());
                modifiableModel.addFacet(ivyIdeaFacet);
                modifiableModel.commit();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Writing of templated ivy.xml failed", e);
            }
        } else {
            LOGGER.info("Skipped creation of ivy.xml. No template is defined yet.");
        }
    }
}
