package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.IntKeyMap;
import sword.collections.MutableIntKeyMap;

public final class ParcelableCorrelation implements Parcelable {

    private final ImmutableIntKeyMap<String> _correlation;

    public ParcelableCorrelation(IntKeyMap<String> correlation) {
        if (correlation == null) {
            throw new IllegalArgumentException();
        }

        _correlation = correlation.toImmutable();
    }

    public ImmutableIntKeyMap<String> get() {
        return _correlation;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    static ImmutableIntKeyMap<String> read(Parcel in) {
        final int correlationSize = in.readInt();
        MutableIntKeyMap<String> correlation = MutableIntKeyMap.empty();
        for (int j = 0; j < correlationSize; j++) {
            final int key = in.readInt();
            final String value = in.readString();
            correlation.put(key, value);
        }

        return correlation.toImmutable();
    }

    static void write(Parcel dest, IntKeyMap<String> correlation) {
        final int correlationSize = correlation.size();
        dest.writeInt(correlationSize);

        for (int j = 0; j < correlationSize; j++) {
            dest.writeInt(correlation.keyAt(j));
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
