package sword.langbook3.android;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.CharacterPickerActivityDelegate;
import sword.langbook3.android.activities.delegates.CharacterPickerActivityDelegate.ArgKeys;
import sword.langbook3.android.interf.ContextExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class CharacterPickerActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ContextExtensions context, String characters) {
        final Intent intent = context.newIntent(CharacterPickerActivity.class);
        intent.putExtra(ArgKeys.CHARACTER_STRING, characters);
        context.startActivity(intent);
    }

    public CharacterPickerActivity() {
        super(ActivityExtensionsAdapter::new, new CharacterPickerActivityDelegate<>());
    }
}
