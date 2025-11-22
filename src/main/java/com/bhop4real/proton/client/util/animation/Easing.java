package com.bhop4real.proton.client.util.animation;

import java.util.function.DoubleUnaryOperator;

/**
 * Collection of easing curves used by UI animations.
 */
public enum Easing
{
    LINEAR(t -> t),
    EASE_OUT_QUAD(t -> 1 - Math.pow(1 - t, 2)),
    EASE_OUT_CUBIC(t -> 1 - Math.pow(1 - t, 3)),
    EASE_OUT_QUART(t -> 1 - Math.pow(1 - t, 4)),
    EASE_OUT_QUINT(t -> 1 - Math.pow(1 - t, 5)),
    EASE_OUT_EXPO(t -> t == 1 ? 1 : 1 - Math.pow(2, -10 * t)),
    EASE_OUT_CIRC(t -> Math.sqrt(1 - Math.pow(t - 1, 2))),
    EASE_IN_QUAD(t -> t * t),
    EASE_IN_CUBIC(t -> t * t * t),
    EASE_IN_QUINT(t -> t * t * t * t * t);

    private final DoubleUnaryOperator function;

    Easing(DoubleUnaryOperator function)
    {
        this.function = function;
    }

    public DoubleUnaryOperator getFunction()
    {
        return function;
    }
}

