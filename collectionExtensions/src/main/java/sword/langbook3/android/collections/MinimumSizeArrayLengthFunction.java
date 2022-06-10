package sword.langbook3.android.collections;

import sword.collections.ArrayLengthFunction;

/**
 * Ensures that a collection will have at least the given number of positions.
 * If the collections need to grow, it will increase the size in chunk of a certain granularity.
 */
public final class MinimumSizeArrayLengthFunction implements ArrayLengthFunction {

    private static final int GRANULARITY = 4;
    private final int _minimum;

    public MinimumSizeArrayLengthFunction(int minimum) {
        _minimum = minimum;
    }

    @Override
    public int suitableArrayLength(int currentSize, int newSize) {
        if (newSize <= _minimum) {
            return _minimum;
        }

        int s = ((newSize + GRANULARITY - 1) / GRANULARITY) * GRANULARITY;
        return (s > 0)? s : GRANULARITY;
    }
}
