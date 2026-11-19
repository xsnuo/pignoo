package com.xuesinuo.pignoo.test.fixture;

import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

/**
 * 覆盖boolean字段的各种getter/setter命名分支
 * <p>
 * admin -> isAdmin/setAdmin，isActive -> isActive/setActive，isadmin -> isIsadmin/setIsadmin，
 * is_admin -> is_admin/set_admin，is -> isIs/setIs
 */
@Table("fx_bool")
@Data
public class BoolNaming {
    private Long id;
    private boolean admin;
    private boolean isActive;
    private boolean isadmin;
    private boolean is_admin;
    private boolean is;
}
