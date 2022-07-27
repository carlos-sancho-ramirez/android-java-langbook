package sword.langbook3.android.db;

import android.content.Intent;
import android.os.Bundle;

public final class BunchIdBundler {

    public static BunchId readAsIntentExtra(Intent intent, String key) {
        final int idKey = intent.getIntExtra(key, 0);
        return (idKey != 0)? new BunchId(idKey) : null;
    }

    public static void writeAsIntentExtra(Intent intent, String key, BunchId bunchId) {
        if (bunchId != null) {
            intent.putExtra(key, bunchId.key);
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
