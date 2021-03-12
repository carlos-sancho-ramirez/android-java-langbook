package sword.langbook3.android.db;

import android.os.Parcel;

public final class BunchIdParceler {

    public static BunchId read(Parcel in) {
        final int rawBunch = in.readInt();
        return (rawBunch != 0)? new BunchId(rawBunch) : null;
    }

    public static void write(Parcel out, BunchId id) {
        final int rawBunch = (id != null)? id.key : 0;
        out.writeInt(rawBunch);
    }

    private BunchIdParceler() {
    }
}
