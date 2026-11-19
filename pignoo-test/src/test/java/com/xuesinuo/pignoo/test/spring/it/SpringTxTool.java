package com.xuesinuo.pignoo.test.spring.it;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.xuesinuo.pignoo.core.Pignoo;

/**
 * 事务边界辅助Bean：@Transactional必须靠Spring代理生效，所以事务体统一从这里进出
 * <p>
 * Transaction boundary helper bean
 */
@Component
public class SpringTxTool {

    private final Pignoo pignoo;
    private final SpringTxInnerTool inner;

    public SpringTxTool(@Autowired Pignoo pignoo, @Autowired SpringTxInnerTool inner) {
        this.pignoo = pignoo;
        this.inner = inner;
    }

    /**
     * 在Spring事务里执行一段有返回值的逻辑，正常返回即提交
     */
    @Transactional
    public <T> T call(Function<Pignoo, T> body) {
        return body.apply(this.pignoo);
    }

    /**
     * 在Spring事务里执行一段无返回值的逻辑，body抛出的运行期异常会触发回滚
     */
    @Transactional
    public void run(Consumer<Pignoo> body) {
        body.accept(this.pignoo);
    }

    /**
     * 在Spring事务（外层事务）里执行一段可以继续调用内层事务方法的逻辑：内层事务方法的调用时机、
     * 异常是否被外层吞掉，全部交给body决定
     */
    @Transactional
    public void runNested(BiConsumer<Pignoo, SpringTxInnerTool> body) {
        body.accept(this.pignoo, this.inner);
    }
}

/**
 * 内层事务Bean：传播行为只有跨Bean调用才由Spring代理接管，自调用不走代理，故必须单独成Bean
 * <p>
 * Inner transaction bean
 */
@Component
class SpringTxInnerTool {

    private final Pignoo pignoo;

    public SpringTxInnerTool(@Autowired Pignoo pignoo) {
        this.pignoo = pignoo;
    }

    /**
     * 内层独立事务：挂起外层事务，自己提交、自己回滚
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void requiresNew(Consumer<Pignoo> body) {
        body.accept(this.pignoo);
    }

    /**
     * 内层加入外层事务：与外层同一个事务，同生共死
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void required(Consumer<Pignoo> body) {
        body.accept(this.pignoo);
    }
}
