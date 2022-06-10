package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.IntResultFunction;
import sword.langbook3.android.controllers.CorrelationPickerController;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdComparator;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdBundler;
import sword.langbook3.android.db.Correlation;
import sword.langbook3.android.db.CorrelationBundler;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;

public final class CorrelationPickerActivity extends Activity implements View.OnClickListener {

    public static final int REQUEST_CODE_PICK_BUNCHES = 1;

    interface ArgKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CONCEPT = BundleKeys.CONCEPT;
        String CONTROLLER = BundleKeys.CONTROLLER;
        String CORRELATION_MAP = BundleKeys.CORRELATION_MAP;
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

    /**
     * Opens the correlation picker in order to build a new correlation array.
     *
     * This method is intended for defining correlation arrays for agent adders, where there is neither a concept nor an acceptation linked.
     * In this case, this activity will not store anything into the database. Once a correlation array is selected, it will return the selected correlation array.
     *
     * @param activity Activity that opens this activity.
     * @param requestCode Request code that will be used on {@link Activity#onActivityResult(int, int, android.content.Intent)}.
     * @param texts Texts entered by the user in the WordEditorActivity.
     */
    public static void open(Activity activity, int requestCode, Correlation<AlphabetId> texts) {
        final Intent intent = new Intent(activity, CorrelationPickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, new CorrelationPickerController(null, null, texts.toImmutable(), false));
        CorrelationBundler.writeAsIntentExtra(intent, ArgKeys.CORRELATION_MAP, texts);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * Opens the correlation picker in order to build a new correlation array.
     *
     * This will open a {@link MatchingBunchesPickerActivity} as the following step.
     * This activity takes the responsibility to insert into the database a new acceptation with the concept provided, or a new concept if none was provided.
     * The resulting new acceptation identifier will be returned.
     *
     * This activity will also take responsibility to insert the resulting acceptation into any bunch selected on {@link sword.langbook3.android.MatchingBunchesPickerActivity}.
     *
     * @param activity Activity that opens this activity.
     * @param requestCode Request code that will be used on {@link Activity#onActivityResult(int, int, android.content.Intent)}.
     * @param concept Optional concept where this correlation array will be attached to, or null if none.
     * @param texts Texts entered by the user in the WordEditorActivity.
     */
    public static void open(Activity activity, int requestCode, ConceptId concept, Correlation<AlphabetId> texts) {
        final Intent intent = new Intent(activity, CorrelationPickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, new CorrelationPickerController(null, concept, texts.toImmutable(), true));
        CorrelationBundler.writeAsIntentExtra(intent, ArgKeys.CORRELATION_MAP, texts);
        if (concept != null) {
            ConceptIdBundler.writeAsIntentExtra(intent, ArgKeys.CONCEPT, concept);
        }

        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * Opens the correlation picker activity assuming that its editing an existing acceptation correlation array.
     *
     * This class will take the responsibility to update the array into the database for the given acceptation before returning back.
     * @param activity Activity that opens this activity.
     * @param requestCode Request code that will be used on {@link Activity#onActivityResult(int, int, android.content.Intent)}.
     * @param texts Texts entered by the user in the WordEditorActivity.
     * @param acceptation Identifier for an existing acceptation that will be modified after selecting the correlation array.
     */
    public static void open(Activity activity, int requestCode, Correlation<AlphabetId> texts, AcceptationId acceptation) {
        final Intent intent = new Intent(activity, CorrelationPickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, new CorrelationPickerController(acceptation, null, texts.toImmutable(), true));
        AcceptationIdBundler.writeAsIntentExtra(intent, ArgKeys.ACCEPTATION, acceptation);
        CorrelationBundler.writeAsIntentExtra(intent, ArgKeys.CORRELATION_MAP, texts);
        activity.startActivityForResult(intent, requestCode);
    }

    private ImmutableCorrelation<AlphabetId> getTexts() {
        final Correlation<AlphabetId> correlation = CorrelationBundler.readAsIntentExtra(getIntent(), ArgKeys.CORRELATION_MAP);
        return (correlation != null)? correlation.toImmutable() : null;
    }

    private ImmutableMap<ImmutableCorrelation<AlphabetId>, CorrelationId> findExistingCorrelations() {
        final ImmutableSet.Builder<ImmutableCorrelation<AlphabetId>> correlationsBuilder = new ImmutableHashSet.Builder<>();
        for (ImmutableCorrelationArray<AlphabetId> option : _options) {
            for (ImmutableCorrelation<AlphabetId> correlation : option) {
                correlationsBuilder.add(correlation);
            }
        }
        final ImmutableSet<ImmutableCorrelation<AlphabetId>> correlations = correlationsBuilder.build();

        final ImmutableMap.Builder<ImmutableCorrelation<AlphabetId>, CorrelationId> builder = new ImmutableHashMap.Builder<>();
        for (ImmutableCorrelation<AlphabetId> correlation : correlations) {
            final CorrelationId id = DbManager.getInstance().getManager().findCorrelation(correlation);
            if (id != null) {
                builder.put(correlation, id);
            }
        }

        return builder.build();
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

        _controller = getIntent().getParcelableExtra(MatchingBunchesPickerActivity.ArgKeys.CONTROLLER);
        final ImmutableCorrelation<AlphabetId> texts = getTexts();
        _options = texts.checkPossibleCorrelationArrays(new AlphabetIdComparator());
        _knownCorrelations = findExistingCorrelations();
        final int suggestedPosition = findSuggestedPosition();

        if (savedInstanceState != null) {
            _selection = savedInstanceState.getInt(SavedKeys.SELECTION, ListView.INVALID_POSITION);
        }

        if (savedInstanceState == null && _options.size() == 1) {
            _selection = 0;
            _controller.complete(this, _options.valueAt(0));
        }
        else {
            _listView = findViewById(R.id.listView);
            _listView.setAdapter(new CorrelationPickerAdapter(_options, _knownCorrelations.keySet()));
            _listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            if (suggestedPosition >= 0) {
                _listView.setItemChecked(suggestedPosition, true);
            }

            findViewById(R.id.nextButton).setOnClickListener(this);
        }
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
        void complete(@NonNull Activity activity, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption);
        void onActivityResult(@NonNull Activity activity, @NonNull ImmutableSet<ImmutableCorrelationArray<AlphabetId>> options, int selection, int requestCode, int resultCode, Intent data);
    }
}
