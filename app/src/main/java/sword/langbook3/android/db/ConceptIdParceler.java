package sword.langbook3.android.db;

import android.os.Parcel;

public final class ConceptIdParceler {

    public static ConceptId read(Parcel in) {
        final int rawConcept = in.readInt();
        return (rawConcept != 0)? new ConceptId(rawConcept) : null;
    }

    public static void write(Parcel out, ConceptId id) {
        final int rawConcept = (id != null)? id.key : 0;
        out.writeInt(rawConcept);
    }

    private ConceptIdParceler() {
    }
}
