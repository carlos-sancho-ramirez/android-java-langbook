package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.widget.Toast;

import androidx.annotation.NonNull;
import sword.collections.ImmutableSet;
import sword.collections.MutableList;
import sword.langbook3.android.CharacterCompositionDefinitionEditorActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.collections.Procedure2;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdSetParceler;
import sword.langbook3.android.db.CharacterCompositionTypeId;
import sword.langbook3.android.db.CharacterCompositionTypeIdBundler;
import sword.langbook3.android.db.CharacterCompositionTypeIdManager;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.models.CharacterCompositionDefinitionArea;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;

import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddCharacterCompositionDefinitionWithNewAcceptationCharacterCompositionDefinitionEditorController implements CharacterCompositionDefinitionEditorActivity.Controller {

    @NonNull
    private final ImmutableCorrelation<AlphabetId> _correlation;

    @NonNull
    private final ImmutableCorrelationArray<AlphabetId> _correlationArray;

    @NonNull
    private final ImmutableSet<BunchId> _bunchesWhereMustBeIncluded;

    public AddCharacterCompositionDefinitionWithNewAcceptationCharacterCompositionDefinitionEditorController(
            @NonNull ImmutableCorrelation<AlphabetId> correlation,
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray,
            @NonNull ImmutableSet<BunchId> bunchesWhereMustBeIncluded) {
        ensureNonNull(correlation, correlationArray, bunchesWhereMustBeIncluded);
        _correlation = correlation;
        _correlationArray = correlationArray;
        _bunchesWhereMustBeIncluded = bunchesWhereMustBeIncluded;
    }

    @Override
    public void load(@NonNull Activity activity, @NonNull Procedure2<String, CharacterCompositionDefinitionRegister> procedure) {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        String title = _correlation.get(preferredAlphabet, null);
        if (title == null) {
            title = _correlation.valueAt(0);
        }

        final CharacterCompositionDefinitionArea defaultArea = new CharacterCompositionDefinitionArea(0, 0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT);
        final CharacterCompositionDefinitionRegister register = new CharacterCompositionDefinitionRegister(defaultArea, defaultArea);

        procedure.apply(title, register);
    }

    @Override
    public void save(@NonNull Activity activity, @NonNull CharacterCompositionDefinitionRegister register) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        if (manager.findCharacterCompositionDefinition(register) != null) {
            Toast.makeText(activity, R.string.characterCompositionDefinitionAlreadyExistsError, Toast.LENGTH_SHORT).show();
        }
        else {
            final MutableList<BunchId> bunchList = _bunchesWhereMustBeIncluded.toList().mutate();
            final ConceptId concept = manager.getNextAvailableConceptId();
            final AcceptationId accId = manager.addAcceptation(concept, _correlationArray);
            for (BunchId bunch : bunchList) {
                manager.addAcceptationInBunch(bunch, accId);
            }

            final CharacterCompositionTypeId typeId = CharacterCompositionTypeIdManager.conceptAsCharacterCompositionTypeId(concept);

            if (!DbManager.getInstance().getManager().updateCharacterCompositionDefinition(typeId, register)) {
                throw new AssertionError();
            }

            final Intent intent = new Intent();
            CharacterCompositionTypeIdBundler.writeAsIntentExtra(intent, CharacterCompositionDefinitionEditorActivity.ResultKeys.CHARACTER_COMPOSITION_TYPE_ID, typeId);
            activity.setResult(Activity.RESULT_OK, intent);
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
        BunchIdSetParceler.write(dest, _bunchesWhereMustBeIncluded);
    }

    public static final Creator<AddCharacterCompositionDefinitionWithNewAcceptationCharacterCompositionDefinitionEditorController> CREATOR = new Creator<AddCharacterCompositionDefinitionWithNewAcceptationCharacterCompositionDefinitionEditorController>() {

        @Override
        public AddCharacterCompositionDefinitionWithNewAcceptationCharacterCompositionDefinitionEditorController createFromParcel(Parcel source) {
            final ImmutableCorrelation<AlphabetId> correlation = CorrelationParceler.read(source).toImmutable();
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            final ImmutableSet<BunchId> bunchesWhereMustBeIncluded = BunchIdSetParceler.read(source);
            return new AddCharacterCompositionDefinitionWithNewAcceptationCharacterCompositionDefinitionEditorController(correlation, correlationArray, bunchesWhereMustBeIncluded);
        }

        @Override
        public AddCharacterCompositionDefinitionWithNewAcceptationCharacterCompositionDefinitionEditorController[] newArray(int size) {
            return new AddCharacterCompositionDefinitionWithNewAcceptationCharacterCompositionDefinitionEditorController[size];
        }
    };
}
