package sword.langbook3.android.activities.delegates;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LanguageCodeRules;
import sword.langbook3.android.R;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ActivityInterface;

public final class LanguageAdderActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> implements View.OnClickListener {
    public static final int REQUEST_CODE_NEXT_STEP = 1;

    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    private Activity _activity;
    private Controller _controller;
    private EditText _codeField;
    private EditText _alphabetCountField;

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        activity.setContentView(R.layout.language_adder_activity);

        _controller = activity.getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _codeField = activity.findViewById(R.id.languageCodeValue);
        _alphabetCountField = activity.findViewById(R.id.languageAlphabetCountValue);
        activity.findViewById(R.id.nextButton).setOnClickListener(this);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(_activity, requestCode, resultCode, data);
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
            errorMessage = _activity.getString(R.string.languageAdderBadLanguageCode);
        }
        else if (alphabetCount <= 0 || alphabetCount > 5) {
            errorMessage = _activity.getString(R.string.languageAdderBadAlphabetCount);
        }
        else if (checker.findLanguageByCode(code) != null) {
            errorMessage = _activity.getString(R.string.languageAdderLanguageCodeInUse);
        }

        if (errorMessage == null) {
            _controller.complete(_activity, code, alphabetCount);
        }
        else {
            _activity.showToast(errorMessage);
        }
    }

    public interface Controller extends Parcelable {
        void complete(@NonNull ActivityExtensions activity, String languageCode, int alphabetCount);
        void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data);
    }
}
