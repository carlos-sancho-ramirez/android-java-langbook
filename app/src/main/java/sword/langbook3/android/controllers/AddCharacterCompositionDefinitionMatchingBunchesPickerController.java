package sword.langbook3.android.controllers;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.collections.Set;
import sword.langbook3.android.activities.delegates.MatchingBunchesPickerActivityDelegate;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

public final class AddCharacterCompositionDefinitionMatchingBunchesPickerController extends AbstractMatchingBunchesPickerController {

    @NonNull
    private final ImmutableCorrelation<AlphabetId> _correlation;

    public AddCharacterCompositionDefinitionMatchingBunchesPickerController(
            @NonNull ImmutableCorrelation<AlphabetId> correlation,
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        super(correlationArray);
        ensureNonNull(correlation);
        _correlation = correlation;
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull Set<BunchId> selectedBunches) {
        presenter.openCharacterCompositionDefinitionEditor(requestCode, new AddCharacterCompositionDefinitionWithNewAcceptationCharacterCompositionDefinitionEditorController(_correlation, _correlationArray, selectedBunches.toImmutable()));
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == MatchingBunchesPickerActivityDelegate.REQUEST_CODE_NEXT_STEP && resultCode == RESULT_OK) {
            activity.setResult(RESULT_OK, data);
            activity.finish();
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CorrelationParceler.write(dest, _correlation);
        CorrelationArrayParceler.write(dest, _correlationArray);
    }

    public static final Creator<AddCharacterCompositionDefinitionMatchingBunchesPickerController> CREATOR = new Creator<AddCharacterCompositionDefinitionMatchingBunchesPickerController>() {

        @Override
        public AddCharacterCompositionDefinitionMatchingBunchesPickerController createFromParcel(Parcel source) {
            final ImmutableCorrelation<AlphabetId> correlation = CorrelationParceler.read(source).toImmutable();
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            return new AddCharacterCompositionDefinitionMatchingBunchesPickerController(correlation, correlationArray);
        }

        @Override
        public AddCharacterCompositionDefinitionMatchingBunchesPickerController[] newArray(int size) {
            return new AddCharacterCompositionDefinitionMatchingBunchesPickerController[size];
        }
    };
}
