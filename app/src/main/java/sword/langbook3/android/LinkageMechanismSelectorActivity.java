package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class LinkageMechanismSelectorActivity extends Activity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {

    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    public static void open(@NonNull Activity activity, int requestCode, @NonNull Controller controller) {
        final Intent intent = new Intent(activity, LinkageMechanismSelectorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    private Controller _controller;
    private RadioGroup _radioGroup;
    private TextView _infoText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.linkage_mechanism_selector_activity);

        _controller = getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _infoText = findViewById(R.id.info);
        findViewById(R.id.confirmButton).setOnClickListener(this);
        _radioGroup = findViewById(R.id.radioGroup);
        _radioGroup.setOnCheckedChangeListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        @StringRes int message = 0;
        if (checkedId == R.id.shareConceptRadioButton) {
            message = R.string.shareConceptOptionDescription;
        }
        else if (checkedId == R.id.duplicateAcceptationRadioButton) {
            message = R.string.duplicateAcceptationOptionDescription;
        }

        final String htmlText = getString(message, _controller.getSourceWord(), _controller.getTargetWord());
        _infoText.setText(Html.fromHtml(htmlText));
    }

    @Override
    public void onClick(View v) {
        final int selection = _radioGroup.getCheckedRadioButtonId();
        Controller.LinkageOption option = null;
        if (selection == R.id.shareConceptRadioButton) {
            option = Controller.LinkageOption.SHARE_CONCEPT;
        }
        else if (selection == R.id.duplicateAcceptationRadioButton) {
            option = Controller.LinkageOption.DUPLICATE_ACCEPTATION;
        }

        if (option != null) {
            _controller.complete(new DefaultPresenter(this), option);
        }
        else {
            Toast.makeText(this, R.string.noOptionSelectedError, Toast.LENGTH_SHORT).show();
        }
    }

    public interface Controller extends Parcelable {
        @NonNull
        String getSourceWord();

        @NonNull
        String getTargetWord();

        void complete(@NonNull Presenter presenter, @NonNull LinkageOption option);

        enum LinkageOption {
            SHARE_CONCEPT,
            DUPLICATE_ACCEPTATION
        }
    }
}
