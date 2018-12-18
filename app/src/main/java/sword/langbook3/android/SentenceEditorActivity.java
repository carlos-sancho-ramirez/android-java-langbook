package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public final class SentenceEditorActivity extends Activity implements View.OnClickListener {

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
        Toast.makeText(this, "Button clicked", Toast.LENGTH_SHORT).show();
    }
}
