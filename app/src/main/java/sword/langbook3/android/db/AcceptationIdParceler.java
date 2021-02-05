package sword.langbook3.android.db;

import android.os.Parcel;

public final class AcceptationIdParceler {

    public static AcceptationId read(Parcel in) {
        final int rawAcceptation = in.readInt();
        return (rawAcceptation != 0)? new AcceptationId(rawAcceptation) : null;
    }

    public static void write(Parcel out, AcceptationId id) {
        final int rawAcceptation = (id != null)? id.key : 0;
        out.writeInt(rawAcceptation);
    }

    private AcceptationIdParceler() {
    }
}
