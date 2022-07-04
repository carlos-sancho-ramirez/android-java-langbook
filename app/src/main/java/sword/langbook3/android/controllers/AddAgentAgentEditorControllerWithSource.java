package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableHashSet;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdParceler;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAgentAgentEditorControllerWithSource extends AddAgentAbstractAgentEditorController {

    @NonNull
    private final BunchId _initialSourceBunch;

    public AddAgentAgentEditorControllerWithSource(@NonNull BunchId initialSourceBunch) {
        ensureNonNull(initialSourceBunch);
        _initialSourceBunch = initialSourceBunch;
    }

    @Override
    public void setup(@NonNull MutableState state) {
        state.setSourceBunches(ImmutableHashSet.empty().add(_initialSourceBunch));
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        BunchIdParceler.write(dest, _initialSourceBunch);
    }

    public static final Creator<AddAgentAgentEditorControllerWithSource> CREATOR = new Creator<AddAgentAgentEditorControllerWithSource>() {

        @Override
        public AddAgentAgentEditorControllerWithSource createFromParcel(Parcel source) {
            final BunchId initialSourceBunch = BunchIdParceler.read(source);
            return new AddAgentAgentEditorControllerWithSource(initialSourceBunch);
        }

        @Override
        public AddAgentAgentEditorControllerWithSource[] newArray(int size) {
            return new AddAgentAgentEditorControllerWithSource[size];
        }
    };
}
