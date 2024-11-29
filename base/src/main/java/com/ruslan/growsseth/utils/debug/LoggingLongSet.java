package com.ruslan.growsseth.utils.debug;

import com.ruslan.growsseth.RuinsOfGrowsseth;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

public class LoggingLongSet implements LongSet {
    private LongSet set;
    private Supplier<Boolean> condition;

    public LoggingLongSet(LongSet set, Supplier<Boolean> logCondition) {
        this.set = set;
        this.condition = logCondition;
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public LongIterator iterator() {
        return set.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return set.toArray(a);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return set.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Long> c) {
        log("addAll", c);
        return set.addAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        log("removeAll", c);
        return set.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        log("retainAll", c);
        return set.retainAll(c);
    }

    @Override
    public void clear() {
        log("clear", null);
        set.clear();
    }

    @Override
    public boolean add(long key) {
        log("add", key);
        return set.add(key);
    }

    @Override
    public boolean contains(long key) {
        return set.contains(key);
    }

    @Override
    public long[] toLongArray() {
        return set.toLongArray();
    }

    @Override
    public long[] toArray(long[] a) {
        return set.toArray(a);
    }

    @Override
    public boolean addAll(LongCollection c) {
        log("addAll", c);
        return set.addAll(c);
    }

    @Override
    public boolean containsAll(LongCollection c) {
        log("containsAll", c);
        return set.containsAll(c);
    }

    @Override
    public boolean removeAll(LongCollection c) {
        log("removeAll", c);
        return set.removeAll(c);
    }

    @Override
    public boolean retainAll(LongCollection c) {
        log("retainAll", c);
        return set.retainAll(c);
    }

    @Override
    public boolean remove(long k) {
        log("remove", k);
        return set.remove(k);
    }

    private void log(String action, @Nullable Object value) {
        if (condition.get()) {
            if (value == null) {
                RuinsOfGrowsseth.getLOGGER().info("DEBUG LONG SET | {}", action);
            } else {
                RuinsOfGrowsseth.getLOGGER().info("DEBUG LONG SET | {} | {}", action, value);
            }
        }
    }
}
