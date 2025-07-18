package com.xuesinuo.pignoo.core;

import com.xuesinuo.pignoo.core.config.AnnotationMode;
import com.xuesinuo.pignoo.core.config.AnnotationMode.AnnotationMixMode;
import com.xuesinuo.pignoo.core.config.DatabaseEngine;

import lombok.Data;

/**
 * Pignoo配置
 * <p>
 * Pignoo configuration
 * 
 * @author xuesinuo
 * @since 0.1.0
 */
@Data
public class PignooConfig {
    /**
     * 数据库引擎
     * <p>
     * Database engine
     */
    private DatabaseEngine engine;

    /**
     * 是否使用事务；配置在Gru或外部数据源管理上是，无效！是否使用事务由外部决定
     * <p>
     * Whether to use transactions; Invalid if configured in Gru or external data source management! Whether to use transactions is determined by external
     */
    private Boolean useTransaction;

    /**
     * 注解使用方式
     * <p>
     * Annotation usage
     * 
     * @version 0.2.0
     */
    private AnnotationMode annotationMode;

    /**
     * 注解混合使用方式
     * <p>
     * Annotation mixed usage
     * 
     * @version 0.2.0
     */
    private AnnotationMixMode annotationMixMode;
}
