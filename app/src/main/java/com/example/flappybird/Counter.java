package com.example.flappybird;

public class Counter {
    private long value = 0L;

    public void increment() {
        ++value;
    }

    public long getValue() {
        final long result = value;
        value = 0L;
        return result;
    }
}

