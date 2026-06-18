package com.xuesinuo.pignoo.test.spring;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.test.spring.tool.Test01_TransactionalTool;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class Test01_Transactional {

    private final Pignoo pignoo;
    private final Test01_TransactionalTool tool;

    public Test01_Transactional(@Autowired Pignoo pignoo, @Autowired Test01_TransactionalTool tool) {
        this.pignoo = pignoo;
        this.tool = tool;
    }

    private static String RandomCode(int length) {
        return UUID.randomUUID().toString().toUpperCase().replaceAll("-", "").substring(0, length);
    }

    @Data
    public static class SpringTest01 {
        private Long id;
        private String name;
        private String code;
    }

    @Test
    public void submit() {
        SpringTest01 dataInJava = tool.add();
        if (!pignoo.reader(SpringTest01.class).sort(SpringTest01::getId, SMode.MAX_FIRST).getFirst().getId().equals(dataInJava.getId())) {
            throw new RuntimeException("submit error 1");
        }
        dataInJava.setName(RandomCode(7));
        if (pignoo.reader(SpringTest01.class).filter(SpringTest01::getId, "==", dataInJava.getId()).getFirst().getCode().length() != 6) {
            throw new RuntimeException("submit error 2");
        }
    }

    @Test
    public void rollback() {
        try {
            tool.rollback();
        } catch (Exception e) { // gru外catch不影响回滚
            String id = e.getMessage();
            SpringTest01 t4 = pignoo.reader(SpringTest01.class).sort(SpringTest01::getId, SMode.MAX_FIRST).getFirst();
            if (t4 != null && t4.getId() != null && t4.getId().toString().equals(id)) {
                throw new RuntimeException("Rollback error");
            }
        }
    }

    @Test
    public void propagation() {
        pignoo.writer(SpringTest01.class).removeAll();
        try {
            tool.propagation();
        } catch (Exception e) {}
        if (pignoo.reader(SpringTest01.class).size() != 1) {
            throw new RuntimeException("Propagation error");
        }
    }
}