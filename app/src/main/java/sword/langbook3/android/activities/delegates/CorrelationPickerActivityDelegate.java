package sword.langbook3.android.activities.delegates;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ListView;

import androidx.annotation.NonNull;

import sword.collections.ImmutableIntList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.IntResultFunction;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.CorrelationPickerAdapter;
import sword.langbook3.android.R;
import sword.langbook3.android.collections.Procedure2;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class CorrelationPickerActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> implements View.OnClickListener {
    public static final int REQUEST_CODE_NEXT_STEP = 1;

    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    private interface SavedKeys {
        String SELECTION = "sel";
    }

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CORRELATION_ARRAY = BundleKeys.CORRELATION_ARRAY;
    }

    private Activity _activity;
    private Controller _controller;
    private int _selection = ListView.INVALID_POSITION;

    private ListView _listView;
    private ImmutableSet<ImmutableCorrelationArray<AlphabetId>> _options;
    private ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId> _knownCorrelations;

    private int findSuggestedPosition() {
        final ImmutableSet<ImmutableCorrelation<AlphabetId>> known = _knownCorrelations.keySet();
        final IntResultFunction<ImmutableCorrelationArray<AlphabetId>> func = option -> option.filter(known::contains).size();
        final ImmutableIntList knownParity = _options.toList().mapToInt(func);

        final int length = knownParity.size();
        int max = 0;
        int index = -1;
        for (int i = 0; i < length; i++) {
            int parity = knownParity.get(i);
            if (parity > max) {
                max = parity;
                index = i;
            }
            else if (parity == max) {
                index = -1;
            }
        }

        return index;
    }

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        activity.setContentView(R.layout.correlation_picker_activity);

        if (savedInstanceState != null) {
            _selection = savedInstanceState.getInt(SavedKeys.SELECTION, ListView.INVALID_POSITION);
        }

        _controller = activity.getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _controller.load(new DefaultPresenter(activity), (options, knownCorrelations) -> {
            _options = options;
            _knownCorrelations = knownCorrelations;

            _listView = activity.findViewById(R.id.listView);
            _listView.setAdapter(new CorrelationPickerAdapter(_options, _knownCorrelations.keySet()));
            _listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            final int suggestedPosition = findSuggestedPosition();
            if (suggestedPosition >= 0) {
                _listView.setItemChecked(suggestedPosition, true);
            }

            activity.findViewById(R.id.nextButton).setOnClickListener(this);
        });
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(activity, _options, _selection, requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        _selection = _listView.getCheckedItemPosition();
        if (_selection != ListView.INVALID_POSITION) {
            _controller.complete(new DefaultPresenter(_activity), _options.valueAt(_selection));
        }
        else {
            _activity.showToast("Please select an option");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        if (_selection >= 0) {
            outState.putInt(SavedKeys.SELECTION, _selection);
        }
    }

    public interface Controller extends Parcelable {
        void load(@NonNull Presenter presenter, @NonNull Procedure2<ImmutableSet<ImmutableCorrelationArray<AlphabetId>>, ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId>> procedure);
        void complete(@NonNull Presenter presenter, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption);
        void onActivityResult(@NonNull ActivityInterface activity, @NonNull ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options, int selection, int requestCode, int resultCode, Intent data);
    }
}
