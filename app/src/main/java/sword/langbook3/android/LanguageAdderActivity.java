package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import sword.collections.ImmutableIntRange;
import sword.database.Database;

public final class LanguageAdderActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_NAME_LANGUAGE = 1;

    public static void open(Activity activity, int requestCode) {
        final Intent intent = new Intent(activity, LanguageAdderActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    private EditText _codeField;
    private EditText _alphabetCountField;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_adder_activity);

        _codeField = findViewById(R.id.languageCodeValue);
        _alphabetCountField = findViewById(R.id.languageAlphabetCountValue);
        findViewById(R.id.nextButton).setOnClickListener(this);
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
        final ImmutableIntRange alphabets = new ImmutableIntRange(languageId + 1, languageId + alphabetCount);

        if (errorMessage == null) {
            WordEditorActivity.open(this, REQUEST_CODE_NAME_LANGUAGE, alphabets, languageId);
        }
        else {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }
}
