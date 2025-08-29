package com.xuesinuo.pignoo.core.implement;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.xuesinuo.pignoo.core.PignooSorter.SMode;
import com.xuesinuo.pignoo.core.SqlExecuter;
import com.xuesinuo.pignoo.core.entity.SqlParam;
import com.xuesinuo.pignoo.core.exception.MapperException;

public class PignooIterator4Mysql<E> implements Iterator<E> {
    /**
     * SQL执行器
     * <p>
     * SQL Executer
     */
    private static final SqlExecuter sqlExecuter = SimpleJdbcSqlExecuter.getInstance();

    private final PignooReader4Mysql<E> reader;
    private final PignooWriter4Mysql<E> writer;
    private final Class<E> c;
    private final boolean isReadOnly;
    private final int step;
    private final long offset;
    private final long limit;
    private final SMode idSortMode;

    private List<E> list;
    private long pageIndex = 0;
    private int stepIndex = -1;
    private long index = 0;
    private E now;

    /**
     * 
     * @param reader
     * @param c
     * @param isReadOnly
     * @param step
     * @param offset
     * @param limit
     * @param idSortMode 不可NULL
     */
    public PignooIterator4Mysql(PignooReader4Mysql<E> reader, Class<E> c, boolean isReadOnly, int step, long offset, long limit, SMode idSortMode) {
        this.reader = reader.copyReader();
        this.writer = reader.copyWriter();
        this.c = c;
        this.isReadOnly = isReadOnly;
        this.step = step;
        this.offset = offset;
        this.limit = limit;
        this.idSortMode = idSortMode;
        try {
            this.nextStep();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new MapperException("Get primary key failed in iterator", e);
        }
    }

    private void nextStep() throws IllegalAccessException, InvocationTargetException {
        this.pageIndex++;
        StringBuilder sql = new StringBuilder("");
        SqlParam sqlParam = new SqlParam();
        sql.append("SELECT ");
        sql.append(this.reader.entityMapper.columns().stream().map(column -> "`" + column + "`").collect(Collectors.joining(",")) + " ");
        sql.append("FROM ");
        sql.append("`" + this.reader.entityMapper.tableName() + "` ");
        String sqlWhere = "";
        if (this.pageIndex > 1) {
            sqlWhere += "`" + this.reader.entityMapper.primaryKeyColumn() + "` ";
            if (this.idSortMode == SMode.MIN_FIRST) {
                sqlWhere += "> ";
            } else {
                sqlWhere += "< ";
            }
            Object lastPk = this.reader.entityMapper.primaryKeyGetter().invoke(list.getLast());
            sqlWhere += sqlParam.next(lastPk) + " ";
        }
        if (this.reader.filter != null) {
            String thisWhere = this.reader.filter2Sql(this.reader.filter, sqlParam);
            if (thisWhere != null && !thisWhere.isBlank()) {
                if (!sqlWhere.isBlank()) {
                    sqlWhere += "AND ";
                }
                sqlWhere += thisWhere;
            }
        }
        if (!sqlWhere.isBlank()) {
            sql.append("WHERE ").append(sqlWhere);
        }
        sql.append("ORDER BY ");
        sql.append("`" + this.reader.entityMapper.primaryKeyColumn() + "` " + this.reader.smodeToSql(this.idSortMode) + " ");
        if (this.pageIndex == 1) {
            sql.append("LIMIT " + this.offset + "," + (this.step + 1) + " ");
        } else {
            sql.append("LIMIT " + this.step + " ");
        }
        this.list = sqlExecuter.selectList(this.reader.connGetter, this.reader.connCloser, sql.toString(), sqlParam.params, this.c);
        this.stepIndex = 0;
    }

    @Override
    public synchronized boolean hasNext() {
        if (list.isEmpty()) {
            return false;
        }
        if (this.pageIndex == 1 && this.stepIndex >= this.list.size()) {
            return false;
        }
        if (this.pageIndex > 1 && this.stepIndex > this.list.size()) {
            return false;
        }
        if (this.index >= this.limit) {
            return false;
        }
        return true;
    }

    @Override
    public synchronized E next() {
        if (this.hasNext() == false) {
            throw new NoSuchElementException("No more data in database");
        }
        this.index++;
        E entity;
        if (this.stepIndex + 1 >= list.size()) {
            entity = list.getLast();
            try {
                this.nextStep();
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new MapperException("Get primary key failed in iterator", e);
            }
        } else {
            entity = list.get(stepIndex);
            this.stepIndex++;
        }
        this.now = entity;
        if (this.isReadOnly == false) {
            if (this.writer.entityProxyFactory != null) {
                entity = this.writer.entityProxyFactory.build(entity);
            }
        }
        return entity;
    }

    @Override
    public void remove() {
        if (this.isReadOnly) {
            throw new UnsupportedOperationException("remove");
        }
        if (this.now == null) {
            throw new IllegalStateException("Iterator got a null value, please call next() first");
        }
        writer.removeById(this.now);
    }
}
