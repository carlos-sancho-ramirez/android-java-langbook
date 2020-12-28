package sword.langbook3.android.db;

import android.os.Parcel;

import sword.collections.List;
import sword.collections.MutableList;

public final class CorrelationEntryListParceler {

    public static void readInto(Parcel in, MutableList<Correlation.Entry<AlphabetId>> list) {
        final int startMatcherLength = in.readInt();
        for (int i = 0; i < startMatcherLength; i++) {
            final AlphabetId alphabetId = AlphabetIdParceler.read(in);
            final String text = in.readString();
            list.append(new Correlation.Entry<>(alphabetId, text));
        }
    }

    public static void write(Parcel out, List<Correlation.Entry<AlphabetId>> list) {
        out.writeInt(list.size());
        for (Correlation.Entry<AlphabetId> entry : list) {
            AlphabetIdParceler.write(out, entry.alphabet);
            out.writeString(entry.text);
        }
    }

    private CorrelationEntryListParceler() {
    }
}
