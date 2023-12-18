package sword.langbook3.android.activities;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.AcceptationDetailsActivityDelegate.ArgKeys;
import sword.langbook3.android.activities.delegates.AcceptationDetailsActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ContextExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class AcceptationDetailsActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ContextExtensions context, @NonNull AcceptationId acceptation) {
        final Intent intent = context.newIntent(AcceptationDetailsActivity.class);
        AcceptationIdBundler.writeAsIntentExtra(intent, ArgKeys.ACCEPTATION, acceptation);
        context.startActivity(intent);
    }

    public static void open(@NonNull ActivityExtensions activity, int requestCode, @NonNull AcceptationId acceptation) {
        final Intent intent = activity.newIntent(AcceptationDetailsActivity.class);
        AcceptationIdBundler.writeAsIntentExtra(intent, ArgKeys.ACCEPTATION, acceptation);
        activity.startActivityForResult(intent, requestCode);
    }

    public AcceptationDetailsActivity() {
        super(ActivityExtensionsAdapter::new, new AcceptationDetailsActivityDelegate<>());
    }
}
