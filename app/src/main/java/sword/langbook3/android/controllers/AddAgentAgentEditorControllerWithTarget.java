package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableHashSet;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdParceler;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAgentAgentEditorControllerWithTarget extends AddAgentAbstractAgentEditorController {

    @NonNull
    private final BunchId _initialTargetBunch;

    public AddAgentAgentEditorControllerWithTarget(@NonNull BunchId initialTargetBunch) {
        ensureNonNull(initialTargetBunch);
        _initialTargetBunch = initialTargetBunch;
    }

    @Override
    public void setup(@NonNull MutableState state) {
        state.setTargetBunches(ImmutableHashSet.empty().add(_initialTargetBunch));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        BunchIdParceler.write(dest, _initialTargetBunch);
    }

    public static final Creator<AddAgentAgentEditorControllerWithTarget> CREATOR = new Creator<AddAgentAgentEditorControllerWithTarget>() {

        @Override
        public AddAgentAgentEditorControllerWithTarget createFromParcel(Parcel source) {
            final BunchId initialTargetBunch = BunchIdParceler.read(source);
            return new AddAgentAgentEditorControllerWithTarget(initialTargetBunch);
        }

        @Override
        public AddAgentAgentEditorControllerWithTarget[] newArray(int size) {
            return new AddAgentAgentEditorControllerWithTarget[size];
        }
    };
}
