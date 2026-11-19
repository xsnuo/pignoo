package com.xuesinuo.pignoo.test.autodatabase.scanerentity.annotation;

import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

/**
 * annotationClassOnly 用例的实体：一个带 {@link Table} 注解，一个不带
 * <p>
 * Entities for the annotationClassOnly cases: one carries {@link Table}, the other does not
 */
@Table("adsc_ann_marked")
@Data
public class AdscAnn {
    private Long id;
    private String name;

    /** 不带注解，表名按驼峰转下划线推导为 adsc_ann_plain */
    @Data
    public static class AdscAnnPlain {
        private Long id;
        private String name;
    }
}
