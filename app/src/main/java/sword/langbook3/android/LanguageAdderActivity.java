package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import sword.database.Database;
import sword.langbook3.android.db.LangbookReadableDatabase;

public final class LanguageAdderActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_NAME_LANGUAGE = 1;
    private static final int REQUEST_CODE_NAME_ALPHABET = 2;

    public static void open(Activity activity, int requestCode) {
        final Intent intent = new Intent(activity, LanguageAdderActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    private interface SavedKeys {
        String STATE = "st";
    }

    private EditText _codeField;
    private EditText _alphabetCountField;

    private LanguageAdderActivityState _state;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_adder_activity);

        _codeField = findViewById(R.id.languageCodeValue);
        _alphabetCountField = findViewById(R.id.languageAlphabetCountValue);
        findViewById(R.id.nextButton).setOnClickListener(this);

        if (savedInstanceState != null) {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }
        else {
            _state = new LanguageAdderActivityState();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_NAME_LANGUAGE && resultCode == RESULT_OK) {
            final ParcelableCorrelationArray parcelableArray = data.getParcelableExtra(CorrelationPickerActivity.ResultKeys.CORRELATION_ARRAY);
            _state.setLanguageCorrelationArray(parcelableArray.get());
            WordEditorActivity.open(this, REQUEST_CODE_NAME_ALPHABET, _state.getAlphabets(), _state.getCurrentConcept());
        }
        else if (requestCode == REQUEST_CODE_NAME_ALPHABET && resultCode == RESULT_OK) {
            final ParcelableCorrelationArray parcelableArray = data.getParcelableExtra(CorrelationPickerActivity.ResultKeys.CORRELATION_ARRAY);
            _state.setNextAlphabetCorrelationArray(parcelableArray.get());

            if (_state.missingAlphabetCorrelationArray()) {
                WordEditorActivity.open(this, REQUEST_CODE_NAME_ALPHABET, _state.getAlphabets(), _state.getCurrentConcept());
            }
            else {
                final Database db = DbManager.getInstance().getDatabase();
                _state.storeIntoDatabase(db);

                Toast.makeText(this, R.string.addLanguageFeedback, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        }
    }

    @Override
    public void onClick(View v) {
        final String code = _codeField.getText().toString();

        final String countStr = _alphabetCountField.getText().toString();
        int alphabetCount = 0;
        try {
            alphabetCount = Integer.parseInt(countStr);
        }
        catch (NumberFormatException e) {
            // Nothing to be done
        }

        final Database db = DbManager.getInstance().getDatabase();
        String errorMessage = null;
        if (!code.matches("[a-z][a-z]")) {
            errorMessage = getString(R.string.languageAdderBadLanguageCode);
        }
        else if (alphabetCount <= 0 || alphabetCount > 5) {
            errorMessage = getString(R.string.languageAdderBadAlphabetCount);
        }
        else if (LangbookReadableDatabase.findLanguageByCode(db, code) != null) {
            errorMessage = getString(R.string.languageAdderLanguageCodeInUse);
        }

        final int languageId = LangbookReadableDatabase.getMaxConcept(db) + 1;

        if (errorMessage == null) {
            _state.setBasicDetails(code, languageId, alphabetCount);
            WordEditorActivity.open(this, REQUEST_CODE_NAME_LANGUAGE, _state.getAlphabets(), languageId);
        }
        else {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(SavedKeys.STATE, _state);
    }
}
