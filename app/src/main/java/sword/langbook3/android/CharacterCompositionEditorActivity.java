package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CharacterIdBundler;
import sword.langbook3.android.models.CharacterCompositionEditorModel;

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
            _firstField = findViewById(R.id.firstField);
            _secondField = findViewById(R.id.secondField);
            _compositionTypeField = findViewById(R.id.compositionTypeField);
            findViewById(R.id.saveButton).setOnClickListener(this);

            if (_model.first != null) {
                _firstField.setText("" + _model.first.character);
            }

            if (_model.second != null) {
                _secondField.setText("" + _model.second.character);
            }

            if (_model.compositionType != UNKNOWN_COMPOSITION_TYPE) {
                _compositionTypeField.setText("" + _model.compositionType);
            }
        }
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
        else if (firstText.contains("{")) {
            errorMessage = getString(R.string.characterCompositionEditorFirstHasBraceError);
        }
        else if (secondText.contains("{")) {
            errorMessage = getString(R.string.characterCompositionEditorSecondHasBraceError);
        }
        else if (firstText.length() > 1) {
            errorMessage = getString(R.string.characterCompositionEditorFirstMulticharError);
        }
        else if (secondText.length() > 1) {
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
            DbManager.getInstance().getManager().updateCharacterComposition(_characterId, firstText.charAt(0), secondText.charAt(0), compositionType);
            finish();
        }
    }
}
