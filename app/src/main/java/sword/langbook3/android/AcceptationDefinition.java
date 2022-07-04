package sword.langbook3.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import sword.collections.ImmutableSet;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ImmutableCorrelationArray;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AcceptationDefinition {
    @NonNull
    public final ImmutableCorrelationArray<AlphabetId> correlationArray;

    @NonNull
    public final ImmutableSet<BunchId> bunchSet;

    public AcceptationDefinition(
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray,
            @NonNull ImmutableSet<BunchId> bunchSet) {
        ensureNonNull(correlationArray, bunchSet);
        this.correlationArray = correlationArray;
        this.bunchSet = bunchSet;
    }

    @Override
    public int hashCode() {
        return correlationArray.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof AcceptationDefinition)) {
            return false;
        }
        else if (obj == this) {
            return true;
        }

        final AcceptationDefinition that = (AcceptationDefinition) obj;
        return correlationArray.equals(that.correlationArray) && bunchSet.equalSet(that.bunchSet);
    }
}
