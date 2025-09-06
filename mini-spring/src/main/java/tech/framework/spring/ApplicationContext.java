package tech.framework.spring;

import tech.framework.spring.Annotation.Autowired;
import tech.framework.spring.Annotation.Component;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplicationContext {

    public ApplicationContext(String packageName) throws IOException, URISyntaxException {
        initContext(packageName);
    }

    private Map<String, Object> ioc = new HashMap<>();
    private Map<String, Object> loadinIoc = new HashMap<>();
    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<>();

    public Object getBean(String name) {
        if (name == null) {
            return null;
        }
        Object bean = this.ioc.get(name);
        if (bean != null) {
            return bean;
        }
        if (beanDefinitionMap.containsKey(name)) {
            return createBean(beanDefinitionMap.get(name));
        }
        return null;
    }

    public <T> T getBean(Class<T> beanType) {
        String beanName = this.beanDefinitionMap.values().stream()
                .filter(beanDefinition -> beanType.isAssignableFrom(beanDefinition.getBeanType()))
                .map(BeanDefinition::getName)
                .findFirst()
                .orElse(null);
        return (T) getBean(beanName);
    }

    public <T> List<T> getBeans(Class<T> beanType) {
        return this.beanDefinitionMap.values().stream()
                .filter(beanDefinition -> beanType.isAssignableFrom(beanDefinition.getBeanType()))
                .map(BeanDefinition::getName)
                .map(this::getBean)
                .map(bean -> (T) bean)
                .toList();
    }

    public void initContext(String packageName) throws IOException, URISyntaxException {
        scanPackage(packageName).stream().filter(this::canCreate).forEach(this::wrapper);
        initBeanPostProcessor();
        beanDefinitionMap.values().forEach(this::createBean);
    }

    private void initBeanPostProcessor() {
        this.beanDefinitionMap.values().stream()
                .filter(beanDefinition -> BeanPostProcessor.class.isAssignableFrom(beanDefinition.getBeanType()))
                .map(this::createBean)
                .map(bean -> (BeanPostProcessor) bean)
                .forEach(beanPostProcessors::add);
    }

    private List<Class<?>> scanPackage(String packageName) throws IOException, URISyntaxException {
        List<Class<?>> classList = new ArrayList<>();
        URL resource = this.getClass().getClassLoader().getResource(packageName.replace(".", File.separator));
        Path path = Paths.get(resource.toURI());
        // 遍历文件夹，获取类
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path absolutePath = file.toAbsolutePath();
                if (absolutePath.toString().endsWith(".class")) {
                    String replaceStr = absolutePath.toString().replace(File.separator, ".");
                    int indexOfPackage = replaceStr.indexOf(packageName);
                    String className = replaceStr.substring(indexOfPackage, replaceStr.length() - ".class".length()); // 全类名
                    try {
                        classList.add(Class.forName(className));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return classList;
    }

    protected boolean canCreate(Class<?> type) {
        return type.isAnnotationPresent(Component.class);
    }

    private void autowiredBean(Object bean, BeanDefinition beanDefinition) throws IllegalAccessException {
        for (Field autowiredField : beanDefinition.getAutowiredFields()) {
            autowiredField.setAccessible(true);
            Object getBean = getBean(autowiredField.getType());
            Autowired declaredAnnotation = autowiredField.getDeclaredAnnotation(Autowired.class);
            if (getBean == null && declaredAnnotation.required()) {
                throw new RuntimeException("缺少需要的 bean：" + beanDefinition.getBeanType());
            }
            autowiredField.set(bean, getBean);
        }
    }

    protected BeanDefinition wrapper(Class<?> type) {
        BeanDefinition beanDefinition = new BeanDefinition(type);
        if (beanDefinitionMap.containsKey(beanDefinition.getName())) {
            throw new RuntimeException("bean 名字重复");
        }
        beanDefinitionMap.put(beanDefinition.getName(), beanDefinition);
        return beanDefinition;
    }

    protected Object createBean(BeanDefinition beanDefinition) {
        String name = beanDefinition.getName();
        if (ioc.containsKey(name)) {
            return ioc.get(name);
        }
        if (loadinIoc.containsKey(name)) {
            return loadinIoc.get(name);
        }
        return doCreateBean(beanDefinition);
    }

    private Object doCreateBean(BeanDefinition beanDefinition) {
        Constructor<?> constructor = beanDefinition.getConstructor();
        Object bean;
        try {
            bean = constructor.newInstance();
            loadinIoc.put(beanDefinition.getName(), bean);
            autowiredBean(bean, beanDefinition);
            bean = initializeBean(bean, beanDefinition);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        loadinIoc.remove(beanDefinition.getName());
        ioc.put(beanDefinition.getName(), bean);
        return bean;
    }

    private Object initializeBean(Object bean, BeanDefinition beanDefinition) throws InvocationTargetException, IllegalAccessException {
        for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            bean = beanPostProcessor.beforeInitializeBean(bean, beanDefinition.getName());
        }

        Method postContructMethod = beanDefinition.getPostConstructMethod();
        if (postContructMethod != null) {
            postContructMethod.invoke(bean);
        }

        for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
            bean = beanPostProcessor.afterInitializeBean(bean, beanDefinition.getName());
        }

        return bean;
    }


}
