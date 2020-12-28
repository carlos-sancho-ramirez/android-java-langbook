package sword.langbook3.android.db;

import android.os.Parcel;

public final class CorrelationArrayParceler {

    public static ImmutableCorrelationArray<AlphabetId> read(Parcel in) {
        final int arraySize = in.readInt();
        final ImmutableCorrelationArray<AlphabetId> array;
        if (arraySize == 0) {
            array = ImmutableCorrelationArray.empty();
        }
        else {
            final ImmutableCorrelationArray.Builder<AlphabetId> builder = new ImmutableCorrelationArray.Builder<>();
            for (int i = 0; i < arraySize; i++) {
                final Correlation<AlphabetId> correlation = CorrelationParceler.read(in);
                builder.append((correlation != null)? correlation.toImmutable() : null);
            }
            array = builder.build();
        }

        return array;
    }

    public static void write(Parcel out, ImmutableCorrelationArray<AlphabetId> array) {
        final int arraySize = array.size();
        out.writeInt(arraySize);

        for (int i = 0; i < arraySize; i++) {
            CorrelationParceler.write(out, array.valueAt(i));
        }
    }

    private CorrelationArrayParceler() {
    }
}
