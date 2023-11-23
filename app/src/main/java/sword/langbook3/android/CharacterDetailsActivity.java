package sword.langbook3.android;

import android.content.Intent;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.CharacterDetailsActivityDelegate;
import sword.langbook3.android.activities.delegates.CharacterDetailsActivityDelegate.ArgKeys;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CharacterIdBundler;
import sword.langbook3.android.interf.ContextExtensions;
import sword.langbook3.android.util.ActivityExtensionsAdapter;

public final class CharacterDetailsActivity extends DelegatorActivity<ActivityExtensionsAdapter> {

    public static void open(@NonNull ContextExtensions context, CharacterId characterId) {
        final Intent intent = context.newIntent(CharacterDetailsActivity.class);
        CharacterIdBundler.writeAsIntentExtra(intent, ArgKeys.CHARACTER, characterId);
        context.startActivity(intent);
    }

    public CharacterDetailsActivity() {
        super(ActivityExtensionsAdapter::new, new CharacterDetailsActivityDelegate<>());
    }
}
