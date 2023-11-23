package sword.langbook3.android.controllers;

import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.app.Activity;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.activities.delegates.CharacterCompositionDefinitionEditorActivityDelegate;
import sword.langbook3.android.collections.Procedure2;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CharacterCompositionTypeId;
import sword.langbook3.android.db.CharacterCompositionTypeIdParceler;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.models.CharacterCompositionDefinitionArea;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;

public final class AddCharacterCompositionDefinitionWithSelectedAcceptationCharacterCompositionDefinitionEditorController implements CharacterCompositionDefinitionEditorActivityDelegate.Controller {

    @NonNull
    private final CharacterCompositionTypeId _id;

    public AddCharacterCompositionDefinitionWithSelectedAcceptationCharacterCompositionDefinitionEditorController(@NonNull CharacterCompositionTypeId id) {
        ensureNonNull(id);
        _id = id;
    }

    @Override
    public void load(@NonNull ActivityInterface activity, @NonNull Procedure2<String, CharacterCompositionDefinitionRegister> procedure) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final String title = checker.readConceptText(_id.getConceptId(), preferredAlphabet);

        CharacterCompositionDefinitionRegister register = checker.getCharacterCompositionDefinition(_id);
        if (register == null) {
            final CharacterCompositionDefinitionArea defaultArea = new CharacterCompositionDefinitionArea(0, 0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT);
            register = new CharacterCompositionDefinitionRegister(defaultArea, defaultArea);
        }

        procedure.apply(title, register);
    }

    @Override
    public void save(@NonNull ActivityExtensions activity, @NonNull CharacterCompositionDefinitionRegister register) {
        if (DbManager.getInstance().getManager().updateCharacterCompositionDefinition(_id, register)) {
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
        }
        else {
            activity.showToast(R.string.createCharacterCompositionDefinitionError);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CharacterCompositionTypeIdParceler.write(dest, _id);
    }

    public static final Creator<AddCharacterCompositionDefinitionWithSelectedAcceptationCharacterCompositionDefinitionEditorController> CREATOR = new Creator<AddCharacterCompositionDefinitionWithSelectedAcceptationCharacterCompositionDefinitionEditorController>() {

        @Override
        public AddCharacterCompositionDefinitionWithSelectedAcceptationCharacterCompositionDefinitionEditorController createFromParcel(Parcel source) {
            final CharacterCompositionTypeId id = CharacterCompositionTypeIdParceler.read(source);
            return new AddCharacterCompositionDefinitionWithSelectedAcceptationCharacterCompositionDefinitionEditorController(id);
        }

        @Override
        public AddCharacterCompositionDefinitionWithSelectedAcceptationCharacterCompositionDefinitionEditorController[] newArray(int size) {
            return new AddCharacterCompositionDefinitionWithSelectedAcceptationCharacterCompositionDefinitionEditorController[size];
        }
    };
}
