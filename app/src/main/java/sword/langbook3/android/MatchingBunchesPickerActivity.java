package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;
import sword.collections.ImmutableMap;
import sword.collections.Procedure;
import sword.collections.Set;
import sword.langbook3.android.db.BunchId;

public final class MatchingBunchesPickerActivity extends Activity implements View.OnClickListener {

    interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    public interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

    private Controller _controller;
    private MatchingBunchesPickerAdapter _adapter;

    public static void open(Activity activity, int requestCode, Controller controller) {
        final Intent intent = new Intent(activity, MatchingBunchesPickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.matching_bunches_picker_activity);

        _controller = getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _controller.loadBunches(this, bunches -> {
            _adapter = new MatchingBunchesPickerAdapter(bunches);
            this.<ListView>findViewById(R.id.listView).setAdapter(_adapter);
        });
    }

    @Override
    public void onClick(View v) {
        _controller.complete(this, _adapter.getCheckedBunches());
    }

    public interface Controller extends Parcelable {
        void loadBunches(@NonNull Activity activity, @NonNull Procedure<ImmutableMap<BunchId, String>> procedure);
        void complete(@NonNull Activity activity, @NonNull Set<BunchId> selectedBunches);
    }
}
