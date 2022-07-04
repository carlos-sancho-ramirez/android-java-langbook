package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableHashSet;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdParceler;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAgentAgentEditorControllerWithDiff extends AddAgentAbstractAgentEditorController {

    @NonNull
    private final BunchId _initialDiffBunch;

    public AddAgentAgentEditorControllerWithDiff(@NonNull BunchId initialDiffBunch) {
        ensureNonNull(initialDiffBunch);
        _initialDiffBunch = initialDiffBunch;
    }

    @Override
    public void setup(@NonNull MutableState state) {
        state.setDiffBunches(ImmutableHashSet.empty().add(_initialDiffBunch));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        BunchIdParceler.write(dest, _initialDiffBunch);
    }

    public static final Creator<AddAgentAgentEditorControllerWithDiff> CREATOR = new Creator<AddAgentAgentEditorControllerWithDiff>() {

        @Override
        public AddAgentAgentEditorControllerWithDiff createFromParcel(Parcel source) {
            final BunchId initialDiffBunch = BunchIdParceler.read(source);
            return new AddAgentAgentEditorControllerWithDiff(initialDiffBunch);
        }

        @Override
        public AddAgentAgentEditorControllerWithDiff[] newArray(int size) {
            return new AddAgentAgentEditorControllerWithDiff[size];
        }
    };
}
