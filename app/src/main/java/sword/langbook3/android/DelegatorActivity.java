package sword.langbook3.android;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import sword.collections.Function;
import sword.langbook3.android.activities.delegates.ActivityDelegate;
import sword.langbook3.android.interf.ActivityInterface;

abstract class DelegatorActivity<ActivityAdapter extends ActivityInterface> extends Activity {

    @NonNull
    private final ActivityAdapter _activityAdapter;

    @NonNull
    private final ActivityDelegate<ActivityAdapter> _delegate;

    DelegatorActivity(@NonNull Function<Activity, ActivityAdapter> adapterCreator, @NonNull ActivityDelegate<ActivityAdapter> delegate) {
        final ActivityAdapter activityAdapter = adapterCreator.apply(this);
        ensureNonNull(activityAdapter, delegate);
        _activityAdapter = activityAdapter;
        _delegate = delegate;
    }

    @Override
    protected final void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _delegate.onCreate(_activityAdapter, savedInstanceState);
    }

    @Override
    protected final void onStart() {
        super.onStart();
        _delegate.onStart(_activityAdapter);
    }

    @Override
    protected final void onResume() {
        super.onResume();
        _delegate.onResume(_activityAdapter);
    }

    @Override
    protected final void onPause() {
        _delegate.onPause(_activityAdapter);
        super.onPause();
    }

    @Override
    protected final void onStop() {
        _delegate.onStop(_activityAdapter);
        super.onStop();
    }

    @Override
    protected final void onDestroy() {
        _delegate.onDestroy(_activityAdapter);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu) | _delegate.onCreateOptionsMenu(_activityAdapter, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item) | _delegate.onOptionsItemSelected(_activityAdapter, item);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        _delegate.onRestoreInstanceState(_activityAdapter, savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        _delegate.onSaveInstanceState(_activityAdapter, outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        _delegate.onActivityResult(_activityAdapter, requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        _delegate.onRequestPermissionsResult(_activityAdapter, requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        if (!_delegate.onBackPressed(_activityAdapter)) {
            super.onBackPressed();
        }
    }
}
