package sword.langbook3.android.db;

import android.os.Parcel;

public final class SentenceIdParceler {

    public static SentenceId read(Parcel in) {
        final int raw = in.readInt();
        return (raw != 0)? new SentenceId(raw) : null;
    }

    public static void write(Parcel out, SentenceId id) {
        out.writeInt((id != null)? id.key : 0);
    }

    private SentenceIdParceler() {
    }
}
