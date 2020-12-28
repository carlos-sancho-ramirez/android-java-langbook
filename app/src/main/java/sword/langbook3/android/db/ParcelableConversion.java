package sword.langbook3.android.db;

import android.os.Parcel;
import android.os.Parcelable;

import sword.langbook3.android.models.Conversion;

public final class ParcelableConversion implements Parcelable {

    private final Conversion<AlphabetId> _conversion;

    public ParcelableConversion(Conversion<AlphabetId> conversion) {
        if (conversion == null) {
            throw new IllegalArgumentException();
        }

        _conversion = conversion;
    }

    public Conversion<AlphabetId> get() {
        return _conversion;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ConversionParceler.write(dest, _conversion);
    }

    public static final Creator<ParcelableConversion> CREATOR = new Creator<ParcelableConversion>() {
        @Override
        public ParcelableConversion createFromParcel(Parcel in) {
            return new ParcelableConversion(ConversionParceler.read(in));
        }

        @Override
        public ParcelableConversion[] newArray(int size) {
            return new ParcelableConversion[size];
        }
    };
}
