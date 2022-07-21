package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import sword.collections.Procedure;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class SentenceEditorActivity extends Activity implements View.OnClickListener {

    public static final int REQUEST_CODE_ADD_SPAN = 1;

    interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    interface ResultKeys {
        String SENTENCE_ID = BundleKeys.SENTENCE_ID;
    }

    static void open(@NonNull Activity activity, int requestCode, @NonNull Controller controller) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    private EditText _textField;

    private Controller _controller;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sentence_editor_activity);

        _controller = getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        findViewById(R.id.nextButton).setOnClickListener(this);
        _textField = findViewById(R.id.textField);
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
        _controller.complete(new DefaultPresenter(this), _textField.getText().toString());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(this, requestCode, resultCode, data);
    }

    public interface Controller extends Parcelable {
        void load(@NonNull Procedure<String> procedure);
        void complete(@NonNull Presenter presenter, @NonNull String text);
        void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data);
    }
}
