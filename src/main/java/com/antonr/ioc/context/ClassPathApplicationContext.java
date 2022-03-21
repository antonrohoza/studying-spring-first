package com.antonr.ioc.context;

import static com.antonr.ioc.context.cast.JavaNumberTypeCast.castPrimitive;

import com.antonr.ioc.exception.BeanInstantiationException;
import com.antonr.ioc.exception.MultipleBeansForClassException;
import com.antonr.ioc.io.BeanDefinitionReader;
import com.antonr.ioc.entity.Bean;
import com.antonr.ioc.entity.BeanDefinition;
import com.antonr.ioc.io.XMLBeanDefinitionReader;

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
import javafx.util.Pair;
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
                        .orElseThrow(() -> new BeanInstantiationException("Wrong instantiation of bean"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        return (T) beans.entrySet().stream()
                        .filter(beanEntry -> name.equals(beanEntry.getKey()))
                        .map(beanEntry -> beanEntry.getValue().getValue())
                        .findFirst()
                        .orElseThrow(() -> new BeanInstantiationException("Wrong instantiation of bean"));
    }

    @Override
    public void setBeanDefinitionReader(BeanDefinitionReader beanDefinitionReader) {
        this.beanDefinitionReader = beanDefinitionReader;
    }

    private void instantiateBeans(List<BeanDefinition> beanDefinitions) {
        beans = beanDefinitions.stream()
                               .map(this::getBeanInstance)
                               .collect(Collectors.toMap(Bean::getId, Function.identity()));
    }

    @SneakyThrows
    private Bean getBeanInstance(BeanDefinition bd) {
        return new Bean(bd.getId(), Class.forName(bd.getBeanClassName()).getConstructor().newInstance());
    }

    private void injectValueDependencies(List<BeanDefinition> beanDefinitions) {
        injectElementDependencies(beanDefinitions, BeanDefinition::getDependencies, this::setAllPropertiesForCurrentBean);
    }

    private void injectRefDependencies(List<BeanDefinition> beanDefinitions) {
        injectElementDependencies(beanDefinitions, BeanDefinition::getRefDependencies, this::setAllRefsForCurrentBean);
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
             .forEach(beanEntry -> setElementsForCurrentBeanConsumer.accept(beanEntry, getElementById(beanDefinitions, beanEntry.getKey(), getElementsFunction)));

    }

    private void setAllRefsForCurrentBean(Map.Entry<String, Bean> beanEntry, Map<String, String> beanProperties) {
        Arrays.stream(beanEntry.getValue().getValue().getClass().getDeclaredFields())
              .filter(field -> beanProperties.containsKey(field.getName()))
              .forEach(field -> injectRefs(beanEntry.getValue().getValue(), field));
    }

    @SneakyThrows
    private void injectRefs(Object object, Field field){
        Class<?> aClass = object.getClass();
        String methodName = getSetterName(field.getName());
        Method m = aClass.getMethod(methodName, field.getType());
        // should be only one element in current list
        List<Object> beanList = beans.entrySet().stream()
                                    .filter(beanEntry -> field.getName().equals(beanEntry.getKey()))
                                    .map(beanEntry -> beanEntry.getValue().getValue())
                                    .collect(Collectors.toList());
        if(beanList.size() > 1){
            throw new MultipleBeansForClassException("There is more that one bean declared with name: " + field.getName());
        }
        m.invoke(object, beanList.get(0));
    }

    private void setAllPropertiesForCurrentBean(Map.Entry<String, Bean> beanEntry, Map<String, String> beanProperties) {
        Arrays.stream(beanEntry.getValue().getValue().getClass().getDeclaredFields())
              .filter(field -> beanProperties.containsKey(field.getName()))
              .forEach(field -> injectField(beanEntry.getValue().getValue(), field, beanProperties.get(field.getName())));
    }

    @SneakyThrows
    private void injectField(Object object, Field field, String fieldValue){
        String methodName = getSetterName(field.getName());
        Method m = object.getClass().getMethod(methodName, field.getType());
        m.invoke(object, field.getType() == String.class ? fieldValue
                                                         : castPrimitive(fieldValue, field.getType()));
    }

    private String getSetterName(String propertyName) {
        return SETTER_PREFIX + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
    }

    private Map<String, String> getElementById(List<BeanDefinition> beanDefinitions, String id, Function<BeanDefinition, Map<String, String>> function){
        return beanDefinitions.stream()
                              .filter(bd -> id.equals(bd.getId()))
                              .map(function)
                              .findFirst()
                              .orElseThrow(RuntimeException::new);
    }

}
