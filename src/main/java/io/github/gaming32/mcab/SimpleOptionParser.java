package io.github.gaming32.mcab;

import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SimpleOptionParser {
    @FunctionalInterface
    public static interface Converter<V, R> {
        public R convert(V v);
    }

    protected final String[] args;

    public SimpleOptionParser(String[] args) {
        this.args = args;
    }

    protected int indexFromName(String name, int end) {
        name = "-" + name;
        for (int i = 0; i < end; i++) {
            if (args[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    protected int indexFromName(String name) {
        return indexFromName(name, args.length);
    }

    public boolean isPresent(String name) {
        return indexFromName(name) != -1;
    }

    public String getOpt(String name, String orDefault) {
        int i = indexFromName(name, args.length - 1);
        return i != -1 ? args[i + 1] : orDefault;
    }

    public String getOpt(String name) {
        return getOpt(name, (String)null);
    }

    public <T> T getOpt(String name, Converter<String, T> converter) {
        return converter.convert(getOpt(name));
    }

    public <T> T getOpt(String name, T orDefault, Converter<String, T> converter) {
        String value = getOpt(name);
        return value == null ? orDefault : converter.convert(value);
    }

    public <T> T getOpt(String name, String orDefault, Converter<String, T> converter) {
        return converter.convert(getOpt(name, orDefault));
    }
}
