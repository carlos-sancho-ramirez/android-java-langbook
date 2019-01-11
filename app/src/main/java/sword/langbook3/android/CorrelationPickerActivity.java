package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Iterator;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntValueHashMap;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntResultFunction;
import sword.langbook3.android.db.Database;
import sword.langbook3.android.sdb.StreamedDatabaseConstants;

import static sword.langbook3.android.LangbookDatabase.addAcceptationInBunch;
import static sword.langbook3.android.LangbookDatabase.insertCorrelation;
import static sword.langbook3.android.LangbookDatabase.insertCorrelationArray;
import static sword.langbook3.android.LangbookDatabase.obtainCorrelationArray;
import static sword.langbook3.android.LangbookDatabase.updateAcceptationCorrelationArray;
import static sword.langbook3.android.LangbookReadableDatabase.findCorrelation;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxConceptInAcceptations;

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
    }

    private int _selection = ListView.INVALID_POSITION;

    private ListView _listView;
    private ImmutableSet<ImmutableList<ImmutableIntKeyMap<String>>> _options;
    private ImmutableIntValueMap<ImmutableIntKeyMap<String>> _knownCorrelations;

    public static void open(Activity activity, int requestCode, int concept, IntKeyMap<String> texts) {
        final int mapSize = texts.size();
        final int[] alphabets = new int[mapSize];
        final String[] str = new String[mapSize];

        for (int i = 0; i < mapSize; i++) {
            alphabets[i] = texts.keyAt(i);
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
     * @param activity Activity that opens this activity.
     * @param requestCode Request code that will be used on {@link Activity#onActivityResult(int, int, android.content.Intent)}.
     * @param texts Texts entered by the user in the WordEditorActivity.
     * @param acceptation Identifier for an existing acceptation that will be modified after selecting the correlation array.
     */
    public static void open(Activity activity, int requestCode, IntKeyMap<String> texts, int acceptation) {
        final int mapSize = texts.size();
        final int[] alphabets = new int[mapSize];
        final String[] str = new String[mapSize];

        for (int i = 0; i < mapSize; i++) {
            alphabets[i] = texts.keyAt(i);
            str[i] = texts.valueAt(i);
        }

        final Intent intent = new Intent(activity, CorrelationPickerActivity.class);
        intent.putExtra(ArgKeys.ACCEPTATION, acceptation);
        intent.putExtra(ArgKeys.ALPHABETS, alphabets);
        intent.putExtra(ArgKeys.TEXTS, str);
        activity.startActivityForResult(intent, requestCode);
    }

    private ImmutableIntKeyMap<String> getTexts() {
        final Bundle extras = getIntent().getExtras();
        final int[] alphabets = extras.getIntArray(ArgKeys.ALPHABETS);
        final String[] texts = extras.getStringArray(ArgKeys.TEXTS);

        if (alphabets == null || texts == null || alphabets.length != texts.length) {
            throw new AssertionError();
        }

        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        for (int i = 0; i < alphabets.length; i++) {
            builder.put(alphabets[i], texts[i]);
        }

        return builder.build();
    }

    private void checkPossibleCorrelationArraysRecursive(
            ImmutableSet.Builder<ImmutableList<ImmutableIntKeyMap<String>>> builder,
            ImmutableIntKeyMap<String> remaining,
            ImmutableIntKeyMap<String> left,
            ImmutableIntKeyMap<String> right) {
        final int remainingSize = remaining.size();
        if (remainingSize == 0) {
            for (ImmutableList<ImmutableIntKeyMap<String>> array : checkPossibleCorrelationArrays(right)) {
                builder.add(array.prepend(left));
            }
        }
        else {
            final int firstAlphabet = remaining.keyAt(0);
            final String firstText = remaining.valueAt(0);

            // TODO: Change this to global.skip(1) when available
            final ImmutableIntKeyMap.Builder<String> tailBuilder = new ImmutableIntKeyMap.Builder<>();
            for (int i = 1; i < remainingSize; i++) {
                tailBuilder.put(remaining.keyAt(i), remaining.valueAt(i));
            }
            final ImmutableIntKeyMap<String> tail = tailBuilder.build();

            final int firstTextSize = firstText.length();
            for (int i = 1; i < firstTextSize; i++) {
                final ImmutableIntKeyMap<String> newLeft = left.put(firstAlphabet, firstText.substring(0, i));
                final ImmutableIntKeyMap<String> newRight = right.put(firstAlphabet, firstText.substring(i));
                checkPossibleCorrelationArraysRecursive(builder, tail, newLeft, newRight);
            }
        }
    }

    private static boolean entryLessThan(ImmutableList<ImmutableIntKeyMap<String>> a, ImmutableList<ImmutableIntKeyMap<String>> b) {
        final Iterator<ImmutableIntKeyMap<String>> itA = a.iterator();
        final Iterator<ImmutableIntKeyMap<String>> itB = b.iterator();

        while (itA.hasNext() && itB.hasNext()) {
            ImmutableIntKeyMap<String> headA = itA.next();
            ImmutableIntKeyMap<String> headB = itB.next();

            for (int i = 0; i < headA.size(); i++) {
                final int alphabet = headA.keyAt(i);
                if (headB.size() == i) {
                    return false;
                }

                final int alphabetB = headB.keyAt(i);

                if (alphabet < alphabetB) {
                    return true;
                }
                else if (alphabet > alphabetB) {
                    return false;
                }

                final String textA = headA.valueAt(i);
                final String textB = headB.valueAt(i);
                if (textA.length() < textB.length()) {
                    return true;
                }
                else if (textA.length() > textB.length()) {
                    return false;
                }
            }
        }

        return itB.hasNext();
    }

    private ImmutableSet<ImmutableList<ImmutableIntKeyMap<String>>> checkPossibleCorrelationArrays(ImmutableIntKeyMap<String> global) {
        final int globalSize = global.size();
        final IntResultFunction<String> lengthFunc = text -> (text == null)? 0 : text.length();
        final ImmutableIntPairMap lengths = global.mapToInt(lengthFunc);
        if (globalSize == 0 || lengths.anyMatch(length -> length <= 0)) {
            return ImmutableHashSet.empty();
        }

        final ImmutableSet.Builder<ImmutableList<ImmutableIntKeyMap<String>>> builder = new ImmutableHashSet.Builder<>();
        builder.add(new ImmutableList.Builder<ImmutableIntKeyMap<String>>().add(global).build());

        if (globalSize > 1) {
            checkPossibleCorrelationArraysRecursive(builder, global, ImmutableIntKeyMap.empty(), ImmutableIntKeyMap.empty());
        }
        return builder.build().sort(CorrelationPickerActivity::entryLessThan);
    }

    private ImmutableIntValueMap<ImmutableIntKeyMap<String>> findExistingCorrelations() {
        final ImmutableSet.Builder<ImmutableIntKeyMap<String>> correlationsBuilder = new ImmutableHashSet.Builder<>();
        for (ImmutableList<ImmutableIntKeyMap<String>> option : _options) {
            for (ImmutableIntKeyMap<String> correlation : option) {
                correlationsBuilder.add(correlation);
            }
        }
        final ImmutableSet<ImmutableIntKeyMap<String>> correlations = correlationsBuilder.build();

        final ImmutableIntValueHashMap.Builder<ImmutableIntKeyMap<String>> builder = new ImmutableIntValueHashMap.Builder<>();
        for (ImmutableIntKeyMap<String> correlation : correlations) {
            final Integer id = findCorrelation(DbManager.getInstance().getDatabase(), correlation);
            if (id != null) {
                builder.put(correlation, id);
            }
        }

        return builder.build();
    }

    private int findSuggestedPosition() {
        final ImmutableSet<ImmutableIntKeyMap<String>> known = _knownCorrelations.keySet();
        final IntResultFunction<ImmutableList<ImmutableIntKeyMap<String>>> func = option -> option.filter(known::contains).size();
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

        final ImmutableIntKeyMap<String> texts = getTexts();
        _options = checkPossibleCorrelationArrays(texts);
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

    private int addCorrelationArray(Database db) {
        ImmutableList<ImmutableIntKeyMap<String>> array = _options.valueAt(_selection);
        boolean correlationInserted = false;
        final ImmutableIntList.Builder arrayBuilder = new ImmutableIntList.Builder();
        for (ImmutableIntKeyMap<String> correlation : array) {
            int id = _knownCorrelations.get(correlation, StreamedDatabaseConstants.nullCorrelationId);
            if (id == StreamedDatabaseConstants.nullCorrelationId) {
                id = insertCorrelation(db, correlation);
                correlationInserted = true;
            }
            arrayBuilder.add(id);
        }

        final ImmutableIntList idArray = arrayBuilder.build();
        return correlationInserted? insertCorrelationArray(db, idArray) : obtainCorrelationArray(db, idArray);
    }

    private int addAcceptation(Database db) {
        int concept = getIntent().getIntExtra(ArgKeys.CONCEPT, NO_CONCEPT);
        if (concept == NO_CONCEPT) {
            concept = getMaxConceptInAcceptations(db) + 1;
        }

        return LangbookDatabase.addAcceptation(db, concept, addCorrelationArray(db));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_BUNCHES && resultCode == RESULT_OK && data != null) {
            final int[] bunchSet = data.getIntArrayExtra(MatchingBunchesPickerActivity.ResultKeys.BUNCH_SET);
            final Database db = DbManager.getInstance().getDatabase();
            final int accId = addAcceptation(db);
            for (int bunch : bunchSet) {
                addAcceptationInBunch(db, bunch, accId);
            }
            Toast.makeText(this, R.string.newAcceptationFeedback, Toast.LENGTH_SHORT).show();

            final Intent intent = new Intent();
            intent.putExtra(ResultKeys.ACCEPTATION, accId);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void completeCorrelationPickingTask() {
        final int existingAcceptation = getIntent().getIntExtra(ArgKeys.ACCEPTATION, NO_ACCEPTATION);
        if (existingAcceptation == NO_ACCEPTATION) {
            MatchingBunchesPickerActivity.open(this, REQUEST_CODE_PICK_BUNCHES, getTexts());
        }
        else {
            final Database db = DbManager.getInstance().getDatabase();
            updateAcceptationCorrelationArray(db, existingAcceptation, addCorrelationArray(db));
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
