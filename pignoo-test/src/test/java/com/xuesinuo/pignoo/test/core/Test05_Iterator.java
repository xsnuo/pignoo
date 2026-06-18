package com.xuesinuo.pignoo.test.core;

import java.util.Iterator;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.implement.BasePignoo;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class Test05_Iterator {

    public final DataSource dataSource;

    public Test05_Iterator(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Data
    public static class Test05 {
        private Long id;
        private String name;
    }

    @Test
    public void forEach() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            var reader = pignoo.reader(Test05.class);
            var writer = pignoo.writer(Test05.class);
            int[] is = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 35, 50, 98, 99, 100, 101, 102, 103, 104, 147, 177, 198, 199, 200, 201, 202, 298, 299, 300, 301, 302 };
            for (int ix : is) {
                writer.removeAll();
                for (int i = 1; i <= ix; i++) {
                    Test05 test05 = new Test05();
                    test05.setId(Long.valueOf(i));
                    writer.add(test05);
                }
                int i = 1;
                for (Test05 item : writer) {
                    if (item.getId() != i++) {
                        throw new RuntimeException("ForEach error 1");
                    }
                    item.setName(item.getId().toString());
                }
                if (i != ix + 1) {
                    throw new RuntimeException("ForEach error 2");
                }
                for (Test05 item : reader) {
                    if (!item.getId().toString().equals(item.getName())) {
                        throw new RuntimeException("ForEach error 3");
                    }
                }
            }
        }
    }

    @Test
    public void forIteratorSettings() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            var reader = pignoo.reader(Test05.class);
            var writer = pignoo.writer(Test05.class);
            int[] is = { 0, 1, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17 };
            for (int ix : is) {
                writer.removeAll();
                for (int i = 1; i <= ix; i++) {
                    Test05 test05 = new Test05();
                    test05.setId(Long.valueOf(i));
                    writer.add(test05);
                }
                int i = ix;
                writer.setIteratorStep(5);
                writer.setIteratorSortMode(SMode.MAX_FIRST);
                for (Test05 item : writer) {
                    if (item.getId() != i--) {
                        throw new RuntimeException("Iterator settings error 1");
                    }
                    item.setName(item.getId().toString());
                }
                if (i != 0) {
                    throw new RuntimeException("Iterator settings error 2");
                }
                reader.setIteratorStep(4);
                for (Test05 item : reader) {
                    if (!item.getId().toString().equals(item.getName())) {
                        throw new RuntimeException("Iterator settings error 3");
                    }
                }
            }
        }
    }

    @Test
    public void forIterator() {
        try (var pignoo = new BasePignoo(this.dataSource)) {
            var writer = pignoo.writer(Test05.class);
            writer.removeAll();
            for (int i = 1; i <= 44; i++) {
                Test05 test05 = new Test05();
                test05.setId(Long.valueOf(i));
                writer.add(test05);
            }
            Iterator<Test05> iterator = writer.iterator(6, SMode.MAX_FIRST, 4, 30);
            int i = 40;
            while (iterator.hasNext()) {
                Test05 item = iterator.next();
                if (item.getId() != i--) {
                    throw new RuntimeException("iterator error 1");
                }
                item.setName(item.getId().toString());
            }
            if (i != 10) {
                throw new RuntimeException("iterator error 2");
            }
            for (Test05 item : writer) {
                if (item.getId() > 10 && item.getId() <= 40 && !item.getId().toString().equals(item.getName())) {
                    throw new RuntimeException("iterator error 3");
                }
            }
        }
    }
}
