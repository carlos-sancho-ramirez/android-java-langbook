package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CharacterIdBundler;

import static sword.langbook3.android.CharacterCompositionEditorActivity.isInvalidRepresentation;

public final class CharacterRepresentationUpdaterActivity extends Activity implements View.OnClickListener, TextWatcher {

    private interface ArgKeys {
        String CHARACTER = BundleKeys.CHARACTER;
    }

    private interface SavedKeys {
        String CONFIRMATION_DIALOG_PRESENT = "cdp";
    }

    interface ResultKeys {
        String MERGED_CHARACTER = "mc";
    }

    public static void open(Activity activity, int requestCode, CharacterId characterId) {
        final Intent intent = new Intent(activity, CharacterRepresentationUpdaterActivity.class);
        CharacterIdBundler.writeAsIntentExtra(intent, ArgKeys.CHARACTER, characterId);
        activity.startActivityForResult(intent, requestCode);
    }

    private CharacterId _characterId;
    private CharacterId _mergingChar;
    private boolean _confirmationDialogPresent;

    private EditText _fieldView;
    private TextView _resultInfoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unicode_assigner_activity);
        findViewById(R.id.assignUnicodeButton).setOnClickListener(this);

        _characterId = CharacterIdBundler.readAsIntentExtra(getIntent(), ArgKeys.CHARACTER);
        final String token = DbManager.getInstance().getManager().getToken(_characterId);

        if (token == null) {
            finish();
        }
        else {
            this.<TextView>findViewById(R.id.tokenInfo).setText(getString(R.string.representationUpdaterTokenInfo, token));
            _fieldView = findViewById(R.id.field);
            _fieldView.addTextChangedListener(this);
            _resultInfoView = findViewById(R.id.resultInfo);
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        _confirmationDialogPresent = savedInstanceState.getBoolean(SavedKeys.CONFIRMATION_DIALOG_PRESENT);
        if (_confirmationDialogPresent) {
            final String text = _fieldView.getText().toString();
            final CharacterId matchingChar = DbManager.getInstance().getManager().findCharacter(text.charAt(0));
            if (matchingChar == null) {
                showAssignConfirmationDialog();
            }
            else {
                showMergeConfirmationDialog();
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Nothing to be done
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Nothing to be done
    }

    @Override
    public void afterTextChanged(Editable s) {
        final String newText = s.toString();

        int textColorRes = R.color.agentStaticTextColor;
        final String resultText;
        if (newText.isEmpty()) {
            resultText = null;
        }
        else if (newText.length() == 1) {
            final char newChar = newText.charAt(0);
            final String unicodeText = Integer.toString(newChar) + " (0x" + Integer.toHexString(newChar) + ")";
            resultText = getString(R.string.representationUpdaterInfoText, unicodeText);
        }
        else if (isInvalidRepresentation(newText)) {
            resultText = getString(R.string.representationUpdaterErrorText);
            textColorRes = R.color.errorTextColor;
        }
        else {
            resultText = getString(R.string.representationUpdaterTokenInfoText, newText);
        }

        _resultInfoView.setText(resultText);
        _resultInfoView.setTextColor(getResources().getColor(textColorRes));
    }

    private void assign() {
        if (DbManager.getInstance().getManager().assignUnicode(_characterId, _fieldView.getText().toString().charAt(0))) {
            Toast.makeText(this, R.string.assignUnicodeFeedback, Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        }
        else {
            Toast.makeText(this, R.string.assignUnicodeError, Toast.LENGTH_SHORT).show();
        }
    }

    private void merge() {
        if (DbManager.getInstance().getManager().mergeCharacters(_mergingChar, _characterId)) {
            Toast.makeText(this, R.string.assignUnicodeFeedback, Toast.LENGTH_SHORT).show();

            final Intent data = new Intent();
            CharacterIdBundler.writeAsIntentExtra(data, ResultKeys.MERGED_CHARACTER, _mergingChar);
            setResult(RESULT_OK, data);
            finish();
        }
        else {
            Toast.makeText(this, R.string.mergeCharactersError, Toast.LENGTH_SHORT).show();
        }
    }

    private void showAssignConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.assignUnicodeConfirmationText)
                .setPositiveButton(R.string.assignUnicodeButtonText, (dialog, which) -> assign())
                .setOnCancelListener(dialog -> _confirmationDialogPresent = false)
                .create().show();
    }

    private void showMergeConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.mergeUnicodeConfirmationText)
                .setPositiveButton(R.string.mergeUnicodeButtonText, (dialog, which) -> merge())
                .setOnCancelListener(dialog -> _confirmationDialogPresent = false)
                .create().show();
    }

    @Override
    public void onClick(View v) {
        final String text = _fieldView.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(this, R.string.representationUpdaterEmptyError, Toast.LENGTH_SHORT).show();
        }
        else if (text.length() == 1) {
            _mergingChar = DbManager.getInstance().getManager().findCharacter(text.charAt(0));
            _confirmationDialogPresent = true;
            if (_mergingChar == null) {
                showAssignConfirmationDialog();
            }
            else {
                showMergeConfirmationDialog();
            }
        }
        else if (isInvalidRepresentation(text)) {
            Toast.makeText(this, R.string.wordEditorWrongTextError, Toast.LENGTH_SHORT).show();
        }
        else {
            if (DbManager.getInstance().getManager().updateToken(_characterId, text.substring(1, text.length() - 1))) {
                Toast.makeText(this, R.string.updateTokenFeedback, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
            else {
                Toast.makeText(this, R.string.updateTokenError, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (_confirmationDialogPresent) {
            outState.putBoolean(SavedKeys.CONFIRMATION_DIALOG_PRESENT, true);
        }
    }
}
