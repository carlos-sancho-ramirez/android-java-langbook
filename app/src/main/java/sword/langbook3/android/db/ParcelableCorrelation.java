package sword.langbook3.android.db;

import android.os.Parcel;
import android.os.Parcelable;

public final class ParcelableCorrelation implements Parcelable {

    private final ImmutableCorrelation<AlphabetId> _correlation;

    public ParcelableCorrelation(ImmutableCorrelation<AlphabetId> correlation) {
        _correlation = correlation;
    }

    public static final Creator<ParcelableCorrelation> CREATOR = new Creator<ParcelableCorrelation>() {
        @Override
        public ParcelableCorrelation createFromParcel(Parcel in) {
            final Correlation<AlphabetId> correlation = CorrelationParceler.read(in);
            return new ParcelableCorrelation((correlation != null)? correlation.toImmutable() : null);
        }

        @Override
        public ParcelableCorrelation[] newArray(int size) {
            return new ParcelableCorrelation[size];
        }
    };

    public ImmutableCorrelation<AlphabetId> get() {
        return _correlation;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        CorrelationParceler.write(parcel, _correlation);
    }
}
