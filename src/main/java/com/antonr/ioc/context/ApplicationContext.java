package com.antonr.ioc.context;


import com.antonr.ioc.io.BeanDefinitionReader;

public interface ApplicationContext {

    <T> T getBean(Class<T> clazz);

    <T> T getBean(String name, Class<T> clazz);

    <T> T getBean(String name);

    void setBeanDefinitionReader(BeanDefinitionReader beanDefinitionReader);
}
