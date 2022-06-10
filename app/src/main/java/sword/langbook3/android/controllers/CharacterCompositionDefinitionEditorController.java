package sword.langbook3.android.controllers;

import android.app.Activity;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import androidx.annotation.NonNull;
import sword.langbook3.android.CharacterCompositionDefinitionEditorActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.collections.Procedure2;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CharacterCompositionTypeId;
import sword.langbook3.android.db.CharacterCompositionTypeIdParceler;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.models.CharacterCompositionDefinitionArea;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;

import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class CharacterCompositionDefinitionEditorController implements CharacterCompositionDefinitionEditorActivity.Controller {

    @NonNull
    private final CharacterCompositionTypeId _id;

    public CharacterCompositionDefinitionEditorController(@NonNull CharacterCompositionTypeId id) {
        ensureNonNull(id);
        _id = id;
    }

    @Override
    public void load(@NonNull Activity activity, @NonNull Procedure2<String, CharacterCompositionDefinitionRegister> procedure) {
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
    public void save(@NonNull Activity activity, @NonNull CharacterCompositionDefinitionRegister register) {
        if (DbManager.getInstance().getManager().updateCharacterCompositionDefinition(_id, register)) {
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
        }
        else {
            Toast.makeText(activity, R.string.updateCharacterCompositionDefinitionError, Toast.LENGTH_SHORT).show();
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

    public static final Parcelable.Creator<CharacterCompositionDefinitionEditorController> CREATOR = new Parcelable.Creator<CharacterCompositionDefinitionEditorController>() {

        @Override
        public CharacterCompositionDefinitionEditorController createFromParcel(Parcel source) {
            final CharacterCompositionTypeId id = CharacterCompositionTypeIdParceler.read(source);
            return new CharacterCompositionDefinitionEditorController(id);
        }

        @Override
        public CharacterCompositionDefinitionEditorController[] newArray(int size) {
            return new CharacterCompositionDefinitionEditorController[size];
        }
    };
}
