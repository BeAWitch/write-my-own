package tech.framework.spring;

import tech.framework.spring.Annotation.Autowired;
import tech.framework.spring.Annotation.Component;
import tech.framework.spring.Annotation.PostConstruct;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

public class BeanDefinition {

    private final String name;
    private final Constructor<?> constructor;
    private final Method postConstructMethod;
    private final List<Field> autowiredFields;
    private final Class<?> beanType;


    public BeanDefinition(Class<?> type) {
        this.beanType = type;
        Component component = type.getDeclaredAnnotation(Component.class);
        this.name = component.name().isEmpty() ? type.getSimpleName() : component.name();
        try {
            this.constructor = type.getConstructor();
            this.postConstructMethod = Arrays.stream(type.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(PostConstruct.class))
                    .findFirst()
                    .orElse(null);
            this.autowiredFields = Arrays.stream(type.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Autowired.class))
                    .toList();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public String getName() {
        return name;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public Method getPostConstructMethod() {
        return postConstructMethod;
    }

    public List<Field> getAutowiredFields() {
        return autowiredFields;
    }

    public Class<?> getBeanType() {
        return beanType;
    }
}
