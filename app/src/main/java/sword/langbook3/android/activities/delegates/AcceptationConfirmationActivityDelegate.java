package sword.langbook3.android.activities.delegates;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class AcceptationConfirmationActivityDelegate<Activity extends ActivityExtensions> extends AbstractAcceptationDetailsActivityDelegate<Activity> {

    public static final int REQUEST_CODE_NEXT_STEP = 1;

    public interface ArgKeys extends AbstractAcceptationDetailsActivityDelegate.ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

    private AcceptationConfirmationActivityDelegate.Controller _controller;

    @Override
    boolean canNavigate() {
        return false;
    }

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        super.onCreate(activity, savedInstanceState);
        _controller = activity.getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _acceptation = _controller.getAcceptation();
        updateModelAndUi();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Activity activity, Menu menu) {
        if (_model != null) {
            activity.newMenuInflater().inflate(R.menu.acceptation_details_activity_confirm, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull Activity activity, MenuItem item) {
        if (item.getItemId() == R.id.menuItemConfirm) {
            _controller.confirm(new DefaultPresenter(_activity));
            return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(activity, requestCode, resultCode, data);
    }

    public interface Controller extends Parcelable {
        @NonNull
        AcceptationId getAcceptation();
        void confirm(@NonNull Presenter presenter);
        void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data);
    }
}
