package com.xuesinuo.pignoo.test.autodatabase;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.autodatabase.EntityScanConfig;
import com.xuesinuo.pignoo.autodatabase.EntityScaner;
import com.xuesinuo.pignoo.core.PignooConfig;

@SpringBootTest
public class Test03_Mapper {
    public final DataSource dataSource;

    public Test03_Mapper(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // 放在这里防止被扫描到
    public static enum Enums {
        A, B, C
    }

    @Test
    public void mapper() throws SQLException {
        try (var con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            con.createStatement().execute("drop table if exists autodatabase_test03");
            con.commit();
        }
        PignooConfig pignooConfig = new PignooConfig();
        EntityScanConfig entityScanConfig = new EntityScanConfig();
        entityScanConfig.setBuildMode(EntityScanConfig.BuildMode.RADICALLY);
        entityScanConfig.setStrictColumnType(true);
        entityScanConfig.setAnnotationClassOnly(false);
        entityScanConfig.setTypeMapper((javaType, scale, field) -> {
            if (String.class.isAssignableFrom(javaType)) {
                if (field.getName().toLowerCase().endsWith("number") && scale == 0) {
                    return "char(20)";
                }
                if (scale > 0 && scale <= 255) {
                    return "varchar(" + scale + ")";
                } else if (scale <= 0) {
                    return "varchar(255)";
                } else {
                    return "longtext";
                }
            }
            return null;
        });
        entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test03entity.addtable.AutodatabaseTest03.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test03entity.addcolumn.AutodatabaseTest03.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test03entity.updatecolumn.AutodatabaseTest03.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test03entity.deletecolumn.AutodatabaseTest03.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
    }
}
