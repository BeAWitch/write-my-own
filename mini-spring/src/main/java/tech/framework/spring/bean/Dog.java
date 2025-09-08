package tech.framework.spring.bean;

import tech.framework.spring.annotation.Autowired;
import tech.framework.spring.annotation.Component;
import tech.framework.spring.annotation.PostConstruct;

@Component
public class Dog {

    @Autowired
    private Cat cat;

    @PostConstruct
    public void init() {
        System.out.println("Dog 创建完成，Cat 注入完成：" + cat);
    }

}
