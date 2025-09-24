package tech.mybatis;


import java.util.List;

public interface UserMapper {

    User selectById(@Param("id") int id);

    User selectByName(@Param("name") String name);

    List<User> selectListByAge(@Param("age") int age);

}
