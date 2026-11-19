package com.xuesinuo.pignoo.test.core.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.config.NamingMode;
import com.xuesinuo.pignoo.core.entity.EntityMapper;
import com.xuesinuo.pignoo.test.fixture.BoolNaming;

/**
 * boolean属性的getter/setter命名推导：五种写法都必须能映射成功
 * <p>
 * Getter/setter name derivation for boolean fields: all five spellings must be mapped successfully
 * <p>
 * admin -> isAdmin/setAdmin，isActive -> isActive/setActive，isadmin -> isIsadmin/setIsadmin，
 * is_admin -> is_admin/set_admin，is -> isIs/setIs
 */
public class BoolNamingTest {

    private static PignooConfig config() {
        return new PignooConfig();
    }

    /**
     * 五个boolean属性全部解析成功，列名按命名规则推导
     */
    @Test
    public void columns() {
        EntityMapper<BoolNaming> mapper = EntityMapper.build(BoolNaming.class, config());
        assertEquals("fx_bool", mapper.tableName(), "表名应取自@Table");
        assertEquals(List.of("id", "admin", "is_active", "isadmin", "is_admin", "is"), mapper.columns(), "boolean属性的列名应按驼峰转下划线推导，不因is前缀而特殊处理");
        assertEquals("id", mapper.primaryKeyColumn(), "主键仍按默认约定取id");
    }

    /**
     * setter方法名的五种推导分支
     */
    @Test
    public void setterNames() {
        EntityMapper<BoolNaming> mapper = EntityMapper.build(BoolNaming.class, config());
        assertEquals(List.of("setId", "setAdmin", "setActive", "setIsadmin", "set_admin", "setIs"), mapper.setterNames(), "boolean属性的setter名推导必须与Lombok生成的方法名一致");

        int indexOfIsActive = mapper.columns().indexOf("is_active");
        assertEquals("setActive", mapper.setterNames().get(indexOfIsActive), "isActive属性的setter应为setActive（去掉is前缀）");
        int indexOfIsAdmin = mapper.columns().indexOf("is_admin");
        assertEquals("set_admin", mapper.setterNames().get(indexOfIsAdmin), "is_admin属性的setter应为set_admin（去掉is前缀后原样拼接）");
    }

    /**
     * getter方法名的五种推导分支：能被getColumnByFunction反查到列名，即说明getter名推导正确
     */
    @Test
    public void getterNames() {
        EntityMapper<BoolNaming> mapper = EntityMapper.build(BoolNaming.class, config());
        assertEquals("id", mapper.getColumnByFunction(BoolNaming::getId), "getId应映射到id列");
        assertEquals("admin", mapper.getColumnByFunction(BoolNaming::isAdmin), "admin属性的getter应为isAdmin");
        assertEquals("is_active", mapper.getColumnByFunction(BoolNaming::isActive), "isActive属性的getter应为isActive");
        assertEquals("isadmin", mapper.getColumnByFunction(BoolNaming::isIsadmin), "isadmin属性的getter应为isIsadmin（is后面接小写，按普通属性处理）");
        assertEquals("is_admin", mapper.getColumnByFunction(BoolNaming::is_admin), "is_admin属性的getter应为is_admin（is后面不是小写，按已有is前缀处理）");
        assertEquals("is", mapper.getColumnByFunction(BoolNaming::isIs), "is属性的getter应为isIs（长度不足3，按普通属性处理）");
    }

    /**
     * SAME模式只改列名，不改getter/setter的推导
     */
    @Test
    public void sameNamingModeKeepsAccessorNames() {
        PignooConfig same = config();
        same.setNamingMode(NamingMode.SAME);
        EntityMapper<BoolNaming> mapper = EntityMapper.build(BoolNaming.class, same);
        assertEquals(List.of("id", "admin", "isActive", "isadmin", "is_admin", "is"), mapper.columns(), "SAME模式下列名应等于属性名原样");
        assertEquals(List.of("setId", "setAdmin", "setActive", "setIsadmin", "set_admin", "setIs"), mapper.setterNames(), "命名模式不应影响getter/setter方法名的推导");
    }
}
