package sword.langbook3.android.db;

import android.os.Parcel;

public final class LanguageIdParceler {

    public static LanguageId read(Parcel in) {
        final int rawLanguage = in.readInt();
        return (rawLanguage != 0)? new LanguageId(rawLanguage) : null;
    }

    public static void write(Parcel out, LanguageId id) {
        final int rawLanguage = (id != null)? id.key : 0;
        out.writeInt(rawLanguage);
    }

    private LanguageIdParceler() {
    }
}
