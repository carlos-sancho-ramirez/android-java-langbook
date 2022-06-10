package sword.langbook3.android.db;

import android.os.Parcel;

public final class CharacterCompositionTypeIdParceler {

    public static CharacterCompositionTypeId read(Parcel in) {
        final int raw = in.readInt();
        return (raw != 0)? new CharacterCompositionTypeId(raw) : null;
    }

    public static void write(Parcel out, CharacterCompositionTypeId id) {
        final int raw = (id != null)? id.key : 0;
        out.writeInt(raw);
    }

    private CharacterCompositionTypeIdParceler() {
    }
}
