package sword.langbook3.android.db;

import android.os.Parcel;

import sword.collections.ImmutableSet;
import sword.collections.MutableHashSet;
import sword.collections.MutableSet;
import sword.collections.Set;
import sword.langbook3.android.collections.MinimumSizeArrayLengthFunction;

public final class BunchIdSetParceler {

    public static ImmutableSet<BunchId> read(Parcel in) {
        final int count = in.readInt();
        final MutableSet<BunchId> set = MutableHashSet.empty(new MinimumSizeArrayLengthFunction(count));
        for (int i = 0; i < count; i++) {
            set.add(BunchIdParceler.read(in));
        }

        return set.toImmutable();
    }

    public static void write(Parcel out, Set<BunchId> set) {
        out.writeInt(set.size());
        for (BunchId id : set) {
            BunchIdParceler.write(out, id);
        }
    }

    private BunchIdSetParceler() {
    }
}
