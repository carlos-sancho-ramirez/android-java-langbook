package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;

public final class AcceptationConfirmationActivity extends AbstractAcceptationDetailsActivity {

    public static void open(Activity activity, int requestCode, AcceptationId acceptation) {
        Intent intent = new Intent(activity, AcceptationConfirmationActivity.class);
        AcceptationIdBundler.writeAsIntentExtra(intent, ArgKeys.ACCEPTATION, acceptation);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    boolean canNavigate() {
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            final Intent intent = new Intent();
            AcceptationIdBundler.writeAsIntentExtra(intent, ResultKeys.ACCEPTATION, _acceptation);
            setResult(Activity.RESULT_OK, intent);
            finish();
            return true;
        }

        return false;
    }
}
