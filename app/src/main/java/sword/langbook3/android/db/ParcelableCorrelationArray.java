package sword.langbook3.android.db;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class ParcelableCorrelationArray implements Parcelable {

    @NonNull
    private final ImmutableCorrelationArray<AlphabetId> _array;

    public ParcelableCorrelationArray(@NonNull ImmutableCorrelationArray<AlphabetId> array) {
        ensureNonNull(array);
        _array = array;
    }

    @NonNull
    public ImmutableCorrelationArray<AlphabetId> get() {
        return _array;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CorrelationArrayParceler.write(dest, _array);
    }

    public static final Creator<ParcelableCorrelationArray> CREATOR = new Creator<ParcelableCorrelationArray>() {
        @Override
        public ParcelableCorrelationArray createFromParcel(Parcel in) {
            return new ParcelableCorrelationArray(CorrelationArrayParceler.read(in));
        }

        @Override
        public ParcelableCorrelationArray[] newArray(int size) {
            return new ParcelableCorrelationArray[size];
        }
    };
}
