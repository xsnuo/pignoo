package com.xuesinuo.pignoo.test.core;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.xuesinuo.pignoo.core.PignooConfig;
import com.xuesinuo.pignoo.core.PignooFilter;
import com.xuesinuo.pignoo.core.PignooFilter.FMode;
import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.annotation.Link;
import com.xuesinuo.pignoo.core.implement.BasePignoo;

import lombok.Data;

@SpringBootTest
public class Test01_CRUD {
    public static final Object NULL = null;
    public final DataSource dataSource;
    public PignooConfig config = new PignooConfig();

    public Test01_CRUD(@Autowired DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static String RandomCode(int length) {
        return UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, length);
    }

    @Data
    public static class Test01 {
        private Long id;
        private String name;
        private String code;
        private BigDecimal money;
    }

    @Link(Test01.class)
    @Data
    public static class Test01Simple {
        private Long id;
        private String name;
    }

    @Link(Test01.class)
    @Data
    public static class Test01Setter {
        private Long id;
        private String name;

        public void setName(String name) {
            this.name = name + "宝宝";
        }
    }

    @Test
    public void insert() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, config)) {
            var reader = pignoo.reader(Test01.class).sort(Test01::getId, SMode.MAX_FIRST);
            var writer = pignoo.writer(Test01.class);
            String newName = RandomCode(4);
            Test01 newTest01 = new Test01();
            newTest01.setName(newName);
            newTest01 = writer.add(newTest01);
            Test01 theNewOne = reader.getFirst();
            if (!theNewOne.getName().equals(newTest01.getName()) || !theNewOne.getName().equals(newName)) {
                throw new RuntimeException("Add error");
            }
            newName = RandomCode(5);
            newTest01.setName(newName);
            theNewOne = reader.getFirst();
            if (!theNewOne.getName().equals(newName) || !newTest01.getName().equals(newName)) {
                throw new RuntimeException("Update error");
            }

            Test01Setter t1Setter = pignoo.writer(Test01Setter.class).sort(Test01Setter::getId, SMode.MAX_FIRST).getFirst();
            newName = RandomCode(6);
            t1Setter.setName(newName);
            theNewOne = reader.getFirst();
            if (!theNewOne.getName().equals(newName + "宝宝")) {
                throw new RuntimeException("Setter error");
            }
        }
    }

    @Test
    public void delete() {
        try (BasePignoo pignoo = new BasePignoo(this.dataSource, config)) {
            var writer = pignoo.writer(Test01.class);
            Test01 newTest01 = new Test01();
            Test01 t1 = writer.add(newTest01);
            Test01 t2 = writer.add(newTest01);
            Test01 t3 = writer.add(newTest01);
            long row = writer.removeById(t3);
            var reader = pignoo.reader(Test01.class).sort(Test01::getId, SMode.MAX_FIRST);
            Test01 theLastOne = reader.getFirst();
            if (!theLastOne.getId().equals(t2.getId()) || row != 1) {
                throw new RuntimeException("Delete error 1");
            }
            row = writer.copyWriter().filter(Test01::getId, ">=", t2.getId()).removeAll();
            theLastOne = reader.getFirst();
            if (!theLastOne.getId().equals(t1.getId()) || row != 1) {
                throw new RuntimeException("Delete error 2");
            }
            row = writer.removeAll();
            theLastOne = reader.getFirst();
            if (theLastOne != null || row < 1) {
                throw new RuntimeException("Delete error 3");
            }
        }
    }

    @Test
    public void update() {
        Test01 t1Read = null;
        try (var pignoo = new BasePignoo(this.dataSource, config)) {
            var writer = pignoo.writer(Test01.class);
            Test01 newTest01 = new Test01();
            Test01 t1 = writer.add(newTest01);
            String newName = RandomCode(4);
            t1.setName(newName);
            t1Read = writer.sort(Test01::getId, SMode.MAX_FIRST).getFirst();
            if (!t1.getName().equals(t1Read.getName())) {
                throw new RuntimeException("Update error 1");
            }
            newName = RandomCode(5);
            t1Read.setName(newName);
            Test01 t1ReadAgain = writer.getFirst();
            if (!t1Read.getName().equals(t1ReadAgain.getName())) {
                throw new RuntimeException("Update error 2");
            }
        }
        try (var pignoo = new BasePignoo(this.dataSource, config)) {
            String newName = RandomCode(6);
            t1Read.setName(newName);
            Test01 t1ReadAgain = pignoo.reader(Test01.class).filter(Test01::getId, "==", t1Read.getId()).getAny();
            if (t1Read.getName().equals(t1ReadAgain.getName())) {
                throw new RuntimeException("Update error 3");
            }
            var writer = pignoo.writer(Test01.class);
            Test01 mixById = new Test01();
            mixById.setId(t1ReadAgain.getId());
            mixById.setCode(RandomCode(7));
            writer.mixById(mixById);
            Test01 mixed = pignoo.reader(Test01.class).filter(Test01::getId, "==", t1ReadAgain.getId()).getAny();
            if (!mixed.getName().equals(t1ReadAgain.getName()) || !mixed.getCode().equals(mixById.getCode())) {
                throw new RuntimeException("Update error 4");
            }
            Test01 replaceById = new Test01();
            replaceById.setId(t1ReadAgain.getId());
            replaceById.setCode(RandomCode(8));
            writer.replaceById(replaceById);
            Test01 replaced = pignoo.reader(Test01.class).filter(Test01::getId, "==", t1ReadAgain.getId()).getAny();
            if (replaced.getName() != null || !replaced.getCode().equals(replaceById.getCode())) {
                throw new RuntimeException("Update error 5");
            }
            writer.filter(Test01::getId, "==", t1ReadAgain.getId());
            mixById.setName(RandomCode(9));
            mixById.setCode(null);
            writer.mixAll(mixById);
            mixed = pignoo.reader(Test01.class).filter(Test01::getId, "==", t1ReadAgain.getId()).getAny();
            if (!mixed.getName().equals(mixById.getName()) || !mixed.getCode().equals(replaceById.getCode())) {
                throw new RuntimeException("Update error 6");
            }
            replaceById.setName(null);
            replaceById.setCode(null);
            writer.replaceAll(replaceById);
            replaced = pignoo.reader(Test01.class).filter(Test01::getId, "==", t1ReadAgain.getId()).getAny();
            if (replaced.getName() != null || replaced.getCode() != null) {
                throw new RuntimeException("Update error 7");
            }
        }
    }

    @Test
    public void select() {
        try (var pignoo = new BasePignoo(this.dataSource, config)) {
            var writer = pignoo.writer(Test01.class);
            writer.removeAll();
            for (long i = 1L; i <= 9L; i++) {
                Test01 t = new Test01();
                t.setId(i);
                if (i % 3 != 0) {
                    t.setName(i + "-NAME");
                    t.setCode(i + "-CODE");
                    t.setMoney(new BigDecimal(i));
                }
                t = writer.add(t);
            }
            var reader = pignoo.reader(Test01.class);
            if (reader.size() != 9L) {
                throw new RuntimeException("Select error 1");
            }
            BigDecimal sum = new BigDecimal(27);
            if (sum.compareTo(reader.sum(Test01::getMoney, BigDecimal.class)) != 0) {
                throw new RuntimeException("Select error 2-1");
            }
            BigDecimal sumNull10 = new BigDecimal(57);
            if (sumNull10.compareTo(reader.sumNullAs(Test01::getMoney, BigDecimal.class, new BigDecimal(10))) != 0) {
                throw new RuntimeException("Select error 2-2");
            }
            BigDecimal avg = new BigDecimal("4.5");
            if (avg.compareTo(reader.avg(Test01::getMoney, BigDecimal.class)) != 0) {
                throw new RuntimeException("Select error 3-1");
            }
            BigDecimal avgNull0 = new BigDecimal("3");
            if (avgNull0.compareTo(reader.avgNullAs(Test01::getMoney, BigDecimal.class, new BigDecimal(0))) != 0) {
                throw new RuntimeException("Select error 3-2");
            }
            if (reader.getAll().size() != 9) {
                throw new RuntimeException("Select error 4");
            }
            if (!reader.getFirst().getId().equals(1L)) {
                throw new RuntimeException("Select error 5");
            }
            if (!reader.get(5, 1).get(0).getId().equals(6L)) {
                throw new RuntimeException("Select error 6");
            }
            var reader1 = reader.copyReader();
            reader1.filter(Test01::getId, ">=", 5L);
            if (reader1.size() != 5L) {
                throw new RuntimeException("Select error 7");
            }
            reader1.filter(Test01::getName, FMode.NOT_LIKE, "7-NAME%");
            if (reader1.size() != 4L) {
                throw new RuntimeException("Select error 8");
            }
            var reader2 = reader.copyReader();
            reader2.filter(f -> f.or(Test01::getName, "like", "7-NAME%")
                    .or(Test01::getCode, "==", null));
            if (reader2.size() != 4L) {
                throw new RuntimeException("Select error 9");
            }
            var reader3 = reader.copyReader();
            PignooFilter<Test01> filterA = new PignooFilter<>();
            PignooFilter<Test01> filterB = new PignooFilter<>();
            PignooFilter<Test01> filter = new PignooFilter<>();
            filterA = filterA.or(Test01::getId, "==", 1L).or(Test01::getId, "==", 2L);
            filterB = filterB.or(Test01::getId, "==", 2L).or(Test01::getId, "==", 3L);
            filter = filterA.and(filterB);
            reader3.filter(filter);
            if (reader3.size() != 1L) {
                throw new RuntimeException("Select error 10");
            }
            var reader4 = reader.copyReader();
            reader4.filter(Test01::getName, "in", Arrays.asList("1-NAME", "2-NAME"));
            if (reader4.size() != 2L) {
                throw new RuntimeException("Select error 11");
            }
            var reader5 = reader.copyReader();
            reader5.filter(Test01::getName, "not in", Arrays.asList("1-NAME", "2-NAME"));
            if (reader5.size() != 7L) {
                throw new RuntimeException("Select error 12");
            }
            var reader6 = reader.copyReader();
            reader6.filter(Test01::getName, "!=", null);
            if (reader6.size() != 6L) {
                throw new RuntimeException("Select error 13");
            }
            var simpleReader = pignoo.reader(Test01Simple.class);
            simpleReader.filter(Test01Simple::getName, "!=" , null);
            List<Test01Simple> simpleList = simpleReader.sort(Test01Simple::getName, SMode.MAX_FIRST).sort(Test01Simple::getId, SMode.MIN_FIRST).get(3, 2);
            if (simpleList.size() != 2 || !simpleList.get(0).getId().equals(5L) || !simpleList.get(1).getId().equals(7L)) {
                throw new RuntimeException("Select error 14");
            }
        }
    }

    @Test
    public void select2() {
        try (var pignoo = new BasePignoo(this.dataSource, config)) {
            var writer = pignoo.writer(Test01.class);
            writer.removeAll();
            for (long i = 1L; i <= 9L; i++) {
                Test01 t = new Test01();
                t.setId(i);
                if (i % 3 != 0) {
                    t.setName(i + "-NAME");
                    t.setCode(i + "-CODE");
                }
                t = writer.add(t);
            }
            var reader = pignoo.reader(Test01.class);
            reader.sort(Test01::getId, SMode.MAX_FIRST);
            Test01 max = reader.getFirst();
            Test01 min = reader.getAny();
            if (max.getId() != 9L || min.getId() != 1L) {
                throw new RuntimeException("Select2 error 1");
            }
            String maxName = reader.max(Test01::getName, String.class); // 8-NAME
            String maxNullName = reader.maxNullAs(Test01::getName, String.class, "9-NAME"); // 9-NAME
            String minName = reader.min(Test01::getName, String.class); // 1-NAME
            String minNullName = reader.minNullAs(Test01::getName, String.class, "0-NAME"); // 0-NAME
            if (!maxName.equals("8-NAME") || !maxNullName.equals("9-NAME") || !minName.equals("1-NAME") || !minNullName.equals("0-NAME")) {
                throw new RuntimeException("Select2 error 2");
            }
            long count = reader.countDistinct(Test01::getName); // 6
            long countNull = reader.countDistinctNullAs(Test01::getName, "NULL-NAME"); // 7
            if (count != 6L || countNull != 7L) {
                throw new RuntimeException("Select2 error 3");
            }
            boolean contains1 = reader.containsId(min); // true
            boolean contains2 = reader.containsIds(List.of(min, max)); // true
            List<Test01> list = new ArrayList<>(List.of(min, max));
            list.add(null);
            boolean contains3 = reader.containsIds(list); // false
            Test01 id10 = new Test01();
            boolean contains4 = reader.containsIds(List.of(min, id10)); // false
            if (!contains1 || !contains2 || contains3 || contains4) {
                throw new RuntimeException("Select2 error 4");
            }
        }
    }

    @Test
    public void select3() {
        try (var pignoo = new BasePignoo(this.dataSource, config)) {
            var writer = pignoo.writer(Test01.class);
            writer.removeAll();
            for (long i = 1L; i <= 9L; i++) {
                Test01 t = new Test01();
                t.setId(i);
                if (i % 3 != 0) {
                    t.setName(i + "-NAME");
                    t.setCode(i + "-CODE");
                }
                t = writer.add(t);
            }
            var reader = pignoo.reader(Test01.class);
            if (reader.size() != 9L) {
                throw new RuntimeException("Select error 1");
            }
            if (reader.copyReader().filter(Test01::getName, "in", Arrays.asList()).size() != 0L) {
                throw new RuntimeException("Select error 2");
            }
            if (reader.copyReader().filter(Test01::getName, "in", NULL).size() != 3L) {
                throw new RuntimeException("Select error 3");
            }
            if (reader.copyReader().filter(Test01::getName, "not in", Arrays.asList()).size() != 9L) {
                throw new RuntimeException("Select error 4");
            }
            if (reader.copyReader().filter(Test01::getName, "not in", NULL).size() != 6L) {
                throw new RuntimeException("Select error 5");
            }
            if (reader.copyReader().filter(Test01::getName, "in", Arrays.asList(NULL, "1-NAME")).size() != 4L) {
                throw new RuntimeException("Select error 6");
            }
            if (reader.copyReader().filter(Test01::getName, "not in", NULL, "1-NAME").size() != 5L) {
                throw new RuntimeException("Select error 7");
            }
            if (reader.copyReader().filter(Test01::getName, "==", NULL).size() != 3L) {
                throw new RuntimeException("Select error 8");
            }
            if (reader.copyReader().filter(Test01::getName, "!=", NULL).size() != 6L) {
                throw new RuntimeException("Select error 9");
            }
            int error = 0;
            try {
                reader.copyReader().filter(Test01::getName, "like", NULL).size();
            } catch (Exception e) {
                error++;
            }
            try {
                reader.copyReader().filter(Test01::getName, "not like", NULL).size();
            } catch (Exception e) {
                error++;
            }
            try {
                reader.copyReader().filter(Test01::getName, ">", NULL).size();
            } catch (Exception e) {
                error++;
            }
            try {
                reader.copyReader().filter(Test01::getName, "<", NULL).size();
            } catch (Exception e) {
                error++;
            }
            try {
                reader.copyReader().filter(Test01::getName, ">=", NULL).size();
            } catch (Exception e) {
                error++;
            }
            try {
                reader.copyReader().filter(Test01::getName, "<=", NULL).size();
            } catch (Exception e) {
                error++;
            }
            if (error != 6) {
                throw new RuntimeException("Select error 10");
            }
            if (reader.copyReader().filter(Test01::getName, "!=", "1-NAME").size() != 8L) {
                throw new RuntimeException("Select error 11");
            }
            if (reader.copyReader().filter(Test01::getName, "==", NULL).size() != 3L) {
                throw new RuntimeException("Select error 12");
            }
            if (reader.copyReader().filter(Test01::getName, "!=", NULL).size() != 6L) {
                throw new RuntimeException("Select error 13");
            }
            if (reader.copyReader().filter(Test01::getName, "not like", "1%").size() != 8L) {
                throw new RuntimeException("Select error 14");
            }
            if (reader.copyReader().filter(Test01::getName, "not in", "1-NAME", "2-NAME").size() != 7L) {
                throw new RuntimeException("Select error 15");
            }
            if (reader.copyReader().filter(Test01::getName, "in", "1-NAME", "2-NAME").size() != 2L) {
                throw new RuntimeException("Select error 16");
            }
            if (reader.copyReader().filter(Test01::getName, "not in", "1-NAME", "2-NAME", NULL).size() != 4L) {
                throw new RuntimeException("Select error 15");
            }
            if (reader.copyReader().filter(Test01::getName, "in", "1-NAME", "2-NAME", NULL).size() != 5L) {
                throw new RuntimeException("Select error 16");
            }
        }
    }

    @Test
    public void select4() {
        try (var pignoo = new BasePignoo(this.dataSource, config)) {
            var writer = pignoo.writer(Test01.class);
            writer.removeAll();
            for (long i = 1L; i <= 9L; i++) {
                Test01 t = new Test01();
                t.setId(i);
                if (i % 3 != 0) {
                    t.setName(i + "-NAME");
                    t.setCode(i + "-CODE");
                }
                t = writer.add(t);
            }
            var reader = pignoo.reader(Test01.class);
            if (reader.size() != 9L) {
                throw new RuntimeException("Select error 1");
            }
            if (reader.copyReader().filter(Test01::getName, "in", Arrays.asList()).size() != 0L) {
                throw new RuntimeException("Select error 2");
            }
            if (reader.copyReader().filter(Test01::getName, "in", Arrays.asList(), new Byte[] {}).size() != 0L) {
                throw new RuntimeException("Select error 3");
            }
            if (reader.copyReader().filter(Test01::getName, "not in", Arrays.asList()).size() != 9L) {
                throw new RuntimeException("Select error 4");
            }
            if (reader.copyReader().filter(Test01::getName, "not in", Arrays.asList(), new byte[] {}).size() != 9L) {
                throw new RuntimeException("Select error 5");
            }

            Object NULL = null;
            if (reader.copyReader().filter(Test01::getName, "not in", NULL).size() != 6L) {
                throw new RuntimeException("Select error 5");
            }
            if (reader.copyReader().filter(Test01::getName, "in", NULL).size() != 3L) {
                throw new RuntimeException("Select error 5");
            }
            List<String> NULL_LIST = null;
            if (reader.copyReader().filter(Test01::getName, "not in", NULL_LIST).size() != 6L) {
                throw new RuntimeException("Select error 5");
            }
            if (reader.copyReader().filter(Test01::getName, "in", NULL_LIST).size() != 3L) {
                throw new RuntimeException("Select error 5");
            }
        }
    }
}
