package tech.framework.spring.bean;

import tech.framework.spring.Annotation.Component;
import tech.framework.spring.BeanPostProcessor;

@Component
public class BeanPostProcessorImpl implements BeanPostProcessor {
    @Override
    public Object afterInitializeBean(Object bean, String beanName) {
        System.out.println(beanName + " 初始化完成");
        return bean;
    }
}
