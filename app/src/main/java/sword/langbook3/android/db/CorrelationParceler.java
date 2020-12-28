package sword.langbook3.android.db;

import android.os.Parcel;

public final class CorrelationParceler {

    public static Correlation<AlphabetId> read(Parcel in) {
        final int size = in.readInt();
        final Correlation<AlphabetId> result;
        if (size == -1) {
            result = null;
        }
        else {
            final MutableCorrelation<AlphabetId> correlation = MutableCorrelation.empty();
            for (int i = 0; i < size; i++) {
                final AlphabetId alphabetId = AlphabetIdParceler.read(in);
                correlation.put(alphabetId, in.readString());
            }

            result = correlation;
        }

        return result;
    }

    public static void write(Parcel out, Correlation<AlphabetId> correlation) {
        final int size = (correlation != null)? correlation.size() : -1;
        out.writeInt(size);

        if (size > 0) {
            for (int i = 0; i < size; i++) {
                AlphabetIdParceler.write(out, correlation.keyAt(i));
                out.writeString(correlation.valueAt(i));
            }
        }
    }

    private CorrelationParceler() {
    }
}
