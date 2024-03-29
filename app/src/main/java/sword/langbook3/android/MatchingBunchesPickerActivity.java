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
import sword.langbook3.android.presenters.Presenter;
import sword.langbook3.android.presenters.DefaultPresenter;

public final class MatchingBunchesPickerActivity extends Activity implements View.OnClickListener {

    public static final int REQUEST_CODE_NEXT_STEP = 1;

    interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    public interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CHARACTER_COMPOSITION_TYPE_ID = BundleKeys.CHARACTER_COMPOSITION_TYPE_ID;
    }

    private final Presenter _presenter = new DefaultPresenter(this);
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
        _controller.loadBunches(_presenter, bunches -> {
            _adapter = new MatchingBunchesPickerAdapter(bunches);
            this.<ListView>findViewById(R.id.listView).setAdapter(_adapter);
            findViewById(R.id.nextButton).setOnClickListener(this);
        });
    }

    @Override
    public void onClick(View v) {
        _controller.complete(_presenter, _adapter.getCheckedBunches());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        _controller.onActivityResult(this, resultCode, resultCode, data);
    }

    public interface Controller extends Parcelable {
        void loadBunches(@NonNull Presenter presenter, @NonNull Procedure<ImmutableMap<BunchId, String>> procedure);
        void complete(@NonNull Presenter presenter, @NonNull Set<BunchId> selectedBunches);
        void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data);
    }
}
