package tech.mybatis;

public class Main {

    public static void main(String[] args) {
        MysqlSessionFactory mysqlSessionFactory = new MysqlSessionFactory();
        UserMapper mapper = mysqlSessionFactory.getMapper(UserMapper.class);
        System.out.println(mapper.selectById(1));
        System.out.println(mapper.selectByName("jack"));
        System.out.println(mapper.selectListByAge(21));
    }

}
