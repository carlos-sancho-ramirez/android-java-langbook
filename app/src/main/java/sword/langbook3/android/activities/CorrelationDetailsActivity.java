package sword.langbook3.android.activities;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.CorrelationDetailsActivityDelegate;
import sword.langbook3.android.activities.delegates.CorrelationDetailsActivityDelegate.ArgKeys;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.CorrelationIdBundler;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class CorrelationDetailsActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, CorrelationId correlationId) {
        Intent intent = activity.newIntent(CorrelationDetailsActivity.class);
        CorrelationIdBundler.writeAsIntentExtra(intent, ArgKeys.CORRELATION, correlationId);
        activity.startActivityForResult(intent, requestCode);
    }

    public CorrelationDetailsActivity() {
        super(ActivityExtensionsAdapter::new, new CorrelationDetailsActivityDelegate<>());
    }
}
