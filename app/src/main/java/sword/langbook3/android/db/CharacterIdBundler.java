package sword.langbook3.android.db;

import android.content.Intent;
import android.os.Bundle;

public final class CharacterIdBundler {

    public static CharacterId readAsIntentExtra(Intent intent, String key) {
        final int idKey = intent.getIntExtra(key, 0);
        return (idKey != 0)? new CharacterId(idKey) : null;
    }

    public static void writeAsIntentExtra(Intent intent, String key, CharacterId characterId) {
        if (characterId != null) {
            intent.putExtra(key, characterId.key);
        }
    }

    public static CharacterId read(Bundle bundle, String key) {
        final int idKey = bundle.getInt(key, 0);
        return (idKey != 0)? new CharacterId(idKey) : null;
    }

    public static void write(Bundle bundle, String key, CharacterId characterId) {
        if (characterId != null) {
            bundle.putInt(key, characterId.key);
        }
    }

    private CharacterIdBundler() {
    }
}
