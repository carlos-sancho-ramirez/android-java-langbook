package sword.langbook3.android.activities.delegates;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import sword.collections.Procedure;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.R;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class SentenceEditorActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> implements View.OnClickListener {
    public static final int REQUEST_CODE_ADD_SPAN = 1;

    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    interface ResultKeys {
        String SENTENCE_ID = BundleKeys.SENTENCE_ID;
    }

    private Activity _activity;
    private EditText _textField;

    private Controller _controller;

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        activity.setContentView(R.layout.sentence_editor_activity);

        _controller = activity.getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        activity.findViewById(R.id.nextButton).setOnClickListener(this);
        _textField = activity.findViewById(R.id.textField);
        _textField.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                openSpanEditor();
                return true;
            }

            return false;
        });

        if (savedInstanceState == null) {
            _controller.load(_textField::setText);
        }
    }

    @Override
    public void onClick(View v) {
        openSpanEditor();
    }

    private void openSpanEditor() {
        _controller.complete(new DefaultPresenter(_activity), _textField.getText().toString());
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(activity, requestCode, resultCode, data);
    }

    public interface Controller extends Parcelable {
        void load(@NonNull Procedure<String> procedure);
        void complete(@NonNull Presenter presenter, @NonNull String text);
        void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data);
    }
}
