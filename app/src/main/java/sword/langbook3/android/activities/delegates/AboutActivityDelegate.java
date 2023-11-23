package sword.langbook3.android.activities.delegates;

import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;

import sword.langbook3.android.BuildConfig;
import sword.langbook3.android.R;
import sword.langbook3.android.interf.ActivityInterface;

public final class AboutActivityDelegate<Activity extends ActivityInterface> extends AbstractActivityDelegate<Activity> {

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        activity.setContentView(R.layout.about_activity);

        final TextView appVersion = activity.findViewById(R.id.appVersion);
        appVersion.setText("version " + BuildConfig.VERSION_NAME);
    }
}
