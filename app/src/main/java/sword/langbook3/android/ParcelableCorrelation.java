package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.Correlation;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.MutableCorrelation;

public final class ParcelableCorrelation implements Parcelable {

    private final ImmutableCorrelation _correlation;

    public ParcelableCorrelation(Correlation correlation) {
        if (correlation == null) {
            throw new IllegalArgumentException();
        }

        _correlation = correlation.toImmutable();
    }

    public ImmutableCorrelation get() {
        return _correlation;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    static ImmutableCorrelation read(Parcel in) {
        final int correlationSize = in.readInt();
        MutableCorrelation correlation = MutableCorrelation.empty();
        for (int j = 0; j < correlationSize; j++) {
            final int key = in.readInt();
            final AlphabetId alphabet = new AlphabetId(key);
            final String value = in.readString();
            correlation.put(alphabet, value);
        }

        return correlation.toImmutable();
    }

    static void write(Parcel dest, Correlation correlation) {
        final int correlationSize = correlation.size();
        dest.writeInt(correlationSize);

        for (int j = 0; j < correlationSize; j++) {
            dest.writeInt(correlation.keyAt(j).key);
            dest.writeString(correlation.valueAt(j));
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        write(dest, _correlation);
    }

    public static final Creator<ParcelableCorrelation> CREATOR = new Creator<ParcelableCorrelation>() {
        @Override
        public ParcelableCorrelation createFromParcel(Parcel in) {
            return new ParcelableCorrelation(read(in));
        }

        @Override
        public ParcelableCorrelation[] newArray(int size) {
            return new ParcelableCorrelation[size];
        }
    };
}
