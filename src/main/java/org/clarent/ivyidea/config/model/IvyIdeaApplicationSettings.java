package org.clarent.ivyidea.config.model;

/**
 * @author Xeonkryptos
 * @since 05.09.2020
 */
public class IvyIdeaApplicationSettings {

    private String ivyTemplateContent;
    private GeneralIvyIdeaSettings generalIvyIdeaSettings = new GeneralIvyIdeaSettings();

    public String getIvyTemplateContent() {
        return ivyTemplateContent;
    }

    public void setIvyTemplateContent(String ivyTemplateContent) {
        if (ivyTemplateContent != null && ivyTemplateContent.trim().isEmpty()) {
            ivyTemplateContent = null;
        }
        this.ivyTemplateContent = ivyTemplateContent;
    }

    public GeneralIvyIdeaSettings getGeneralIvyIdeaSettings() {
        return generalIvyIdeaSettings;
    }

    public void setGeneralIvyIdeaSettings(GeneralIvyIdeaSettings generalIvyIdeaSettings) {
        this.generalIvyIdeaSettings = new GeneralIvyIdeaSettings(generalIvyIdeaSettings);
    }

    public void updateWith(GeneralIvyIdeaSettings generalIvyIdeaSettings) {
        this.generalIvyIdeaSettings.updateWith(generalIvyIdeaSettings);
    }
}
