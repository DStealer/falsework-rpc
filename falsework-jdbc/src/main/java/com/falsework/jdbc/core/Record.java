package com.falsework.jdbc.core;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 查询结果集
 */
public class Record {
    private final Object[] data;
    private final Map<String, Integer> nameIndex;

    public Record(Object[] data, Map<String, Integer> nameIndex) {
        this.data = data;
        this.nameIndex = nameIndex;
    }

    /**
     * 获取原始数据
     *
     * @return
     */
    public Object[] getRaws() {
        return this.data;
    }

    /**
     * 根据索引查询
     *
     * @param index
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(int index) {
        if (index >= 0 && index <= this.data.length) {
            return (T) this.data[index];
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * 根据索引查询
     *
     * @param index
     * @param clazz
     * @param <T>
     * @return
     */
    @SuppressWarnings({"unchecked", "unused"})
    public <T> T get(int index, Class<T> clazz) {
        if (index >= 0 && index <= this.data.length) {
            return (T) this.data[index];
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * 命名查询
     *
     * @param name
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String name) {
        if (this.nameIndex.containsKey(name)) {
            return (T) this.data[this.nameIndex.get(name)];
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * 命名查询
     *
     * @param name
     * @param clazz
     * @param <T>
     * @return
     */
    @SuppressWarnings({"unchecked", "unused"})
    public <T> T get(String name, Class<T> clazz) {
        if (this.nameIndex.containsKey(name)) {
            return (T) this.data[this.nameIndex.get(name)];
        } else {
            throw new NoSuchElementException();
        }
    }
}
