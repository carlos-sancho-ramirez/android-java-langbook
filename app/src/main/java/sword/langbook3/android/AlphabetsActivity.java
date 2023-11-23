package sword.langbook3.android;

import androidx.annotation.NonNull;
import sword.langbook3.android.activities.delegates.AlphabetsActivityDelegate;
import sword.langbook3.android.interf.ContextExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class AlphabetsActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ContextExtensions context) {
        context.startActivity(context.newIntent(AlphabetsActivity.class));
    }

    public AlphabetsActivity() {
        super(ActivityExtensionsAdapter::new, new AlphabetsActivityDelegate<>());
    }
}
