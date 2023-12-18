package sword.langbook3.android.activities;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.CharacterCompositionEditorActivityDelegate;
import sword.langbook3.android.activities.delegates.CharacterCompositionEditorActivityDelegate.ArgKeys;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CharacterIdBundler;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class CharacterCompositionEditorActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, CharacterId characterId) {
        final Intent intent = activity.newIntent(CharacterCompositionEditorActivity.class);
        CharacterIdBundler.writeAsIntentExtra(intent, ArgKeys.CHARACTER, characterId);
        activity.startActivityForResult(intent, requestCode);
    }

    public CharacterCompositionEditorActivity() {
        super(ActivityExtensionsAdapter::new, new CharacterCompositionEditorActivityDelegate<>());
    }
}
