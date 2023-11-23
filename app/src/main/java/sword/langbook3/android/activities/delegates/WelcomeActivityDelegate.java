package sword.langbook3.android.activities.delegates;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;

import sword.langbook3.android.Intentions;
import sword.langbook3.android.R;
import sword.langbook3.android.interf.ActivityExtensions;

public final class WelcomeActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> {
    private static final int REQUEST_CODE_ADD_LANGUAGE = 1;

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        activity.setContentView(R.layout.welcome_activity);
        activity.findViewById(R.id.addLanguageButton).setOnClickListener(v ->
                Intentions.addLanguage(activity, REQUEST_CODE_ADD_LANGUAGE));
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ADD_LANGUAGE && resultCode == RESULT_OK) {
            activity.finish();
        }
    }
}
