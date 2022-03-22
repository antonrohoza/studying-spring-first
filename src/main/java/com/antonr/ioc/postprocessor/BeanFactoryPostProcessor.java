package com.antonr.ioc.postprocessor;

import com.antonr.ioc.entity.BeanDefinition;

public interface BeanFactoryPostProcessor {

  void postProcessBeanFactory(BeanDefinition beanDefinition);

}
