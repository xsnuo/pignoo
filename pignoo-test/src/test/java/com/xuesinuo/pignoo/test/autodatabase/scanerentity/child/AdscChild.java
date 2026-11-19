package com.xuesinuo.pignoo.test.autodatabase.scanerentity.child;

import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

/**
 * scanChildPackages 用例的锚点实体：它自己在锚点包里，另一个实体在子包 {@code child.sub} 里
 * <p>
 * Anchor entity for the scanChildPackages cases: it sits in the anchor package while the other entity
 * lives in the {@code child.sub} subpackage
 */
@Table("adsc_child_parent")
@Data
public class AdscChild {
    private Long id;
    private String name;
}
