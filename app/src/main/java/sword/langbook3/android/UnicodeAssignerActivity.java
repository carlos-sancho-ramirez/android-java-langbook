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

import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CharacterIdBundler;

public final class UnicodeAssignerActivity extends Activity implements View.OnClickListener, TextWatcher {

    private interface ArgKeys {
        String CHARACTER = BundleKeys.CHARACTER;
    }

    private interface SavedKeys {
        String CONFIRMATION_DIALOG_PRESENT = "cdp";
    }

    public static void open(Activity activity, int requestCode, CharacterId characterId) {
        final Intent intent = new Intent(activity, UnicodeAssignerActivity.class);
        CharacterIdBundler.writeAsIntentExtra(intent, ArgKeys.CHARACTER, characterId);
        activity.startActivityForResult(intent, requestCode);
    }

    private CharacterId _characterId;
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
            this.<TextView>findViewById(R.id.tokenInfo).setText(getString(R.string.unicodeAssignerTokenInfo, token));
            _fieldView = findViewById(R.id.field);
            _fieldView.addTextChangedListener(this);
            _resultInfoView = findViewById(R.id.resultInfo);

            if (savedInstanceState != null) {
                _confirmationDialogPresent = savedInstanceState.getBoolean(SavedKeys.CONFIRMATION_DIALOG_PRESENT);
                if (_confirmationDialogPresent) {
                    showAssignConfirmationDialog();
                }
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
            resultText = getString(R.string.unicodeAssignerInfoText, unicodeText);
        }
        else {
            resultText = getString(R.string.unicodeAssignerErrorText);
            textColorRes = R.color.errorTextColor;
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

    private void showAssignConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.assignUnicodeConfirmationText)
                .setPositiveButton(R.string.assignUnicodeButtonText, (dialog, which) -> assign())
                .setOnCancelListener(dialog -> _confirmationDialogPresent = false)
                .create().show();
    }

    @Override
    public void onClick(View v) {
        if (_fieldView.getText().toString().length() == 1) {
            _confirmationDialogPresent = true;
            showAssignConfirmationDialog();
        }
        else {
            Toast.makeText(this, R.string.wordEditorWrongTextError, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (_confirmationDialogPresent) {
            outState.putBoolean(SavedKeys.CONFIRMATION_DIALOG_PRESENT, true);
        }
    }
}
