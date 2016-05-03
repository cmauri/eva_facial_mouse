package com.crea_si.eviacam.service;

/**
 * Possible directions for a flip operation
 */
public enum FlipDirection {
    NONE(0), VERTICAL(1), HORIZONTAL(2);

    private final int mValue;
    FlipDirection(int v) { mValue= v; }
    public int getValue() { return mValue; }
}
