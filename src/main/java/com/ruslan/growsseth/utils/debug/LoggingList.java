package com.ruslan.growsseth.utils.debug;

import com.ruslan.growsseth.RuinsOfGrowsseth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;

public class LoggingList<T> implements List<T> {
    private List<T> list;
    private Supplier<Boolean> condition;

    public LoggingList(List<T> list, Supplier<Boolean> logCondition) {
        this.list = list;
        this.condition = logCondition;
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return list.iterator();
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray() {
        return list.toArray();
    }

    @NotNull
    @Override
    public <T1> T1 @NotNull [] toArray(@NotNull T1[] a) {
        return list.toArray(a);
    }

    @Override
    public boolean add(T t) {
        log("add", t, null);
        return list.add(t);
    }

    @Override
    public boolean remove(Object o) {
        log("remove", o, null);
        return list.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        log("addAll", c, null);
        return list.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends T> c) {
        log("addAll", c, index);
        return list.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        log("removeAll", c, null);
        return list.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        log("retainAll", c, null);
        return list.retainAll(c);
    }

    @Override
    public void clear() {
        log("clear", null, null);
        list.clear();
    }

    @Override
    public T get(int index) {
        return list.get(index);
    }

    @Override
    public T set(int index, T element) {
        log("set", element, index);
        return list.set(index, element);
    }

    @Override
    public void add(int index, T element) {
        log("add", element, index);
        list.add(index, element);
    }

    @Override
    public T remove(int index) {
        log("remove", index, null);
        return list.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return list.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator() {
        return list.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(int index) {
        return list.listIterator(index);
    }

    @NotNull
    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }


    private void log(String action, @Nullable Object value, @Nullable Object pos) {
        if (true) {//condition.get()) {
            if (value == null) {
                RuinsOfGrowsseth.getLOGGER().info("DEBUG LIST | {}", action);
            } else if (pos == null) {
                RuinsOfGrowsseth.getLOGGER().info("DEBUG LIST | {} | {}", action, value);
            } else {
                RuinsOfGrowsseth.getLOGGER().info("DEBUG LIST | {} | {} at {}", action, value, pos);
            }
        }
    }
}
