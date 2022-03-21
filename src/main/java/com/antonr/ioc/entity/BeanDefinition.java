package com.antonr.ioc.entity;

import java.util.Map;
import lombok.Data;

@Data
public class BeanDefinition {

  private String id;
  private String beanClassName;
  private Map<String, String> dependencies;
  private Map<String, String> refDependencies;

}
