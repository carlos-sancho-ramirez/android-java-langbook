package sword.langbook3.android;

import android.util.SparseArray;
import android.util.SparseIntArray;

public final class EqualUtils {

    private EqualUtils() {
    }

    public static boolean equal(Object a, Object b) {
        return a == b || a != null && a.equals(b);
    }

    public static boolean checkEqual(SparseIntArray a, SparseIntArray b) {
        if (a == b) {
            return true;
        }

        if (b == null || a == null) {
            return false;
        }

        final int length = a.size();
        if (b.size() != length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (a.keyAt(i) != b.keyAt(i) || a.valueAt(i) != b.valueAt(i)) {
                return false;
            }
        }

        return true;
    }

    public static <T> boolean checkEqual(SparseArray<T> a, SparseArray<T> b) {
        if (a == b) {
            return true;
        }

        if (b == null || a == null) {
            return false;
        }

        final int length = a.size();
        if (b.size() != length) {
            return false;
        }

        for (int i = 0; i < length; i++) {
            if (a.keyAt(i) != b.keyAt(i) || !equal(a.valueAt(i), b.valueAt(i))) {
                return false;
            }
        }

        return true;
    }
}
