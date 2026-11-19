package com.xuesinuo.pignoo.test.autodatabase.scanerentity.child.sub;

import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

/**
 * scanChildPackages 用例的子包实体：只有递归扫描时才应该被扫到
 * <p>
 * Subpackage entity for the scanChildPackages cases: it should only be found by a recursive scan
 */
@Table("adsc_child_sub")
@Data
public class AdscChildSub {
    private Long id;
    private String name;
}
