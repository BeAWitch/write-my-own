package tech.framework.spring.bean;

import tech.framework.spring.Annotation.Autowired;
import tech.framework.spring.Annotation.Component;
import tech.framework.spring.Annotation.PostConstruct;

@Component
public class Dog {

    @Autowired
    private Cat cat;

    @PostConstruct
    public void init() {
        System.out.println("Dog 创建完成，Cat 注入完成：" + cat);
    }

}
