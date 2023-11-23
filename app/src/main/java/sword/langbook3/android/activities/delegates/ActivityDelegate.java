package sword.langbook3.android.activities.delegates;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import sword.langbook3.android.interf.ActivityInterface;

public interface ActivityDelegate<Activity extends ActivityInterface> {
    void onCreate(@NonNull Activity activity, Bundle savedInstanceState);
    void onStart(@NonNull Activity activity);
    void onResume(@NonNull Activity activity);
    void onPause(@NonNull Activity activity);
    void onStop(@NonNull Activity activity);
    void onDestroy(@NonNull Activity activity);

    boolean onCreateOptionsMenu(@NonNull Activity activity, Menu menu);
    boolean onOptionsItemSelected(@NonNull Activity activity, MenuItem item);

    void onRestoreInstanceState(@NonNull Activity activity, @NonNull Bundle savedInstanceState);
    void onSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState);
    void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data);
    void onRequestPermissionsResult(@NonNull Activity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults);

    /**
     * Queried when the activity onBackPressed is called.
     * @param activity The activity interface
     * @return True if the event has been consumed. In case of being false,
     *         the activity should proceed by calling its parent and execute
     *         the default behavior.
     */
    boolean onBackPressed(@NonNull Activity activity);
}
