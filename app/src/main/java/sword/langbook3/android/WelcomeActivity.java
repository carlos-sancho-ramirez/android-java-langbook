package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public final class WelcomeActivity extends Activity {

    public static void open(Activity activity, int requestCode) {
        final Intent intent = new Intent(activity, WelcomeActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_activity);
    }
}
