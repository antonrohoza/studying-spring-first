package com.antonr.ioc.io;


import com.antonr.ioc.entity.BeanDefinition;
import java.util.List;

public interface BeanDefinitionReader {

  List<BeanDefinition> getBeanDefinitions();

}
