package com.antonr.ioc.service;

import com.antonr.ioc.postprocessor.BeanFactoryPostProcessor;
import com.antonr.ioc.entity.BeanDefinition;
import java.util.Map;

public class PasswordBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

  @Override
  public void postProcessBeanFactory(BeanDefinition beanDefinition) {
    Map<String, String> dependencies = beanDefinition.getDependencies();
    if(dependencies == null){
      return;
    }
    String password = dependencies.get("password");
    if(password == null){
      return;
    }
    dependencies.put("password", "qwerty");
  }
}
