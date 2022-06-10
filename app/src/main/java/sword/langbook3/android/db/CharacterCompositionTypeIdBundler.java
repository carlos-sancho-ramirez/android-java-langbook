package sword.langbook3.android.db;

import android.content.Intent;
import android.os.Bundle;

public final class CharacterCompositionTypeIdBundler {

    public static CharacterCompositionTypeId readAsIntentExtra(Intent intent, String key) {
        final int idKey = intent.getIntExtra(key, 0);
        return (idKey != 0)? new CharacterCompositionTypeId(idKey) : null;
    }

    public static void writeAsIntentExtra(Intent intent, String key, CharacterCompositionTypeId typeId) {
        if (typeId != null) {
            intent.putExtra(key, typeId.key);
        }
    }

    public static CharacterCompositionTypeId read(Bundle bundle, String key) {
        final Object value = bundle.get(key);
        return (value instanceof Integer)? new CharacterCompositionTypeId((Integer) value) : null;
    }

    public static void write(Bundle bundle, String key, CharacterCompositionTypeId typeId) {
        if (typeId != null) {
            bundle.putInt(key, typeId.key);
        }
    }

    private CharacterCompositionTypeIdBundler() {
    }
}
