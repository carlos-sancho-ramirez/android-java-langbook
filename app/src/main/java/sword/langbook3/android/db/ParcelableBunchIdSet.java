package sword.langbook3.android.db;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.ImmutableSet;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class ParcelableBunchIdSet implements Parcelable {

    @NonNull
    private final ImmutableSet<BunchId> _set;

    public ParcelableBunchIdSet(@NonNull ImmutableSet<BunchId> set) {
        ensureNonNull(set);
        _set = set;
    }

    @NonNull
    public ImmutableSet<BunchId> get() {
        return _set;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        BunchIdSetParceler.write(dest, _set);
    }

    public static final Creator<ParcelableBunchIdSet> CREATOR = new Creator<ParcelableBunchIdSet>() {
        @Override
        public ParcelableBunchIdSet createFromParcel(Parcel in) {
            return new ParcelableBunchIdSet(BunchIdSetParceler.read(in));
        }

        @Override
        public ParcelableBunchIdSet[] newArray(int size) {
            return new ParcelableBunchIdSet[size];
        }
    };
}
