package sword.langbook3.android.activities;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.ConversionDetailsActivityDelegate;
import sword.langbook3.android.activities.delegates.ConversionDetailsActivityDelegate.ArgKeys;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdBundler;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ContextExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class ConversionDetailsActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ContextExtensions context, AlphabetId sourceAlphabet, AlphabetId targetAlphabet) {
        final Intent intent = context.newIntent(ConversionDetailsActivity.class);
        AlphabetIdBundler.writeAsIntentExtra(intent, ArgKeys.SOURCE_ALPHABET, sourceAlphabet);
        AlphabetIdBundler.writeAsIntentExtra(intent, ArgKeys.TARGET_ALPHABET, targetAlphabet);
        context.startActivity(intent);
    }

    public static void open(@NonNull ActivityExtensions activity, int requestCode, AlphabetId sourceAlphabet, AlphabetId targetAlphabet) {
        final Intent intent = activity.newIntent(ConversionDetailsActivity.class);
        AlphabetIdBundler.writeAsIntentExtra(intent, ArgKeys.SOURCE_ALPHABET, sourceAlphabet);
        AlphabetIdBundler.writeAsIntentExtra(intent, ArgKeys.TARGET_ALPHABET, targetAlphabet);
        activity.startActivityForResult(intent, requestCode);
    }

    public ConversionDetailsActivity() {
        super(ActivityExtensionsAdapter::new, new ConversionDetailsActivityDelegate<>());
    }
}
