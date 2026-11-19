package com.xuesinuo.pignoo.test.core.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.implement.BasePignoo;
import com.xuesinuo.pignoo.test.fixture.Plain;

/**
 * setter自动回写集成测试：验证writer查出的实体调用setter时同步更新数据库的边界
 * <p>
 * Integration test for the setter write-through proxy
 */
@SpringBootTest
public class SetterProxyTest {

    public final DataSource dataSource;

    public SetterProxyTest(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** 用另一个Pignoo（另一条连接）读回数据库中的真实值 */
    private Plain readByAnotherConnection(Long id) {
        try (var other = new BasePignoo(this.dataSource)) {
            return other.reader(Plain.class).filter(Plain::getId, "==", id).getAny();
        }
    }

    @Test
    public void setterWritesThroughByDefault() {
        try (var pignoo = new BasePignoo(this.dataSource, new PignooConfig())) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            Plain plain = new Plain();
            plain.setName("原始名字");
            plain.setCode("CODE-1");
            Plain saved = writer.add(plain);

            saved.setName("新名字");

            assertThat(saved.getName()).as("setter应当同时改到内存中的对象").isEqualTo("新名字");
            Plain back = this.readByAnotherConnection(saved.getId());
            assertThat(back).as("另开一条连接应当能读到这一行").isNotNull();
            assertThat(back.getName()).as("默认开启setter代理时，调用setter应当立即UPDATE到数据库").isEqualTo("新名字");
        }
    }

    @Test
    public void setterDoesNotWriteWhenProxyDisabled() {
        PignooConfig config = new PignooConfig();
        config.setOpenSetterProxy(false);
        try (var pignoo = new BasePignoo(this.dataSource, config)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            Plain plain = new Plain();
            plain.setName("原始名字");
            Plain saved = writer.add(plain);

            saved.setName("新名字");

            assertThat(saved.getName()).as("关闭setter代理后，setter仍然会改内存中的对象").isEqualTo("新名字");
            Plain back = this.readByAnotherConnection(saved.getId());
            assertThat(back.getName()).as("openSetterProxy=false时，调用setter不应写库").isEqualTo("原始名字");
        }
    }

    @Test
    public void readerEntityHasNoSetterProxy() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            Plain plain = new Plain();
            plain.setName("原始名字");
            Long id = writer.add(plain).getId();

            Plain fromReader = pignoo.reader(Plain.class).filter(Plain::getId, "==", id).getAny();
            fromReader.setName("新名字");

            assertThat(fromReader.getName()).as("reader查出的实体是原始对象，setter只改内存").isEqualTo("新名字");
            Plain back = this.readByAnotherConnection(id);
            assertThat(back.getName()).as("只读reader查出的实体调用setter不应写库").isEqualTo("原始名字");
        }
    }

    @Test
    public void setterDoesNotWriteAfterPignooClosed() {
        Plain saved;
        try (var pignoo = new BasePignoo(this.dataSource)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            Plain plain = new Plain();
            plain.setName("原始名字");
            saved = writer.add(plain);
        }

        assertThatCode(() -> saved.setName("新名字"))
                .as("Pignoo关闭后调用代理的setter不应抛异常")
                .doesNotThrowAnyException();
        assertThat(saved.getName()).as("Pignoo关闭后setter仍然会改内存中的对象").isEqualTo("新名字");

        Plain back = this.readByAnotherConnection(saved.getId());
        assertThat(back.getName()).as("Pignoo关闭后代理失效，setter不应再写库").isEqualTo("原始名字");
    }

    @Test
    public void eachSetterIsAnIndependentUpdate() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            var writer = pignoo.writer(Plain.class);
            writer.removeAll();
            Plain plain = new Plain();
            plain.setName("原始名字");
            plain.setCode("CODE-1");
            Plain saved = writer.add(plain);
            Long id = saved.getId();

            saved.setName("新名字");
            Plain middle = this.readByAnotherConnection(id);
            assertThat(middle.getName()).as("第一个setter应当已经单独提交到数据库").isEqualTo("新名字");
            assertThat(middle.getCode()).as("多字段setter不是一条原子UPDATE，此时第二个字段还是旧值").isEqualTo("CODE-1");

            saved.setCode("CODE-2");
            Plain end = this.readByAnotherConnection(id);
            assertThat(end.getName()).as("两个setter都执行后name应当是新值").isEqualTo("新名字");
            assertThat(end.getCode()).as("两个setter都执行后code应当是新值").isEqualTo("CODE-2");
        }
    }
}
