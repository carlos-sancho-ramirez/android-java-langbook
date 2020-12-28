package sword.langbook3.android.db;

import android.os.Parcel;

public final class AlphabetIdParceler {

    public static AlphabetId read(Parcel in) {
        final int rawAlphabet = in.readInt();
        return (rawAlphabet != 0)? new AlphabetId(rawAlphabet) : null;
    }

    public static void write(Parcel out, AlphabetId id) {
        final int rawAlphabet = (id != null)? id.key : 0;
        out.writeInt(rawAlphabet);
    }

    private AlphabetIdParceler() {
    }
}
