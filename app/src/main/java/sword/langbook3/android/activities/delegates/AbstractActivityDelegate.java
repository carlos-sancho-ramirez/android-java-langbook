package sword.langbook3.android.activities.delegates;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import sword.langbook3.android.interf.ActivityInterface;

abstract class AbstractActivityDelegate<Activity extends ActivityInterface> implements ActivityDelegate<Activity> {

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        // Nothing to be done by default
    }

    @Override
    public void onStart(@NonNull Activity activity) {
        // Nothing to be done by default
    }

    @Override
    public void onResume(@NonNull Activity activity) {
        // Nothing to be done by default
    }

    @Override
    public void onPause(@NonNull Activity activity) {
        // Nothing to be done by default
    }

    @Override
    public void onStop(@NonNull Activity activity) {
        // Nothing to be done by default
    }

    @Override
    public void onDestroy(@NonNull Activity activity) {
        // Nothing to be done by default
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Activity activity, Menu menu) {
        // Nothing to be done by default
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull Activity activity, MenuItem item) {
        // Nothing to be done by default
        return false;
    }

    @Override
    public void onRestoreInstanceState(@NonNull Activity activity, @NonNull Bundle savedInstanceState) {
        // Nothing to be done by default
    }

    @Override
    public void onSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        // Nothing to be done by default
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        // Nothing to be done by default
    }

    @Override
    public void onRequestPermissionsResult(@NonNull Activity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Nothing to be done by default
    }

    @Override
    public boolean onBackPressed(@NonNull Activity activity) {
        return false;
    }
}
