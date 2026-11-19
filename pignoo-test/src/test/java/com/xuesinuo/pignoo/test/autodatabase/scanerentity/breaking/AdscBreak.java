package com.xuesinuo.pignoo.test.autodatabase.scanerentity.breaking;

import com.xuesinuo.pignoo.core.annotation.Table;

import lombok.Data;

/**
 * breakRunning 用例的实体：一个带无法自动处理的问题，一个完全可以自动处理
 * <p>
 * Entities for the breakRunning cases: one carries an unhandleable problem, the other is fully fixable
 */
@Table("adsc_break_bad")
@Data
public class AdscBreak {
    private Long id;
    private String name;
    /** Object 无法映射成 SQL 类型：产生无法自动处理的问题 */
    private Object payload;

    /** 结构完好、可以建表：用来验证 breakRunning=false 时仍会继续执行 */
    @Table("adsc_break_good")
    @Data
    public static class AdscBreakOk {
        private Long id;
        private String name;
    }
}
