package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.LangbookDbChecker;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class LanguageAdderActivity extends Activity implements View.OnClickListener {

    public static final int REQUEST_CODE_NEXT_STEP = 1;

    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    public static void open(@NonNull Activity activity, int requestCode, @NonNull Controller controller) {
        ensureNonNull(activity, controller);
        final Intent intent = new Intent(activity, LanguageAdderActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    private Controller _controller;
    private EditText _codeField;
    private EditText _alphabetCountField;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.language_adder_activity);

        _controller = getIntent().getParcelableExtra(MatchingBunchesPickerActivity.ArgKeys.CONTROLLER);
        _codeField = findViewById(R.id.languageCodeValue);
        _alphabetCountField = findViewById(R.id.languageAlphabetCountValue);
        findViewById(R.id.nextButton).setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(this, requestCode, resultCode, data);
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
            _controller.complete(this, code, alphabetCount);
        }
        else {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    public interface Controller extends Parcelable {
        void complete(@NonNull Activity activity, String languageCode, int alphabetCount);
        void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data);
    }
}
