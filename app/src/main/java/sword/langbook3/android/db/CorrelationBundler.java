package sword.langbook3.android.db;

import android.content.Intent;
import android.os.Bundle;

public final class CorrelationBundler {

    public static Correlation<AlphabetId> readAsIntentExtra(Intent intent, String key) {
        final ParcelableCorrelation parcelable = intent.getParcelableExtra(key);
        return (parcelable != null)? parcelable.get() : null;
    }

    public static void writeAsIntentExtra(Intent intent, String key, Correlation<AlphabetId> correlation) {
        if (correlation != null) {
            intent.putExtra(key, new ParcelableCorrelation(correlation.toImmutable()));
        }
    }

    public static Correlation<AlphabetId> read(Bundle bundle, String key) {
        final ParcelableCorrelation parcelable = bundle.getParcelable(key);
        return (parcelable != null)? parcelable.get() : null;
    }

    public static void write(Bundle bundle, String key, Correlation<AlphabetId> correlation) {
        if (correlation != null) {
            bundle.putParcelable(key, new ParcelableCorrelation((correlation.toImmutable())));
        }
    }

    private CorrelationBundler() {
    }
}
