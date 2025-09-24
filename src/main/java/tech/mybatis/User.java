package tech.mybatis;


import lombok.Data;
import lombok.ToString;

@Table(tableName = "user")
@Data
@ToString
public class User {

    private Integer id;
    private String name;
    private Integer age;

}
