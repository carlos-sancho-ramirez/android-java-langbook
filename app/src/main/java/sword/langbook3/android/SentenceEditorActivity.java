package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;

public final class SentenceEditorActivity extends Activity implements View.OnClickListener {

    static final int NO_SYMBOL_ARRAY = 0;

    private static final int REQUEST_CODE_ADD_SPAN = 1;

    interface ArgKeys {
        String STATIC_ACCEPTATION = BundleKeys.STATIC_ACCEPTATION;
        String SYMBOL_ARRAY = BundleKeys.SYMBOL_ARRAY;
    }

    interface ResultKeys {
        String SYMBOL_ARRAY = BundleKeys.SYMBOL_ARRAY;
    }

    static void open(Activity activity, int requestCode) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    static void openWithStaticAcceptation(Activity activity, int requestCode, int staticAcceptation) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        intent.putExtra(ArgKeys.STATIC_ACCEPTATION, staticAcceptation);
        activity.startActivityForResult(intent, requestCode);
    }

    static void open(Activity activity, int requestCode, int symbolArrayId) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        intent.putExtra(ArgKeys.SYMBOL_ARRAY, symbolArrayId);
        activity.startActivityForResult(intent, requestCode);
    }

    private EditText _textField;

    private int getSymbolArrayId() {
        return getIntent().getIntExtra(ArgKeys.SYMBOL_ARRAY, NO_SYMBOL_ARRAY);
    }

    private int getStaticAcceptationId() {
        return getIntent().getIntExtra(ArgKeys.STATIC_ACCEPTATION, 0);
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

        final int symbolArrayId = getSymbolArrayId();
        if (symbolArrayId != NO_SYMBOL_ARRAY) {
            _textField.setText(DbManager.getInstance().getManager().getSymbolArray(symbolArrayId));
        }
    }

    @Override
    public void onClick(View v) {
        openSpanEditor();
    }

    private void openSpanEditor() {
        final int symbolArrayId = getSymbolArrayId();
        final int staticAcceptation = getStaticAcceptationId();
        final String text = _textField.getText().toString();

        if (staticAcceptation != 0) {
            SpanEditorActivity.openWithStaticAcceptation(this, REQUEST_CODE_ADD_SPAN, text, staticAcceptation);
        }
        else if (symbolArrayId == NO_SYMBOL_ARRAY) {
            SpanEditorActivity.open(this, REQUEST_CODE_ADD_SPAN, text);
        }
        else {
            SpanEditorActivity.open(this, REQUEST_CODE_ADD_SPAN, text, symbolArrayId);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            final Intent resultIntent = new Intent();
            resultIntent.putExtra(ResultKeys.SYMBOL_ARRAY, data.getIntExtra(SpanEditorActivity.ResultKeys.SYMBOL_ARRAY, 0));
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }
}
