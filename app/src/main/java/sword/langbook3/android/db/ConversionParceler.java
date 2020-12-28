package sword.langbook3.android.db;

import android.os.Parcel;

import sword.collections.ImmutableMap;
import sword.collections.MutableHashMap;
import sword.collections.MutableMap;
import sword.langbook3.android.models.Conversion;

public final class ConversionParceler {

    public static Conversion<AlphabetId> read(Parcel in) {
        final AlphabetId sourceAlphabet = AlphabetIdParceler.read(in);
        final AlphabetId targetAlphabet = AlphabetIdParceler.read(in);

        final int mapSize = in.readInt();
        final MutableMap<String, String> map = MutableHashMap.empty();
        for (int i = 0; i < mapSize; i++) {
            final String source = in.readString();
            final String target = in.readString();
            map.put(source, target);
        }

        return new Conversion<>(sourceAlphabet, targetAlphabet, map);
    }

    public static void write(Parcel out, Conversion<AlphabetId> conversion) {
        AlphabetIdParceler.write(out, conversion.getSourceAlphabet());
        AlphabetIdParceler.write(out, conversion.getTargetAlphabet());

        final ImmutableMap<String, String> map = conversion.getMap();
        final int mapSize = map.size();
        out.writeInt(mapSize);

        for (int i = 0; i < mapSize; i++) {
            out.writeString(map.keyAt(i));
            out.writeString(map.valueAt(i));
        }
    }

    private ConversionParceler() {
    }
}
