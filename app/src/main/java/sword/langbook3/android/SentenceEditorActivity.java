package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

public final class SentenceEditorActivity extends Activity implements View.OnClickListener {

    static final int NO_SENTENCE_ID = 0;

    private static final int REQUEST_CODE_ADD_SPAN = 1;

    interface ArgKeys {
        String CONCEPT = BundleKeys.CONCEPT;
        String STATIC_ACCEPTATION = BundleKeys.STATIC_ACCEPTATION;
        String SENTENCE_ID = BundleKeys.SENTENCE_ID;
    }

    static void openWithConcept(Activity activity, int requestCode, int concept) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        intent.putExtra(ArgKeys.CONCEPT, concept);
        activity.startActivityForResult(intent, requestCode);
    }

    static void openWithStaticAcceptation(Activity activity, int requestCode, int staticAcceptation) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        intent.putExtra(ArgKeys.STATIC_ACCEPTATION, staticAcceptation);
        activity.startActivityForResult(intent, requestCode);
    }

    static void openWithSentenceId(Activity activity, int requestCode, int sentenceId) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        intent.putExtra(ArgKeys.SENTENCE_ID, sentenceId);
        activity.startActivityForResult(intent, requestCode);
    }

    private EditText _textField;

    private int getSentenceId() {
        return getIntent().getIntExtra(ArgKeys.SENTENCE_ID, NO_SENTENCE_ID);
    }

    private int getStaticAcceptationId() {
        return getIntent().getIntExtra(ArgKeys.STATIC_ACCEPTATION, 0);
    }

    private int getConcept() {
        return getIntent().getIntExtra(ArgKeys.CONCEPT, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sentence_editor_activity);

        findViewById(R.id.nextButton).setOnClickListener(this);
        _textField = findViewById(R.id.textField);
        _textField.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                openSpanEditor();
                return true;
            }

            return false;
        });

        final int sentenceId = getSentenceId();
        if (sentenceId != NO_SENTENCE_ID) {
            _textField.setText(DbManager.getInstance().getManager().getSentenceText(sentenceId));
        }
    }

    @Override
    public void onClick(View v) {
        openSpanEditor();
    }

    private void openSpanEditor() {
        final int sentenceId = getSentenceId();
        final int staticAcceptation = getStaticAcceptationId();
        final String text = _textField.getText().toString();

        if (staticAcceptation != 0) {
            SpanEditorActivity.openWithStaticAcceptation(this, REQUEST_CODE_ADD_SPAN, text, staticAcceptation);
        }
        else if (sentenceId == NO_SENTENCE_ID) {
            SpanEditorActivity.openWithConcept(this, REQUEST_CODE_ADD_SPAN, text, getConcept());
        }
        else {
            SpanEditorActivity.openWithSentenceId(this, REQUEST_CODE_ADD_SPAN, text, sentenceId);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            setResult(RESULT_OK);
            finish();
        }
    }
}
