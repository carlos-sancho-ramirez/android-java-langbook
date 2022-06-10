package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.IntResultFunction;
import sword.langbook3.android.collections.Procedure2;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;

public final class CorrelationPickerActivity extends Activity implements View.OnClickListener {

    public static final int REQUEST_CODE_NEXT_STEP = 1;

    interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    private interface SavedKeys {
        String SELECTION = "sel";
    }

    public interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CORRELATION_ARRAY = BundleKeys.CORRELATION_ARRAY;
    }

    private Controller _controller;
    private int _selection = ListView.INVALID_POSITION;

    private ListView _listView;
    private ImmutableSet<ImmutableCorrelationArray<AlphabetId>> _options;
    private ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId> _knownCorrelations;

    public static void open(@NonNull Activity activity, int requestCode, @NonNull CorrelationPickerActivity.Controller controller) {
        final Intent intent = new Intent(activity, CorrelationPickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.correlation_picker_activity);

        if (savedInstanceState != null) {
            _selection = savedInstanceState.getInt(SavedKeys.SELECTION, ListView.INVALID_POSITION);
        }

        _controller = getIntent().getParcelableExtra(MatchingBunchesPickerActivity.ArgKeys.CONTROLLER);
        _controller.load(this, savedInstanceState == null, (options, knownCorrelations) -> {
            _options = options;
            _knownCorrelations = knownCorrelations;

            _listView = findViewById(R.id.listView);
            _listView.setAdapter(new CorrelationPickerAdapter(_options, _knownCorrelations.keySet()));
            _listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            final int suggestedPosition = findSuggestedPosition();
            if (suggestedPosition >= 0) {
                _listView.setItemChecked(suggestedPosition, true);
            }

            findViewById(R.id.nextButton).setOnClickListener(this);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(this, _options, _selection, requestCode, resultCode, data);
    }

    @Override
    public void onClick(View view) {
        _selection = _listView.getCheckedItemPosition();
        if (_selection != ListView.INVALID_POSITION) {
            _controller.complete(this, _options.valueAt(_selection));
        }
        else {
            Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (_selection >= 0) {
            outState.putInt(SavedKeys.SELECTION, _selection);
        }
    }

    public interface Controller extends Parcelable {
        void load(@NonNull Activity activity, boolean firstTime, @NonNull Procedure2<ImmutableSet<ImmutableCorrelationArray<AlphabetId>>, ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId>> procedure);
        void complete(@NonNull Activity activity, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption);
        void onActivityResult(@NonNull Activity activity, @NonNull ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options, int selection, int requestCode, int resultCode, Intent data);
    }
}
