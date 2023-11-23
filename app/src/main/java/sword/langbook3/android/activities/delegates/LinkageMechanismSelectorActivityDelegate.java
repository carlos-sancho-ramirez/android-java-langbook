package sword.langbook3.android.activities.delegates;

import android.os.Bundle;
import android.os.Parcelable;
import android.text.Html;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.R;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class LinkageMechanismSelectorActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    private Activity _activity;
    private Controller _controller;
    private RadioGroup _radioGroup;
    private TextView _infoText;

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        activity.setContentView(R.layout.linkage_mechanism_selector_activity);

        _controller = activity.getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _infoText = activity.findViewById(R.id.info);
        activity.findViewById(R.id.confirmButton).setOnClickListener(this);
        _radioGroup = activity.findViewById(R.id.radioGroup);
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

        final String htmlText = _activity.getString(message, _controller.getSourceWord(), _controller.getTargetWord());
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
            _controller.complete(new DefaultPresenter(_activity), option);
        }
        else {
            _activity.showToast(R.string.noOptionSelectedError);
        }
    }

    public interface Controller extends Parcelable {
        @NonNull
        String getSourceWord();

        @NonNull
        String getTargetWord();

        void complete(@NonNull Presenter presenter, @NonNull Controller.LinkageOption option);

        enum LinkageOption {
            SHARE_CONCEPT,
            DUPLICATE_ACCEPTATION
        }
    }
}
