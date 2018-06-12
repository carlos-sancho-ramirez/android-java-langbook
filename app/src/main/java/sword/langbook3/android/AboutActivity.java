package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public final class AboutActivity extends Activity {

    public static void open(Context context) {
        Intent intent = new Intent(context, AboutActivity.class);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_activity);

        final TextView appVersion = findViewById(R.id.appVersion);
        appVersion.setText("version " + BuildConfig.VERSION_NAME);
    }
}
