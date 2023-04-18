package com.pearrity.mantys.repository;

import com.pearrity.mantys.domain.ThymeleafTemplate;

public interface ThymeLeafTemplateRepository {

  ThymeleafTemplate findByTemplateName(String name);
}
