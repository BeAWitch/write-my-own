package tech.mybatis;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MysqlSessionFactory {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/fordemo";
    private static final String DB_USER = "root";
    private static final String PASSWORD = "root";

    @SuppressWarnings("all")
    public <T> T getMapper(Class<?> mapperClass) {
        return (T) Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{mapperClass},
                new MapperInvocationHandler()
        );
    }

    static class MapperInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().startsWith("select")) {
                return invokeSelect(proxy, method, args);
            }
            return null;
        }

        private Object invokeSelect(Object proxy, Method method, Object[] args) {
            String sql = createSelectSql(method);
            try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_USER, PASSWORD)) {
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Integer) {
                        preparedStatement.setInt(i + 1, (int) args[i]);
                    } else if (args[i] instanceof String) {
                        preparedStatement.setString(i + 1, (String) args[i]);
                    }
                }
                ResultSet resultSet = preparedStatement.executeQuery();
                if (List.class.isAssignableFrom(method.getReturnType())) {
                    List<Object> res = new ArrayList<>();
                    while (resultSet.next()) {
                        ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
                        res.add(parseResult(resultSet, (Class<?>) genericReturnType.getActualTypeArguments()[0]));
                    }
                    return res;
                }
                if (resultSet.next()) {
                    return parseResult(resultSet, method.getReturnType());
                }
            } catch (Exception e) {
            }
            return null;
        }

        private Object parseResult(ResultSet resultSet, Class<?> returnType) throws Exception {
            Constructor<?> constructor = returnType.getConstructor();
            Object result = constructor.newInstance();
            Field[] declaredFields = result.getClass().getDeclaredFields();
            for (Field declaredField : declaredFields) {
                String name = declaredField.getName();
                Object column = null;
                if (declaredField.getType() == String.class) {
                    column = resultSet.getString(name);
                } else if (declaredField.getType() == Integer.class) {
                    column = resultSet.getInt(name);
                }
                declaredField.setAccessible(true);
                declaredField.set(result, column);
            }
            return result;
        }

        private String createSelectSql(Method method) {
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("select ");
            List<String> selectCols = getSelectCols(method);
            sqlBuilder.append(String.join(",", selectCols));
            sqlBuilder.append(" from ");
            String tableName = getSelectTableName(method);
            sqlBuilder.append(tableName);
            sqlBuilder.append(" where ");
            String where = sqlSelectWhere(method);
            sqlBuilder.append(where);
            return sqlBuilder.toString();
        }

        private String sqlSelectWhere(Method method) {
            return Arrays.stream(method.getParameters())
                    .map(parameter -> {
                        Param param = parameter.getAnnotation(Param.class);
                        String column = param.value();
                        return column + " = ?";
                    })
                    .collect(Collectors.joining(" and "));
        }

        private String getSelectTableName(Method method) {
            Class<?> type = getReturnTypeFromMethod(method);
            Table table = type.getAnnotation(Table.class);
            if (table == null) {
                throw new RuntimeException("返回值无法确定查询的表！");
            }
            return table.tableName();
        }

        private List<String> getSelectCols(Method method) {
            Class<?> returnType = getReturnTypeFromMethod(method);
            Field[] declaredField = returnType.getDeclaredFields();
            return Arrays.stream(declaredField).map(Field::getName).toList();
        }

        private Class<?> getReturnTypeFromMethod(Method method) {
            Class<?> type = null;
            if (List.class.isAssignableFrom(method.getReturnType())) {
                ParameterizedType genericReturnType = (ParameterizedType) method.getGenericReturnType();
                type = (Class<?>) genericReturnType.getActualTypeArguments()[0];
            } else {
                type = method.getReturnType();
            }
            return type;
        }

    }

}
