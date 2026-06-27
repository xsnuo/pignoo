package com.xuesinuo.pignoo.test.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.annotation.Column;
import com.xuesinuo.pignoo.core.annotation.Link;
import com.xuesinuo.pignoo.core.annotation.Table;
import com.xuesinuo.pignoo.core.config.AnnotationMode;
import com.xuesinuo.pignoo.core.config.DatabaseEngine;
import com.xuesinuo.pignoo.core.config.NamingMode;
import com.xuesinuo.pignoo.core.implement.BasePignoo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class Test02_Config {

    public final DataSource dataSource;

    public Test02_Config(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static String RandomCode(int length) {
        return UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, length);
    }

    private String camel2Underline(String str) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (char c : str.toCharArray()) {
            if (i++ == 0) {
                sb.append(Character.toLowerCase(c));
            } else {
                if (Character.isUpperCase(c)) {
                    sb.append("_").append(Character.toLowerCase(c));
                } else {
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD })
    public static @interface Id {}

    @Data
    public static class Test02ForMixCamel { // 原始表对象
        @Id
        private Long test02ForMixCamelId;
        @Column("name_for_mix_camel")
        private String name;
    }

    @Table("test02_for_mix_camel")
    @Data
    public static class Test02ForMixCamel2 { // 使用其他对象映射
        @Column("test02_for_mix_camel_id")
        private Long id;
        private String nameForMixCamel;
        @Column(ignore = true)
        private String code;
    }

    @Link(Test02ForMixCamel.class)
    @Data
    public static class Test02ForMixCamel3 { // 使用Link注解映射
        private Long test02ForMixCamelId;
        private String nameForMixCamel;
        @Column("some_thing") // 在被链接的实体上没有的列，会被忽略
        private String something;
    }

    @Test
    public void withoutProxy() {
        PignooConfig config = new PignooConfig();
        config.setOpenSetterProxy(false);
        config.setPrimaryKeyNamingConvention((tableName, className, c, fields) -> tableName + "_id");
        try (var pignoo = new BasePignoo(dataSource, config)) {
            Test02ForMixCamel newTest02ForMixCamel = new Test02ForMixCamel();
            newTest02ForMixCamel.setName(RandomCode(4));
            Test02ForMixCamel noProxy = pignoo.writer(Test02ForMixCamel.class).add(newTest02ForMixCamel);
            noProxy.setName(RandomCode(5));
            Test02ForMixCamel read = pignoo.reader(Test02ForMixCamel.class)
                    .filter(Test02ForMixCamel::getTest02ForMixCamelId, "==", noProxy.getTest02ForMixCamelId())
                    .getFirst();
            if (read.getName().equals(noProxy.getName())) {
                throw new RuntimeException("Without proxy error");
            }
        }
    }

    @Test
    public void configMixCamel() {
        PignooConfig config = new PignooConfig();
        config.setEngine(DatabaseEngine.MySQL);
        config.setAnnotationMode(AnnotationMode.MIX);
        config.setNamingMode(NamingMode.CAMEL_TO_SNAKE);
        config.setAutoPrimaryKey(true);
        config.setPrimaryKeyNamingConvention((tableName, className, c, fields) -> {
            for (Field field : fields) {
                if (field.getAnnotation(Id.class) != null) {
                    return this.camel2Underline(field.getName());
                }
            }
            return tableName + "_id";
        });
        try (var pignoo = new BasePignoo(dataSource, config)) {
            Test02ForMixCamel newObject = new Test02ForMixCamel();
            String name = RandomCode(10);
            newObject.setName(name);
            pignoo.writer(Test02ForMixCamel2.class).removeAll();
            pignoo.writer(Test02ForMixCamel.class).add(newObject);
            if (!pignoo.reader(Test02ForMixCamel2.class).getFirst().getNameForMixCamel().equals(name)) {
                throw new RuntimeException("Config mix camel error 1");
            }
            if (!pignoo.reader(Test02ForMixCamel3.class).getFirst().getNameForMixCamel().equals(name)) {
                throw new RuntimeException("Config mix camel error 2");
            }
            Test02ForMixCamel3 newObject2 = new Test02ForMixCamel3();
            pignoo.writer(Test02ForMixCamel3.class).add(newObject2); // @Link可以被用做数据操作，前提是@Link实体包含了算不必填项，否则会出现缺少必填项错误
        }
    }

    @Table
    @Data
    public static class Test02ForMastSame {
        @Column("id")
        private Long id;
        @Column
        private String nameForMastSame;
        private String code; // Mast模式下：没标记的列会被忽略
    }

    @Table("Test02ForMastSame")
    @Data
    public static class Test02ForMastSame2 {
        @Column(primaryKey = Column.PrimaryKey.AUTO, value = "id") // primaryKey设置错误不影响读取
        private Long tid;
        @Column("nameForMastSame")
        private String name;
    }

    @Test
    public void configMastSame() {
        PignooConfig config = new PignooConfig();
        config.setEngine(DatabaseEngine.MySQL);
        config.setAnnotationMode(AnnotationMode.MUST);
        config.setNamingMode(NamingMode.SAME);
        config.setAutoPrimaryKey(false);
        boolean catched = false;
        try (var pignoo = new BasePignoo(dataSource, config)) {
            Test02ForMastSame newObject = new Test02ForMastSame();
            pignoo.writer(Test02ForMastSame.class).add(newObject);
        } catch (Exception e) {
            String message = e.getMessage().toLowerCase();
            if (message.indexOf("primary") < 0 || message.indexOf("null") < 0) { // 没有主键，错误提示中应包含"primary"和"null"
                throw e;
            } else {
                catched = true;
            }
        }
        if (catched == false) {
            throw new RuntimeException("Config mast same error 1");
        }
        try (var pignoo = new BasePignoo(dataSource, config)) {
            Test02ForMastSame newObject = new Test02ForMastSame();
            String name = RandomCode(10);
            newObject.setId(1L);
            newObject.setNameForMastSame(name);
            newObject.setCode(RandomCode(4));
            pignoo.writer(Test02ForMastSame2.class).removeAll();
            Test02ForMastSame writerObject = pignoo.writer(Test02ForMastSame.class).add(newObject);
            Test02ForMastSame2 readObject = pignoo.reader(Test02ForMastSame2.class).getFirst();
            if (!readObject.getName().equals(name) || !readObject.getTid().equals(writerObject.getId())) {
                throw new RuntimeException("Config mast same error 2");
            }
        }
    }
}
