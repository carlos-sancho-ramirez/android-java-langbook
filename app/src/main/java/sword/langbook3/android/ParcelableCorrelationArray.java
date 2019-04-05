package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.List;
import sword.collections.MutableIntKeyMap;

public final class ParcelableCorrelationArray implements Parcelable {

    private final ImmutableList<ImmutableIntKeyMap<String>> _array;

    public ParcelableCorrelationArray(List<ImmutableIntKeyMap<String>> array) {
        if (array == null) {
            throw new IllegalArgumentException();
        }

        _array = array.toImmutable();
    }

    public ImmutableList<ImmutableIntKeyMap<String>> get() {
        return _array;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    static ImmutableList<ImmutableIntKeyMap<String>> read(Parcel in) {
        final int arraySize = in.readInt();
        final ImmutableList<ImmutableIntKeyMap<String>> array;
        if (arraySize == 0) {
            array = ImmutableList.empty();
        }
        else {
            final ImmutableIntRange range = new ImmutableIntRange(0, arraySize - 1);
            array = range.map(index -> {
                final int correlationSize = in.readInt();
                MutableIntKeyMap<String> correlation = MutableIntKeyMap.empty();
                for (int j = 0; j < correlationSize; j++) {
                    final int key = in.readInt();
                    final String value = in.readString();
                    correlation.put(key, value);
                }

                return correlation.toImmutable();
            });
        }

        return array.toImmutable();
    }

    static void write(Parcel dest, List<ImmutableIntKeyMap<String>> array) {
        final int arraySize = array.size();
        dest.writeInt(arraySize);

        for (int i = 0; i < arraySize; i++) {
            final ImmutableIntKeyMap<String> correlation = array.valueAt(i);
            final int correlationSize = correlation.size();
            dest.writeInt(correlationSize);

            for (int j = 0; j < correlationSize; j++) {
                dest.writeInt(correlation.keyAt(j));
                dest.writeString(correlation.valueAt(j));
            }
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
