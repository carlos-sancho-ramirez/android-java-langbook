package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public final class SentenceEditorActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_ADD_SPAN = 1;

    static void open(Activity activity, int requestCode) {
        final Intent intent = new Intent(activity, SentenceEditorActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sentence_editor_activity);

        findViewById(R.id.nextButton).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        final EditText textField = findViewById(R.id.textField);
        final String text = textField.getText().toString();
        SpanEditorActivity.open(this, REQUEST_CODE_ADD_SPAN, text);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            finish();
        }
    }
}
