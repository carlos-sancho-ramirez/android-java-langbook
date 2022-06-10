package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.langbook3.android.collections.TransformableUtils;
import sword.langbook3.android.collections.TraversableUtils;
import sword.langbook3.android.controllers.AddLanguageWordEditorControllerForLanguage;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdManager;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdManager;

public final class LanguageAdderActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_NEXT_STEP = 1;

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_NEXT_STEP && resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
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

        final LangbookDbChecker checker = DbManager.getInstance().getManager();
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

        if (errorMessage == null) {
            final ImmutableSet<ConceptId> concepts = checker.getNextAvailableConceptIds(alphabetCount + 1);
            final LanguageId languageId = LanguageIdManager.conceptAsLanguageId(TraversableUtils.first(concepts));
            final ImmutableList<AlphabetId> alphabets = TransformableUtils.skip(concepts, 1).map(AlphabetIdManager::conceptAsAlphabetId);
            final String title = getString(R.string.newLanguageNameActivityTitle);
            final WordEditorActivity.Controller controller = new AddLanguageWordEditorControllerForLanguage(code, languageId, alphabets, title);
            WordEditorActivity.open(this, REQUEST_CODE_NEXT_STEP, controller);
        }
        else {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }
}
