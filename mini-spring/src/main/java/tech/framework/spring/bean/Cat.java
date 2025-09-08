package tech.framework.spring.bean;

import tech.framework.spring.annotation.Autowired;
import tech.framework.spring.annotation.Component;
import tech.framework.spring.annotation.PostConstruct;

@Component
public class Cat {

    @Autowired
    private Dog dog;

    @PostConstruct
    public void init() {
        System.out.println("Cat 创建完成，Dog 注入完成：" + dog);
    }

}
