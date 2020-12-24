package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;

public final class ParcelableCorrelationArray implements Parcelable {

    private final ImmutableCorrelationArray _array;

    public ParcelableCorrelationArray(ImmutableCorrelationArray array) {
        if (array == null) {
            throw new IllegalArgumentException();
        }

        _array = array;
    }

    public ImmutableCorrelationArray get() {
        return _array;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    static ImmutableCorrelationArray read(Parcel in) {
        final int arraySize = in.readInt();
        final ImmutableCorrelationArray array;
        if (arraySize == 0) {
            array = ImmutableCorrelationArray.empty();
        }
        else {
            final ImmutableCorrelationArray.Builder builder = new ImmutableCorrelationArray.Builder();
            for (int i = 0; i < arraySize; i++) {
                builder.append(ParcelableCorrelation.read(in));
            }
            array = builder.build();
        }

        return array;
    }

    static void write(Parcel dest, ImmutableCorrelationArray array) {
        final int arraySize = array.size();
        dest.writeInt(arraySize);

        for (int i = 0; i < arraySize; i++) {
            final ImmutableCorrelation correlation = array.valueAt(i);
            ParcelableCorrelation.write(dest, correlation);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        write(dest, _array);
    }

    public static final Creator<ParcelableCorrelationArray> CREATOR = new Creator<ParcelableCorrelationArray>() {
        @Override
        public ParcelableCorrelationArray createFromParcel(Parcel in) {
            return new ParcelableCorrelationArray(read(in));
        }

        @Override
        public ParcelableCorrelationArray[] newArray(int size) {
            return new ParcelableCorrelationArray[size];
        }
    };
}
