package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.collections.ImmutableMap;
import sword.collections.MutableHashMap;
import sword.collections.MutableMap;
import sword.langbook3.android.db.Conversion;

public final class ParcelableConversion implements Parcelable {

    private final Conversion _conversion;

    public ParcelableConversion(Conversion conversion) {
        if (conversion == null) {
            throw new IllegalArgumentException();
        }

        _conversion = conversion;
    }

    public Conversion get() {
        return _conversion;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private static Conversion read(Parcel in) {
        final int sourceAlphabet = in.readInt();
        final int targetAlphabet = in.readInt();

        final int mapSize = in.readInt();
        final MutableMap<String, String> map = MutableHashMap.empty();
        for (int i = 0; i < mapSize; i++) {
            final String source = in.readString();
            final String target = in.readString();
            map.put(source, target);
        }

        return new Conversion(sourceAlphabet, targetAlphabet, map);
    }

    private static void write(Parcel dest, Conversion conversion) {
        dest.writeInt(conversion.getSourceAlphabet());
        dest.writeInt(conversion.getTargetAlphabet());

        final ImmutableMap<String, String> map = conversion.getMap();
        final int mapSize = map.size();
        dest.writeInt(mapSize);

        for (int i = 0; i < mapSize; i++) {
            dest.writeString(map.keyAt(i));
            dest.writeString(map.valueAt(i));
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        write(dest, _conversion);
    }

    public static final Creator<ParcelableConversion> CREATOR = new Creator<ParcelableConversion>() {
        @Override
        public ParcelableConversion createFromParcel(Parcel in) {
            return new ParcelableConversion(read(in));
        }

        @Override
        public ParcelableConversion[] newArray(int size) {
            return new ParcelableConversion[size];
        }
    };
}
