package com.xuesinuo.pignoo.test.autodatabase;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.autodatabase.EntityScanConfig;
import com.xuesinuo.pignoo.autodatabase.EntityScaner;
import com.xuesinuo.pignoo.autodatabase.exception.ScanException;
import com.xuesinuo.pignoo.core.PignooConfig;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class Test01_RunSQL {
    public final DataSource dataSource;

    public Test01_RunSQL(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Test
    public void radically() throws SQLException {
        try (var con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            con.createStatement().execute("drop table if exists autodatabase_test01");
            con.commit();
        }
        PignooConfig pignooConfig = new PignooConfig();
        EntityScanConfig entityScanConfig = new EntityScanConfig();
        entityScanConfig.setAnnotationClassOnly(false);
        entityScanConfig.setStrictColumnType(true);
        entityScanConfig.setBuildMode(EntityScanConfig.BuildMode.RADICALLY);
        entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.addtable.AutodatabaseTest01.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.addcolumn.AutodatabaseTest01.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.updatecolumn.AutodatabaseTest01.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.deletecolumn.AutodatabaseTest01.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
    }

    @Test
    public void usability() throws SQLException {
        try (var con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            con.createStatement().execute("drop table if exists autodatabase_test01");
            con.commit();
        }
        PignooConfig pignooConfig = new PignooConfig();
        EntityScanConfig entityScanConfig = new EntityScanConfig();
        entityScanConfig.setAnnotationClassOnly(false);
        entityScanConfig.setStrictColumnType(true);
        entityScanConfig.setBuildMode(EntityScanConfig.BuildMode.USABILITY);
        int catchs = 0;
        entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.addtable.AutodatabaseTest01.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.addcolumn.AutodatabaseTest01.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.updatecolumn.AutodatabaseTest01.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        try {
            entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.deletecolumn.AutodatabaseTest01.class });
            new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        } catch (ScanException e) {
            catchs++;
            log.info("易用模式下无法执行删除列操作");
        }
        if (catchs != 1) {
            throw new RuntimeException("Usability error");
        }
    }

    @Test
    public void safely() throws SQLException {
        try (var con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            con.createStatement().execute("drop table if exists autodatabase_test01");
            con.commit();
        }
        PignooConfig pignooConfig = new PignooConfig();
        EntityScanConfig entityScanConfig = new EntityScanConfig();
        entityScanConfig.setAnnotationClassOnly(false);
        entityScanConfig.setStrictColumnType(true);
        entityScanConfig.setBuildMode(EntityScanConfig.BuildMode.SAFELY);
        int catchs = 0;
        entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.addtable.AutodatabaseTest01.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.addcolumn.AutodatabaseTest01.class });
        new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        try {
            entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.updatecolumn.AutodatabaseTest01.class });
            new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        } catch (ScanException e) {
            catchs++;
            log.info("安全模式下无法执行修改列操作");
        }
        try {
            entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.deletecolumn.AutodatabaseTest01.class });
            new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        } catch (ScanException e) {
            catchs++;
            log.info("安全模式下无法执行删除列操作");
        }
        if (catchs != 2) {
            throw new RuntimeException("Safely error");
        }
    }

    @Test
    public void carefully() throws SQLException {
        try (var con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            con.createStatement().execute("drop table if exists autodatabase_test01");
            con.commit();
        }
        PignooConfig pignooConfig = new PignooConfig();
        EntityScanConfig entityScanConfig = new EntityScanConfig();
        entityScanConfig.setAnnotationClassOnly(false);
        entityScanConfig.setStrictColumnType(true);
        entityScanConfig.setBuildMode(EntityScanConfig.BuildMode.CAREFULLY);
        int catchs = 0;
        try {
            entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.addtable.AutodatabaseTest01.class });
            new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        } catch (ScanException e) {
            catchs++;
            log.info("谨慎模式下无法执行新增表操作");
        }
        try {
            entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.addcolumn.AutodatabaseTest01.class });
            new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        } catch (ScanException e) {
            catchs++;
            log.info("谨慎模式下无法执行新增列操作");
        }
        try {
            entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.updatecolumn.AutodatabaseTest01.class });
            new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        } catch (ScanException e) {
            catchs++;
            log.info("谨慎模式下无法执行修改列操作");
        }
        try {
            entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.deletecolumn.AutodatabaseTest01.class });
            new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        } catch (ScanException e) {
            catchs++;
            log.info("谨慎模式下无法执行删除列操作");
        }
        if (catchs != 4) {
            throw new RuntimeException("Carefully error");
        }
    }

    @Test
    public void unBreak() throws SQLException {
        try (var con = dataSource.getConnection()) {
            con.setAutoCommit(false);
            con.createStatement().execute("drop table if exists autodatabase_test01");
            con.commit();
        }
        PignooConfig pignooConfig = new PignooConfig();
        EntityScanConfig entityScanConfig = new EntityScanConfig();
        entityScanConfig.setAnnotationClassOnly(false);
        entityScanConfig.setStrictColumnType(true);
        entityScanConfig.setBuildMode(EntityScanConfig.BuildMode.CAREFULLY);
        entityScanConfig.setBreakRunning(false);
        int catchs = 0;
        try {
            entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.addtable.AutodatabaseTest01.class });
            new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        } catch (ScanException e) {
            catchs++;
        }
        try {
            entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.addcolumn.AutodatabaseTest01.class });
            new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        } catch (ScanException e) {
            catchs++;
        }
        try {
            entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.updatecolumn.AutodatabaseTest01.class });
            new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        } catch (ScanException e) {
            catchs++;
        }
        try {
            entityScanConfig.setClassesForScanPackage(new Class[] { com.xuesinuo.pignoo.test.autodatabase.test01entity.deletecolumn.AutodatabaseTest01.class });
            new EntityScaner(dataSource, pignooConfig, entityScanConfig).scan();
        } catch (ScanException e) {
            catchs++;
        }
        if (catchs != 0) {
            throw new RuntimeException("Carefully error");
        }
    }
}
