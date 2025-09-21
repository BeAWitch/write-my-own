package tech.framework.spring.web;

import tech.framework.spring.annotation.Component;
import tech.framework.spring.annotation.Controller;
import tech.framework.spring.annotation.Param;
import tech.framework.spring.annotation.RequestMapping;
import tech.framework.spring.annotation.ResponseBody;
import tech.framework.spring.bean.User;

@Controller
@Component
@RequestMapping("/hello")
public class HelloController {

    @RequestMapping("/a")
    public String hello(@Param("name") String name, @Param("age") Integer age) {
        return String.format("<h1>hello %s %s</h1><br>", name, age);
    }

    @RequestMapping("/json")
    @ResponseBody
    public User json(@Param("name") String name, @Param("age") Integer age) {
        return new User(name, age);
    }

    @RequestMapping("/html")
    public ModelAndView html(@Param("name") String name) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setView("index.html");
        modelAndView.getContext().put("name", name);
        return modelAndView;
    }

}
