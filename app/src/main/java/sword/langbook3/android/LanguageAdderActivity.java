package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableList;
import sword.collections.IntKeyMap;
import sword.collections.MutableIntKeyMap;
import sword.langbook3.android.db.LangbookChecker;

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

    private ImmutableIntKeyMap<String> correlationFromCorrelationArray(ImmutableList<ImmutableIntKeyMap<String>> correlationArray) {
        return correlationArray.reduce((corr1, corr2) -> {
            final MutableIntKeyMap<String> mixed = corr1.mutate();
            for (IntKeyMap.Entry<String> entry : corr2.entries()) {
                final int key = entry.key();
                mixed.put(key, mixed.get(key) + entry.value());
            }

            return mixed.toImmutable();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_NAME_LANGUAGE) {
            if (resultCode == RESULT_OK) {
                final ParcelableCorrelationArray parcelableArray = data
                        .getParcelableExtra(CorrelationPickerActivity.ResultKeys.CORRELATION_ARRAY);
                _state.setLanguageCorrelationArray(parcelableArray.get());
                final String title = getString(R.string.newMainAlphabetNameActivityTitle);
                WordEditorActivity.open(this, REQUEST_CODE_NAME_ALPHABET, title, _state.getEmptyCorrelation(),
                        _state.getCurrentConcept());
            }
            else {
                _state.reset();
            }
        }
        else if (requestCode == REQUEST_CODE_NAME_ALPHABET) {
            if (resultCode == RESULT_OK) {
                final ParcelableCorrelationArray parcelableArray = data
                        .getParcelableExtra(CorrelationPickerActivity.ResultKeys.CORRELATION_ARRAY);
                _state.setNextAlphabetCorrelationArray(parcelableArray.get());

                if (_state.missingAlphabetCorrelationArray()) {
                    final String title = getString(R.string.newAuxAlphabetNameActivityTitle);
                    WordEditorActivity.open(this, REQUEST_CODE_NAME_ALPHABET, title, _state.getEmptyCorrelation(),
                            _state.getCurrentConcept());
                }
                else {
                    _state.storeIntoDatabase(DbManager.getInstance().getManager());

                    Toast.makeText(this, R.string.addLanguageFeedback, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }
            }
            else {
                if (_state.hasAtLeastOneAlphabetCorrelationArray()) {
                    final ImmutableIntKeyMap<String> correlation = correlationFromCorrelationArray(_state.popLastAlphabetCorrelationArray());
                    final int titleResId = _state.hasAtLeastOneAlphabetCorrelationArray()?
                            R.string.newMainAlphabetNameActivityTitle : R.string.newAuxAlphabetNameActivityTitle;
                    WordEditorActivity.open(this, REQUEST_CODE_NAME_ALPHABET, getString(titleResId), correlation, _state.getCurrentConcept());
                }
                else {
                    final ImmutableIntKeyMap<String> correlation = correlationFromCorrelationArray(_state.popLanguageCorrelationArray());
                    final String title = getString(R.string.newLanguageNameActivityTitle);
                    WordEditorActivity.open(this, REQUEST_CODE_NAME_LANGUAGE, title, correlation, _state.getCurrentConcept());
                }
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

        final LangbookChecker checker = DbManager.getInstance().getManager();
        String errorMessage = null;
        if (!code.matches(LanguageCodeRules.REGEX)) {
            errorMessage = getString(R.string.languageAdderBadLanguageCode);
        }
        else if (alphabetCount <= 0 || alphabetCount > 5) {
            errorMessage = getString(R.string.languageAdderBadAlphabetCount);
        }
        else if (checker.findLanguageByCode(code) != null) {
            errorMessage = getString(R.string.languageAdderLanguageCodeInUse);
        }

        final int languageId = checker.getMaxConcept() + 1;

        if (errorMessage == null) {
            _state.setBasicDetails(code, languageId, alphabetCount);
            final String title = getString(R.string.newLanguageNameActivityTitle);
            WordEditorActivity.open(this, REQUEST_CODE_NAME_LANGUAGE, title, _state.getEmptyCorrelation(), languageId);
        }
        else {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SavedKeys.STATE, _state);
    }
}
