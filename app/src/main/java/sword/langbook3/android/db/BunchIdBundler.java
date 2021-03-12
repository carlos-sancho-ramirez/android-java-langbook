package sword.langbook3.android.db;

import android.content.Intent;
import android.os.Bundle;

import sword.collections.List;
import sword.collections.MutableList;

import static sword.langbook3.android.db.BunchIdManager.conceptAsBunchId;

public final class BunchIdBundler {

    public static BunchId readAsIntentExtra(Intent intent, String key) {
        return intent.hasExtra(key)? new BunchId(intent.getIntExtra(key, 0)) : null;
    }

    public static void writeAsIntentExtra(Intent intent, String key, BunchId bunchId) {
        if (bunchId != null) {
            intent.putExtra(key, bunchId.key);
        }
    }

    public static MutableList<BunchId> readListAsIntentExtra(Intent intent, String key) {
        if (!intent.hasExtra(key)) {
            return null;
        }

        final int[] bunchArray = intent.getIntArrayExtra(key);
        final MutableList<BunchId> bunchList = MutableList.empty();
        for (int bunch : bunchArray) {
            bunchList.append(conceptAsBunchId(bunch));
        }

        return bunchList;
    }

    public static void writeListAsIntentExtra(Intent intent, String key, List<BunchId> bunchList) {
        if (bunchList != null) {
            final int size = bunchList.size();
            final int[] bunchArray = new int[size];
            for (int i = 0; i < size; i++) {
                bunchArray[i] = bunchList.valueAt(i).getConceptId();
            }
            intent.putExtra(key, bunchArray);
        }
    }

    public static BunchId read(Bundle bundle, String key) {
        final Object value = bundle.get(key);
        return (value instanceof Integer)? new BunchId((Integer) value) : null;
    }

    public static void write(Bundle bundle, String key, BunchId bunchId) {
        if (bunchId != null) {
            bundle.putInt(key, bunchId.key);
        }
    }

    private BunchIdBundler() {
    }
}
