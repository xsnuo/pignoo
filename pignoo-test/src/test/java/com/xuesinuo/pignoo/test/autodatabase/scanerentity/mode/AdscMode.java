package com.xuesinuo.pignoo.test.autodatabase.scanerentity.mode;

import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

/**
 * BuildMode 四级用例的实体，独占一个包，保证扫描范围不会误伤其它用例的实体
 * <p>
 * Entities for the four BuildMode cases; they own this package so the scan range never touches other cases
 */
@Table("adsc_mode_change")
@Data
public class AdscMode {
    private Long id;
    private String name;
    /** 表里是 varchar，实体是 Integer：触发改列 */
    private Integer qty;
    /** 表里没有这一列：触发加列 */
    private String extra;

    /** 表不存在：触发建表 */
    @Table("adsc_mode_create")
    @Data
    public static class AdscModeCreate {
        private Long id;
        private String name;
    }
}
