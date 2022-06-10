package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public final class WelcomeActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_ADD_LANGUAGE = 1;

    public static void open(Activity activity, int requestCode) {
        final Intent intent = new Intent(activity, WelcomeActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);

        findViewById(R.id.addLanguageButton).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intentions.addLanguage(this, REQUEST_CODE_ADD_LANGUAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ADD_LANGUAGE && resultCode == RESULT_OK) {
            finish();
        }
    }
}
