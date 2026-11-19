package com.xuesinuo.pignoo.test.autodatabase.scanerentity.filter;

import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

/**
 * 扫描过滤用例的实体：四种可见性/嵌套形式各一个，且都带 {@link Table} 注解，只由过滤规则决定是否被识别
 * <p>
 * Entities for the scan filter case: four visibility/nesting forms, all annotated with {@link Table},
 * so only the filter rule decides which ones are recognized
 */
@Table("adsc_filter_top")
@Data
public class AdscFilter {
    private Long id;
    private String name;

    /** public 静态内部类：应被识别 */
    @Table("adsc_filter_static")
    @Data
    public static class AdscFilterStatic {
        private Long id;
        private String name;
    }

    /** public 非静态内部类：不应被识别 */
    @Table("adsc_filter_member")
    @Data
    public class AdscFilterMember {
        private Long id;
        private String name;
    }
}

/** 非 public 的顶层类：不应被识别 */
@Table("adsc_filter_package")
@Data
class AdscFilterPackage {
    private Long id;
    private String name;
}
