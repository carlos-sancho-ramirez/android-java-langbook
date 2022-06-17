package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class AcceptationConfirmationActivity extends AbstractAcceptationDetailsActivity {

    public static final int REQUEST_CODE_NEXT_STEP = 1;

    interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    public interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

    public static void open(Activity activity, int requestCode, @NonNull Controller controller) {
        Intent intent = new Intent(activity, AcceptationConfirmationActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    private Controller _controller;

    @Override
    boolean canNavigate() {
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _controller = getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _acceptation = _controller.getAcceptation();
        updateModelAndUi();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (_model != null) {
            final MenuInflater inflater = new MenuInflater(this);
            inflater.inflate(R.menu.acceptation_details_activity_confirm, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuItemConfirm) {
            _controller.confirm(new DefaultPresenter(this));
            return true;
        }

        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        _controller.onActivityResult(this, requestCode, resultCode, data);
    }

    public interface Controller extends Parcelable {
        @NonNull
        AcceptationId getAcceptation();
        void confirm(@NonNull Presenter presenter);
        void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data);
    }
}
