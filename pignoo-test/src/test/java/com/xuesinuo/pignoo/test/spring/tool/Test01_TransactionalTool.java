package com.xuesinuo.pignoo.test.spring.tool;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.xuesinuo.pignoo.core.Pignoo;
import com.xuesinuo.pignoo.test.spring.Test01_Transactional;

@Component
public class Test01_TransactionalTool {

    private final Pignoo pignoo;
    private final Test01_TransactionalTool2 tool;

    public Test01_TransactionalTool(@Autowired Pignoo pignoo, @Autowired Test01_TransactionalTool2 tool) {
        this.pignoo = pignoo;
        this.tool = tool;
    }

    private static String RandomCode(int length) {
        return UUID.randomUUID().toString().toUpperCase().replaceAll("-", "").substring(0, length);
    }

    @Transactional
    public Test01_Transactional.SpringTest01 add() {
        Test01_Transactional.SpringTest01 dataInDb = new Test01_Transactional.SpringTest01();
        dataInDb.setName(RandomCode(4));
        dataInDb.setCode(RandomCode(5));
        dataInDb = pignoo.writer(Test01_Transactional.SpringTest01.class).add(dataInDb);
        dataInDb.setCode(RandomCode(6));
        return dataInDb;
    }

    @Transactional
    public void rollback() {
        Test01_Transactional.SpringTest01 object = new Test01_Transactional.SpringTest01();
        object.setName(RandomCode(4));
        object.setCode(RandomCode(5));
        Long id = pignoo.writer(Test01_Transactional.SpringTest01.class).add(object).getId();
        if (id >= Long.MIN_VALUE) { // 一定触发回滚
            throw new RuntimeException(id + "");
        }
    }

    @Transactional
    public void propagation() {
        Test01_Transactional.SpringTest01 object = new Test01_Transactional.SpringTest01();
        object.setName(RandomCode(4));
        object.setCode(RandomCode(5));
        Long id = pignoo.writer(Test01_Transactional.SpringTest01.class).add(object).getId();
        tool.propagation(); // 子事务
        if (id >= Long.MIN_VALUE) {
            throw new RuntimeException(id + "");
        }
    }
}

@Component
class Test01_TransactionalTool2 {

    private final Pignoo pignoo;

    public Test01_TransactionalTool2(@Autowired Pignoo pignoo) {
        this.pignoo = pignoo;
    }

    private static String RandomCode(int length) {
        return UUID.randomUUID().toString().toUpperCase().replaceAll("-", "").substring(0, length);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void propagation() {
        Test01_Transactional.SpringTest01 dataInDb = new Test01_Transactional.SpringTest01();
        dataInDb.setName(RandomCode(6));
        dataInDb.setCode(RandomCode(7));
        dataInDb = pignoo.writer(Test01_Transactional.SpringTest01.class).add(dataInDb);
    }

}
