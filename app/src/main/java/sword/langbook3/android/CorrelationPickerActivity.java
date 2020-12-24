package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntValueHashMap;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.IntResultFunction;
import sword.collections.Map;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.Correlation;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookChecker;
import sword.langbook3.android.db.LangbookManager;

public final class CorrelationPickerActivity extends Activity implements View.OnClickListener {

    private static final int REQUEST_CODE_PICK_BUNCHES = 1;
    static final int NO_CONCEPT = 0;
    static final int NO_ACCEPTATION = 0;

    interface ArgKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String ALPHABETS = BundleKeys.ALPHABETS;
        String CONCEPT = BundleKeys.CONCEPT;
        String TEXTS = BundleKeys.TEXTS;
    }

    private interface SavedKeys {
        String SELECTION = "sel";
    }

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String CORRELATION_ARRAY = BundleKeys.CORRELATION_ARRAY;
    }

    private int _selection = ListView.INVALID_POSITION;

    private ListView _listView;
    private ImmutableSet<ImmutableList<ImmutableCorrelation>> _options;
    private ImmutableIntValueMap<ImmutableCorrelation> _knownCorrelations;

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
     * @param concept Optional concept where this correlation array will be attached to, or {@link #NO_CONCEPT} if none.
     * @param texts Texts entered by the user in the WordEditorActivity.
     */
    public static void open(Activity activity, int requestCode, int concept, Correlation texts) {
        final int mapSize = texts.size();
        final int[] alphabets = new int[mapSize];
        final String[] str = new String[mapSize];

        for (int i = 0; i < mapSize; i++) {
            alphabets[i] = texts.keyAt(i).key;
            str[i] = texts.valueAt(i);
        }

        final Intent intent = new Intent(activity, CorrelationPickerActivity.class);
        intent.putExtra(ArgKeys.ALPHABETS, alphabets);
        if (concept != NO_CONCEPT) {
            intent.putExtra(ArgKeys.CONCEPT, concept);
        }

        intent.putExtra(ArgKeys.TEXTS, str);
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
    public static void open(Activity activity, int requestCode, Correlation texts, int acceptation) {
        final int mapSize = texts.size();
        final int[] alphabets = new int[mapSize];
        final String[] str = new String[mapSize];

        for (int i = 0; i < mapSize; i++) {
            alphabets[i] = texts.keyAt(i).key;
            str[i] = texts.valueAt(i);
        }

        final Intent intent = new Intent(activity, CorrelationPickerActivity.class);
        intent.putExtra(ArgKeys.ACCEPTATION, acceptation);
        intent.putExtra(ArgKeys.ALPHABETS, alphabets);
        intent.putExtra(ArgKeys.TEXTS, str);
        activity.startActivityForResult(intent, requestCode);
    }

    private ImmutableCorrelation getTexts() {
        final Bundle extras = getIntent().getExtras();
        final int[] alphabets = extras.getIntArray(ArgKeys.ALPHABETS);
        final String[] texts = extras.getStringArray(ArgKeys.TEXTS);

        if (alphabets == null || texts == null || alphabets.length != texts.length) {
            throw new AssertionError();
        }

        final ImmutableCorrelation.Builder builder = new ImmutableCorrelation.Builder();
        for (int i = 0; i < alphabets.length; i++) {
            builder.put(new AlphabetId(alphabets[i]), texts[i]);
        }

        return builder.build();
    }

    private ImmutableIntValueMap<ImmutableCorrelation> findExistingCorrelations() {
        final ImmutableSet.Builder<ImmutableCorrelation> correlationsBuilder = new ImmutableHashSet.Builder<>();
        for (ImmutableList<ImmutableCorrelation> option : _options) {
            for (ImmutableCorrelation correlation : option) {
                correlationsBuilder.add(correlation);
            }
        }
        final ImmutableSet<ImmutableCorrelation> correlations = correlationsBuilder.build();

        final ImmutableIntValueHashMap.Builder<ImmutableCorrelation> builder = new ImmutableIntValueHashMap.Builder<>();
        for (ImmutableCorrelation correlation : correlations) {
            final Integer id = DbManager.getInstance().getManager().findCorrelation(correlation);
            if (id != null) {
                builder.put(correlation, id);
            }
        }

        return builder.build();
    }

    private int findSuggestedPosition() {
        final ImmutableSet<ImmutableCorrelation> known = _knownCorrelations.keySet();
        final IntResultFunction<ImmutableList<ImmutableCorrelation>> func = option -> option.filter(known::contains).size();
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

        final ImmutableCorrelation texts = getTexts();
        _options = texts.checkPossibleCorrelationArrays();
        _knownCorrelations = findExistingCorrelations();
        final int suggestedPosition = findSuggestedPosition();

        if (savedInstanceState != null) {
            _selection = savedInstanceState.getInt(SavedKeys.SELECTION, ListView.INVALID_POSITION);
        }

        if (savedInstanceState == null && _options.size() == 1) {
            _selection = 0;
            completeCorrelationPickingTask();
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

    private int addAcceptation(LangbookManager manager) {
        int concept = getIntent().getIntExtra(ArgKeys.CONCEPT, NO_CONCEPT);
        if (concept == NO_CONCEPT) {
            concept = manager.getMaxConcept() + 1;
        }

        return manager.addAcceptation(concept, _options.valueAt(_selection));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_BUNCHES) {
            if (resultCode == RESULT_OK && data != null) {
                final int[] bunchSet = data.getIntArrayExtra(MatchingBunchesPickerActivity.ResultKeys.BUNCH_SET);
                final LangbookManager manager = DbManager.getInstance().getManager();
                if (manager.allValidAlphabets(getTexts())) {
                    final int accId = addAcceptation(manager);
                    for (int bunch : bunchSet) {
                        manager.addAcceptationInBunch(bunch, accId);
                    }
                    Toast.makeText(this, R.string.newAcceptationFeedback, Toast.LENGTH_SHORT).show();

                    final Intent intent = new Intent();
                    intent.putExtra(ResultKeys.ACCEPTATION, accId);
                    setResult(RESULT_OK, intent);
                    finish();
                }
                else {
                    final ImmutableList<ImmutableCorrelation> array = _options.valueAt(_selection);

                    final Intent intent = new Intent();
                    intent.putExtra(ResultKeys.CORRELATION_ARRAY, new ParcelableCorrelationArray(array));
                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
            else if (_options.size() == 1) {
                finish();
            }
        }
    }

    private void completeCorrelationPickingTask() {
        final int existingAcceptation = getIntent().getIntExtra(ArgKeys.ACCEPTATION, NO_ACCEPTATION);
        if (existingAcceptation == NO_ACCEPTATION) {
            MatchingBunchesPickerActivity.open(this, REQUEST_CODE_PICK_BUNCHES, getTexts());
        }
        else {
            DbManager.getInstance().getManager().updateAcceptationCorrelationArray(existingAcceptation, _options.valueAt(_selection));
            setResult(RESULT_OK);
            finish();
        }
    }

    @Override
    public void onClick(View view) {
        _selection = _listView.getCheckedItemPosition();
        if (_selection != ListView.INVALID_POSITION) {
            completeCorrelationPickingTask();
        }
        else {
            Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (_selection >= 0) {
            outState.putInt(SavedKeys.SELECTION, _selection);
        }
    }
}
