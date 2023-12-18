package sword.langbook3.android.activities;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.CharacterRepresentationUpdaterActivityDelegate;
import sword.langbook3.android.activities.delegates.CharacterRepresentationUpdaterActivityDelegate.ArgKeys;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CharacterIdBundler;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class CharacterRepresentationUpdaterActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ActivityExtensions activity, int requestCode, CharacterId characterId) {
        final Intent intent = activity.newIntent(CharacterRepresentationUpdaterActivity.class);
        CharacterIdBundler.writeAsIntentExtra(intent, ArgKeys.CHARACTER, characterId);
        activity.startActivityForResult(intent, requestCode);
    }

    public CharacterRepresentationUpdaterActivity() {
        super(ActivityExtensionsAdapter::new, new CharacterRepresentationUpdaterActivityDelegate<>());
    }
}
