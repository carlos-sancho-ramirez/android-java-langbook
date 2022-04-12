package sword.langbook3.android.util;

public final class PreconditionUtils {

    /**
     * Ensure that some preconditions are met, or throw an IllegalArgumentException if not.
     * <p>
     * This method is expected to be used at the beginning of a method in order to ensure that the
     * given arguments matches some assumed preconditions.
     *
     * This is especially valuable in constructors or setters where some arguments are going to be
     * stored to be used later in order to find any possible error as soon as possible.
     *
     * This method should never be used to check user entered, or server entered data, as it cannot
     * be considered a development error, and a proper feedback should be provided to the user
     * instead of crashing.
     *
     * @param condition If true, nothing happens.
     *                  If false, the app will crash to prevent propagating the error.
     */
    public static void ensureValidArguments(boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Ensure that all references provided as parameters are different from null.
     * <p>
     * If all references are different from null, nothing will happen.
     * If any of the references is null, an IllegalArgumentException will be thrown.
     *
     * @param references References to be checked.
     */
    public static void ensureNonNull(Object... references) {
        for (Object ref : references) {
            if (ref == null) {
                throw new IllegalArgumentException();
            }
        }
    }

    private PreconditionUtils() {
    }
}
