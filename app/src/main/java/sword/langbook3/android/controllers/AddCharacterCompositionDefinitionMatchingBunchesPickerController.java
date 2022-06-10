package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableMap;
import sword.collections.Procedure;
import sword.collections.Set;
import sword.langbook3.android.CharacterCompositionDefinitionEditorActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.MatchingBunchesPickerActivity;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddCharacterCompositionDefinitionMatchingBunchesPickerController implements MatchingBunchesPickerActivity.Controller {

    @NonNull
    private final ImmutableCorrelation<AlphabetId> _correlation;

    @NonNull
    private final ImmutableCorrelationArray<AlphabetId> _correlationArray;

    public AddCharacterCompositionDefinitionMatchingBunchesPickerController(
            @NonNull ImmutableCorrelation<AlphabetId> correlation,
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        ensureNonNull(correlation, correlationArray);
        _correlation = correlation;
        _correlationArray = correlationArray;
    }

    void fire(@NonNull Activity activity, int requestCode) {
        // TODO: This can be optimised as no texts are required
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlation, preferredAlphabet);

        if (bunches.isEmpty()) {
            complete(activity, requestCode, bunches.keySet());
        }
        else {
            MatchingBunchesPickerActivity.open(activity, requestCode, this);
        }
    }

    @Override
    public void loadBunches(@NonNull Activity activity, @NonNull Procedure<ImmutableMap<BunchId, String>> procedure) {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlation, preferredAlphabet);

        if (bunches.isEmpty()) {
            complete(activity, ImmutableHashSet.empty());
        }
        else {
            procedure.apply(bunches);
        }
    }

    private void complete(@NonNull Activity activity, int requestCode, @NonNull Set<BunchId> selectedBunches) {
        CharacterCompositionDefinitionEditorActivity.open(activity, requestCode, new AddCharacterCompositionDefinitionWithNewAcceptationCharacterCompositionDefinitionEditorController(_correlation, _correlationArray, selectedBunches.toImmutable()));
    }

    @Override
    public void complete(@NonNull Activity activity, @NonNull Set<BunchId> selectedBunches) {
        complete(activity, MatchingBunchesPickerActivity.REQUEST_CODE_NEXT_STEP, selectedBunches);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == MatchingBunchesPickerActivity.REQUEST_CODE_NEXT_STEP && resultCode == RESULT_OK) {
            activity.setResult(RESULT_OK, data);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
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