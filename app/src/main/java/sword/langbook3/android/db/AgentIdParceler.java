package sword.langbook3.android.db;

import android.os.Parcel;

public final class AgentIdParceler {

    public static AgentId read(Parcel in) {
        final int raw = in.readInt();
        return (raw != 0)? new AgentId(raw) : null;
    }

    public static void write(Parcel out, AgentId id) {
        final int raw = (id != null)? id.key : 0;
        out.writeInt(raw);
    }

    private AgentIdParceler() {
    }
}
