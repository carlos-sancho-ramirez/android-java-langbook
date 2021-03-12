package sword.langbook3.android.db;

import android.os.Parcel;

public final class RuleIdParceler {

    public static RuleId read(Parcel in) {
        final int rawRule = in.readInt();
        return (rawRule != 0)? new RuleId(rawRule) : null;
    }

    public static void write(Parcel out, RuleId id) {
        final int rawRule = (id != null)? id.key : 0;
        out.writeInt(rawRule);
    }

    private RuleIdParceler() {
    }
}
