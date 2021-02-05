package sword.langbook3.android.db;

import android.content.Intent;
import android.os.Bundle;

public final class AcceptationIdBundler {

    public static AcceptationId readAsIntentExtra(Intent intent, String key) {
        final int idKey = intent.getIntExtra(key, 0);
        return (idKey != 0)? new AcceptationId(idKey) : null;
    }

    public static void writeAsIntentExtra(Intent intent, String key, AcceptationId acceptationId) {
        if (acceptationId != null) {
            intent.putExtra(key, acceptationId.key);
        }
    }

    public static AcceptationId read(Bundle bundle, String key) {
        final int idKey = bundle.getInt(key, 0);
        return (idKey != 0)? new AcceptationId(idKey) : null;
    }

    public static void write(Bundle bundle, String key, AcceptationId acceptationId) {
        if (acceptationId != null) {
            bundle.putInt(key, acceptationId.key);
        }
    }

    private AcceptationIdBundler() {
    }
}
