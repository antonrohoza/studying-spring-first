package com.antonr.ioc.context;

import static com.antonr.ioc.context.cast.JavaNumberTypeCast.castPrimitive;

import com.antonr.ioc.exception.BeanInstantiationException;
import com.antonr.ioc.exception.BeanNotFoundException;
import com.antonr.ioc.exception.MultipleBeansForClassException;
import com.antonr.ioc.io.BeanDefinitionReader;
import com.antonr.ioc.entity.Bean;
import com.antonr.ioc.entity.BeanDefinition;
import com.antonr.ioc.io.XMLBeanDefinitionReader;

import com.antonr.ioc.postprocessor.BeanFactoryPostProcessor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.SneakyThrows;

public class ClassPathApplicationContext implements ApplicationContext {
    private static final String SETTER_PREFIX = "set";

    private Map<String, Bean> beans;
    private BeanDefinitionReader beanDefinitionReader;

    public ClassPathApplicationContext() {

    }

    public ClassPathApplicationContext(String... path) {
        setBeanDefinitionReader(new XMLBeanDefinitionReader(path));
        start();
    }

    public void start() {
        beans = new HashMap<>();
        List<BeanDefinition> beanDefinitions = beanDefinitionReader.getBeanDefinitions();
        beanFactoryPostProcess(beanDefinitions);
        instantiateBeans(beanDefinitions);
        injectValueDependencies(beanDefinitions);
        injectRefDependencies(beanDefinitions);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> clazz) {
        List<Object> listOfBeans = beans.values().stream()
                                      .map(Bean::getValue)
                                      .filter(value -> value.getClass() == clazz)
                                      .collect(Collectors.toList());
        if(listOfBeans.size() > 1){
            throw new MultipleBeansForClassException("There is more that one bean declared with name: " + clazz.getName());
        }
        return (T) listOfBeans.get(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name, Class<T> clazz) {
        return (T) beans.entrySet().stream()
                        .filter(beanEntry -> name.equals(beanEntry.getKey()) && clazz == beanEntry.getValue().getValue().getClass())
                        .map(beanEntry -> beanEntry.getValue().getValue())
                        .findFirst()
                        .orElseThrow(() -> new BeanNotFoundException("There is no bean with name: " + name + " and class: " + clazz.getName()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        if(beans.get(name) != null){
            return (T) beans.get(name).getValue();
        }else {
            throw new BeanNotFoundException("There is no bean with name: " + name);
        }
    }

    @Override
    public void setBeanDefinitionReader(BeanDefinitionReader beanDefinitionReader) {
        this.beanDefinitionReader = beanDefinitionReader;
    }

    private void beanFactoryPostProcess(List<BeanDefinition> beanDefinitions){
        List<BeanDefinition> postProcessors = beanDefinitions.stream()
                                                             .filter(this::isPostProcessor)
                                                             .collect(Collectors.toList());
        beanDefinitions.stream()
                       .filter(beanDefinition -> !isPostProcessor(beanDefinition))
                       .forEach(beanDefinition -> postProcessors.forEach(postProcessor -> invokePostProcessor(beanDefinition, postProcessor)));

    }

    private boolean isPostProcessor(BeanDefinition beanDefinition) {
        return getInstanceByBeanClassName(beanDefinition.getBeanClassName()) instanceof BeanFactoryPostProcessor;
    }

    private void invokePostProcessor(BeanDefinition beanDefinition, BeanDefinition postProcessor) {
        String methodName = "postProcessBeanFactory";
        try {
            Object currentObject = getInstanceByBeanClassName(postProcessor.getBeanClassName());
            Method method = currentObject.getClass().getMethod(methodName, beanDefinition.getClass());
            method.invoke(currentObject, beanDefinition);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new BeanInstantiationException("Cannot instantiate bean", e);
        }
    }

    private void instantiateBeans(List<BeanDefinition> beanDefinitions) {
        beans = beanDefinitions.stream()
                               .map(this::getBeanInstance)
                               .collect(Collectors.toMap(Bean::getId, Function.identity()));
    }

    private Bean getBeanInstance(BeanDefinition bd) {
        return new Bean(bd.getId(), getInstanceByBeanClassName(bd.getBeanClassName()));
    }

    @SneakyThrows
    private Object getInstanceByBeanClassName(String beanClassName){
        return Class.forName(beanClassName).getConstructor().newInstance();
    }

    private void injectValueDependencies(List<BeanDefinition> beanDefinitions) {
        injectElementDependencies(beanDefinitions,
                                  BeanDefinition::getDependencies,
                                  this::setAllPropertiesForCurrentBean);
    }

    private void injectRefDependencies(List<BeanDefinition> beanDefinitions) {
        injectElementDependencies(beanDefinitions,
                                  BeanDefinition::getRefDependencies,
                                  this::setAllRefsForCurrentBean);
    }

    private void injectElementDependencies(List<BeanDefinition> beanDefinitions,
                                           Function<BeanDefinition, Map<String, String>> getElementsFunction,
                                           BiConsumer<Entry<String,Bean>, Map<String,String>> setElementsForCurrentBeanConsumer) {
        List<String> filteredBeans = beanDefinitions.stream()
                                                    .filter(beanDefinition -> getElementsFunction.apply(beanDefinition) != null)
                                                    .map(BeanDefinition::getId)
                                                    .collect(Collectors.toList());

        beans.entrySet().stream()
             .filter(beanEntry -> filteredBeans.contains(beanEntry.getKey()))
             .forEach(beanEntry -> setElementsForCurrentBeanConsumer.accept(beanEntry, getBeanPropertyById(beanDefinitions, beanEntry.getKey(), getElementsFunction)));

    }

    private void setAllRefsForCurrentBean(Map.Entry<String, Bean> beanEntry, Map<String, String> beanProperties) {
        Object currentObject = beanEntry.getValue().getValue();
        Arrays.stream(currentObject.getClass().getDeclaredFields())
              .filter(field -> beanProperties.containsKey(field.getName()))
              .forEach(field -> injectRefs(currentObject, field));
    }

    private void injectRefs(Object object, Field field){
        String methodName = getSetterName(field.getName());
        try {
            Method m = object.getClass().getMethod(methodName, field.getType());
            // should be only one element in current list
            List<Object> beanList = beans.entrySet().stream()
                                         .filter(beanEntry -> field.getName().equals(beanEntry.getKey()))
                                         .map(beanEntry -> beanEntry.getValue().getValue())
                                         .collect(Collectors.toList());
            if (beanList.size() > 1) {
                throw new MultipleBeansForClassException("There is more that one bean declared with name: " + field.getName());
            }
            m.invoke(object, beanList.get(0));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new BeanInstantiationException("Cannot instantiate bean", e);
        }
    }

    private void setAllPropertiesForCurrentBean(Map.Entry<String, Bean> beanEntry, Map<String, String> beanProperties) {
        Object currentObject = beanEntry.getValue().getValue();
        Arrays.stream(currentObject.getClass().getDeclaredFields())
              .filter(field -> beanProperties.containsKey(field.getName()))
              .forEach(field -> injectField(currentObject, field, beanProperties.get(field.getName())));
    }

    private void injectField(Object object, Field field, String fieldValue) {
        String methodName = getSetterName(field.getName());
        try {
            Method method = object.getClass().getMethod(methodName, field.getType());
            method.invoke(object, field.getType() == String.class ? fieldValue
                                                                  : castPrimitive(fieldValue, field.getType()));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new BeanInstantiationException("Cannot instantiate bean", e);
        }
    }

    private String getSetterName(String propertyName) {
        return SETTER_PREFIX + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }

    private Map<String, String> getBeanPropertyById(List<BeanDefinition> beanDefinitions, String id, Function<BeanDefinition, Map<String, String>> function){
        return beanDefinitions.stream()
                              .filter(bd -> id.equals(bd.getId()))
                              .map(function)
                              .findFirst()
                              .orElseThrow(RuntimeException::new);
    }

}
