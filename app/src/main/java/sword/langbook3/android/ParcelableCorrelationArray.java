package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.List;
import sword.langbook3.android.db.AlphabetId;

public final class ParcelableCorrelationArray implements Parcelable {

    private final ImmutableList<ImmutableMap<AlphabetId, String>> _array;

    public ParcelableCorrelationArray(List<ImmutableMap<AlphabetId, String>> array) {
        if (array == null) {
            throw new IllegalArgumentException();
        }

        _array = array.toImmutable();
    }

    public ImmutableList<ImmutableMap<AlphabetId, String>> get() {
        return _array;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    static ImmutableList<ImmutableMap<AlphabetId, String>> read(Parcel in) {
        final int arraySize = in.readInt();
        final ImmutableList<ImmutableMap<AlphabetId, String>> array;
        if (arraySize == 0) {
            array = ImmutableList.empty();
        }
        else {
            final ImmutableIntRange range = new ImmutableIntRange(0, arraySize - 1);
            array = range.map(index -> ParcelableCorrelation.read(in));
        }

        return array.toImmutable();
    }

    static void write(Parcel dest, List<ImmutableMap<AlphabetId, String>> array) {
        final int arraySize = array.size();
        dest.writeInt(arraySize);

        for (int i = 0; i < arraySize; i++) {
            final ImmutableMap<AlphabetId, String> correlation = array.valueAt(i);
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
