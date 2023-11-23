package sword.langbook3.android.activities.delegates;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;

import sword.collections.ImmutableMap;
import sword.collections.Procedure;
import sword.collections.Set;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.MatchingBunchesPickerAdapter;
import sword.langbook3.android.R;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class MatchingBunchesPickerActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> implements View.OnClickListener {
    public static final int REQUEST_CODE_NEXT_STEP = 1;

    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CHARACTER_COMPOSITION_TYPE_ID = BundleKeys.CHARACTER_COMPOSITION_TYPE_ID;
    }

    private Presenter _presenter;
    private Controller _controller;
    private MatchingBunchesPickerAdapter _adapter;

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _presenter = new DefaultPresenter(activity);
        activity.setContentView(R.layout.matching_bunches_picker_activity);

        _controller = activity.getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _controller.loadBunches(_presenter, bunches -> {
            _adapter = new MatchingBunchesPickerAdapter(bunches);
            activity.<ListView>findViewById(R.id.listView).setAdapter(_adapter);
            activity.findViewById(R.id.nextButton).setOnClickListener(this);
        });
    }

    @Override
    public void onClick(View v) {
        _controller.complete(_presenter, _adapter.getCheckedBunches());
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(activity, resultCode, resultCode, data);
    }

    public interface Controller extends Parcelable {
        void loadBunches(@NonNull Presenter presenter, @NonNull Procedure<ImmutableMap<BunchId, String>> procedure);
        void complete(@NonNull Presenter presenter, @NonNull Set<BunchId> selectedBunches);
        void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data);
    }
}
