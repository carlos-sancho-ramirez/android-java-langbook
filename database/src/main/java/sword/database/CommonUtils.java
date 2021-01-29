package sword.database;

public final class CommonUtils {

    private CommonUtils() {
    }

    public static boolean equal(Object a, Object b) {
        return a == b || a != null && a.equals(b);
    }
}
