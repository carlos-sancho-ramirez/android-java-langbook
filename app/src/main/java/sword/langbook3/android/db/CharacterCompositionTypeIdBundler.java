package sword.langbook3.android.db;

import android.os.Bundle;

public final class CharacterCompositionTypeIdBundler {

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
