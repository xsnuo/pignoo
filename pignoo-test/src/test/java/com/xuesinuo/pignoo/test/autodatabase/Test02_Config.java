package com.xuesinuo.pignoo.test.autodatabase;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.autodatabase.EntityScanConfig;
import com.xuesinuo.pignoo.autodatabase.EntityScaner;
import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.test.autodatabase.test02entity.AutodatabaseTest02;

@SpringBootTest
public class Test02_Config {
    public final DataSource dataSource;

    public Test02_Config(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Test
    public void scaner() {
        PignooConfig pignooConfig = new PignooConfig();
        EntityScanConfig entityScanConfig = new EntityScanConfig();
        entityScanConfig.setBreakRunning(false);

        entityScanConfig.setClassesForScanPackage(new Class[] { AutodatabaseTest02.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan(); // child

        entityScanConfig.setAnnotationClassOnly(false);
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan(); // test02 + child + child_public_static

        entityScanConfig.setScanChildPackages(false);
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan(); // test02

        entityScanConfig.setClassesForScanPackage(null);
        entityScanConfig.setPackages(new String[] { "com.xuesinuo.pignoo.test.autodatabase.test02entity.childpackage" });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan(); // child + child_public_static
    }
}
