package me.petulikan1.zMachines.utils;

public record Triplet<A, B, C>(A a, B b, C c) {
    public A getA() { return a; }
    public B getB() { return b; }
    public C getC() { return c; }
}
