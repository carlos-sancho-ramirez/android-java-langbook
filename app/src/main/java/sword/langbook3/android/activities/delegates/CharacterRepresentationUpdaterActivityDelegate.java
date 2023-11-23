package sword.langbook3.android.activities.delegates;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.activities.delegates.CharacterCompositionEditorActivityDelegate.isInvalidRepresentation;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;

import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CharacterIdBundler;
import sword.langbook3.android.interf.ActivityExtensions;

public final class CharacterRepresentationUpdaterActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> implements View.OnClickListener, TextWatcher {
    public interface ArgKeys {
        String CHARACTER = BundleKeys.CHARACTER;
    }

    private interface SavedKeys {
        String CONFIRMATION_DIALOG_PRESENT = "cdp";
    }

    interface ResultKeys {
        String MERGED_CHARACTER = "mc";
    }

    private Activity _activity;
    private CharacterId _characterId;
    private CharacterId _mergingChar;
    private boolean _confirmationDialogPresent;

    private EditText _fieldView;
    private TextView _resultInfoView;

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        activity.setContentView(R.layout.unicode_assigner_activity);
        activity.findViewById(R.id.assignUnicodeButton).setOnClickListener(this);

        _characterId = CharacterIdBundler.readAsIntentExtra(activity.getIntent(), ArgKeys.CHARACTER);
        final String token = DbManager.getInstance().getManager().getToken(_characterId);

        if (token == null) {
            activity.finish();
        }
        else {
            activity.<TextView>findViewById(R.id.tokenInfo).setText(activity.getString(R.string.representationUpdaterTokenInfo, token));
            _fieldView = activity.findViewById(R.id.field);
            _fieldView.addTextChangedListener(this);
            _resultInfoView = activity.findViewById(R.id.resultInfo);
        }
    }

    @Override
    public void onRestoreInstanceState(@NonNull Activity activity, @NonNull Bundle savedInstanceState) {
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
            resultText = _activity.getString(R.string.representationUpdaterInfoText, unicodeText);
        }
        else if (isInvalidRepresentation(newText)) {
            resultText = _activity.getString(R.string.representationUpdaterErrorText);
            textColorRes = R.color.errorTextColor;
        }
        else {
            resultText = _activity.getString(R.string.representationUpdaterTokenInfoText, newText);
        }

        _resultInfoView.setText(resultText);
        _resultInfoView.setTextColor(_activity.getResources().getColor(textColorRes));
    }

    private void assign() {
        if (DbManager.getInstance().getManager().assignUnicode(_characterId, _fieldView.getText().toString().charAt(0))) {
            _activity.showToast(R.string.assignUnicodeFeedback);
            _activity.setResult(RESULT_OK);
            _activity.finish();
        }
        else {
            _activity.showToast(R.string.assignUnicodeError);
        }
    }

    private void merge() {
        if (DbManager.getInstance().getManager().mergeCharacters(_mergingChar, _characterId)) {
            _activity.showToast(R.string.assignUnicodeFeedback);

            final Intent data = new Intent();
            CharacterIdBundler.writeAsIntentExtra(data, ResultKeys.MERGED_CHARACTER, _mergingChar);
            _activity.setResult(RESULT_OK, data);
            _activity.finish();
        }
        else {
            _activity.showToast(R.string.mergeCharactersError);
        }
    }

    private void showAssignConfirmationDialog() {
        _activity.newAlertDialogBuilder()
                .setMessage(R.string.assignUnicodeConfirmationText)
                .setPositiveButton(R.string.assignUnicodeButtonText, (dialog, which) -> assign())
                .setOnCancelListener(dialog -> _confirmationDialogPresent = false)
                .create().show();
    }

    private void showMergeConfirmationDialog() {
        _activity.newAlertDialogBuilder()
                .setMessage(R.string.mergeUnicodeConfirmationText)
                .setPositiveButton(R.string.mergeUnicodeButtonText, (dialog, which) -> merge())
                .setOnCancelListener(dialog -> _confirmationDialogPresent = false)
                .create().show();
    }

    @Override
    public void onClick(View v) {
        final String text = _fieldView.getText().toString();
        if (text.isEmpty()) {
            _activity.showToast(R.string.representationUpdaterEmptyError);
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
            _activity.showToast(R.string.wordEditorWrongTextError);
        }
        else {
            if (DbManager.getInstance().getManager().updateToken(_characterId, text.substring(1, text.length() - 1))) {
                _activity.showToast(R.string.updateTokenFeedback);
                _activity.setResult(RESULT_OK);
                _activity.finish();
            }
            else {
                _activity.showToast(R.string.updateTokenError);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        if (_confirmationDialogPresent) {
            outState.putBoolean(SavedKeys.CONFIRMATION_DIALOG_PRESENT, true);
        }
    }
}
