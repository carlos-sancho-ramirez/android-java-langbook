package sword.langbook3.android.db;

import android.content.Intent;

public final class CorrelationIdBundler {

    public static CorrelationId readAsIntentExtra(Intent intent, String key) {
        return intent.hasExtra(key)? new CorrelationId(intent.getIntExtra(key, 0)) : null;
    }

    public static void writeAsIntentExtra(Intent intent, String key, CorrelationId correlationId) {
        if (correlationId != null) {
            intent.putExtra(key, correlationId.key);
        }
    }

    private CorrelationIdBundler() {
    }
}
