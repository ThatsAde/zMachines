package me.petulikan1.zMachines.utils;

public record Pair<A, B>(A key, B value) {
    public A getKey() { return key; }
    public B getValue() { return value; }
}
