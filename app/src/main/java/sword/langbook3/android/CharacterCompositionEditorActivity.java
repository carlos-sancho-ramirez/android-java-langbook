package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CharacterIdBundler;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.models.CharacterCompositionEditorModel;
import sword.langbook3.android.models.CharacterCompositionPart;
import sword.langbook3.android.models.CharacterCompositionRepresentation;

import static sword.langbook3.android.models.CharacterCompositionRepresentation.INVALID_CHARACTER;
import static sword.langbook3.android.models.CharacterDetailsModel.UNKNOWN_COMPOSITION_TYPE;

public final class CharacterCompositionEditorActivity extends Activity implements View.OnClickListener {

    private interface ArgKeys {
        String CHARACTER = BundleKeys.CHARACTER;
    }

    public static void open(Activity activity, int requestCode, CharacterId characterId) {
        final Intent intent = new Intent(activity, CharacterCompositionEditorActivity.class);
        CharacterIdBundler.writeAsIntentExtra(intent, ArgKeys.CHARACTER, characterId);
        activity.startActivityForResult(intent, requestCode);
    }

    private CharacterId _characterId;
    private CharacterCompositionEditorModel<CharacterId> _model;

    private EditText _firstField;
    private EditText _secondField;
    private EditText _compositionTypeField;

    private static void setPartText(CharacterCompositionPart<CharacterId> part, TextView textView) {
        final String text = (part == null)? null :
                (part.representation.character != INVALID_CHARACTER)? "" + part.representation.character :
                (part.representation.token != null)? "{" + part.representation.token + '}' : null;
        textView.setText(text);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.character_composition_editor_activity);

        _characterId = CharacterIdBundler.readAsIntentExtra(getIntent(), ArgKeys.CHARACTER);
        _model = DbManager.getInstance().getManager().getCharacterCompositionDetails(_characterId);

        if (_model == null) {
            finish();
        }
        else {
            final String representation = CharacterDetailsAdapter.representChar(_model.representation);
            setTitle(getString(R.string.characterCompositionEditorActivityTitle, representation));

            _firstField = findViewById(R.id.firstField);
            _secondField = findViewById(R.id.secondField);
            _compositionTypeField = findViewById(R.id.compositionTypeField);
            findViewById(R.id.saveButton).setOnClickListener(this);

            setPartText(_model.first, _firstField);
            setPartText(_model.second, _secondField);

            if (_model.compositionType != UNKNOWN_COMPOSITION_TYPE) {
                _compositionTypeField.setText("" + _model.compositionType);
            }
        }
    }

    private static boolean hasInvalidBraces(String text) {
        final int textLength = text.length();
        final boolean braceAtStart = text.startsWith("{");
        final boolean braceAtEnd = text.endsWith("}");
        final int lastIndexOfStartingBrace = text.lastIndexOf("{");
        final int firstIndexOfEndingBrace = text.indexOf("}");
        return lastIndexOfStartingBrace > 0 || firstIndexOfEndingBrace >= 0 && firstIndexOfEndingBrace < textLength - 1 || braceAtStart ^ braceAtEnd;
    }

    @Override
    public void onClick(View v) {
        final String firstText = _firstField.getText().toString();
        final String secondText = _secondField.getText().toString();
        final String compositionTypeText = _compositionTypeField.getText().toString();

        int compositionType = UNKNOWN_COMPOSITION_TYPE;
        String errorMessage = null;
        if (firstText.isEmpty()) {
            errorMessage = getString(R.string.characterCompositionEditorFirstEmptyError);
        }
        else if (secondText.isEmpty()) {
            errorMessage = getString(R.string.characterCompositionEditorSecondEmptyError);
        }
        else if (compositionTypeText.isEmpty()) {
            errorMessage = getString(R.string.characterCompositionEditorTypeEmptyError);
        }
        else if (hasInvalidBraces(firstText)) {
            errorMessage = getString(R.string.characterCompositionEditorFirstHasBraceError);
        }
        else if (hasInvalidBraces(secondText)) {
            errorMessage = getString(R.string.characterCompositionEditorSecondHasBraceError);
        }
        else if (firstText.length() > 1 && !firstText.startsWith("{")) {
            errorMessage = getString(R.string.characterCompositionEditorFirstMulticharError);
        }
        else if (secondText.length() > 1 && !secondText.startsWith("{")) {
            errorMessage = getString(R.string.characterCompositionEditorSecondMulticharError);
        }
        else {
            try {
                compositionType = Integer.parseInt(compositionTypeText);
            }
            catch (NumberFormatException e) {
                // Nothing to be done
            }

            if (compositionType == UNKNOWN_COMPOSITION_TYPE) {
                errorMessage = getString(R.string.characterCompositionEditorInvalidTypeError);
            }
        }

        if (errorMessage != null) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
        else {
            final CharacterCompositionRepresentation firstRepresentation = (firstText.length() > 1)?
                    new CharacterCompositionRepresentation(INVALID_CHARACTER, firstText.substring(1, firstText.length() - 1)) :
                    new CharacterCompositionRepresentation(firstText.charAt(0), null);

            final CharacterCompositionRepresentation secondRepresentation = (secondText.length() > 1)?
                    new CharacterCompositionRepresentation(INVALID_CHARACTER, secondText.substring(1, secondText.length() - 1)) :
                    new CharacterCompositionRepresentation(secondText.charAt(0), null);

            final LangbookDbManager manager = DbManager.getInstance().getManager();
            manager.updateCharacterComposition(_characterId, firstRepresentation, secondRepresentation, compositionType);
            finish();
        }
    }
}
