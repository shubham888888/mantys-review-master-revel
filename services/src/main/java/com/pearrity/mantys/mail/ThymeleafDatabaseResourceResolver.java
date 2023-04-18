package com.pearrity.mantys.mail;

import com.pearrity.mantys.domain.ThymeleafTemplate;
import com.pearrity.mantys.repository.ThymeLeafTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;

import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class ThymeleafDatabaseResourceResolver extends StringTemplateResolver {

  private static final String PREFIX = "";

  @Autowired ThymeLeafTemplateRepository thymeLeafTemplateRepository;

  public ThymeleafDatabaseResourceResolver() {
    setResolvablePatterns(Set.of(PREFIX + "*"));
  }

  @Override
  protected ITemplateResource computeTemplateResource(
      IEngineConfiguration configuration,
      String ownerTemplate,
      String template,
      Map<String, Object> templateResolutionAttributes) {

    ThymeleafTemplate thymeleafTemplate = thymeLeafTemplateRepository.findByTemplateName(template);
    if (thymeleafTemplate != null) {
      return super.computeTemplateResource(
          configuration, ownerTemplate, thymeleafTemplate.getHtml(), templateResolutionAttributes);
    }
    return null;
  }
}
