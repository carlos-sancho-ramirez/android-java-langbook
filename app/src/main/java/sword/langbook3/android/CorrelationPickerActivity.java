package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import sword.collections.Function;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntResultFunction;
import sword.collections.IntSet;
import sword.collections.MutableIntSet;
import sword.langbook3.android.db.Database;
import sword.langbook3.android.db.DbImporter;
import sword.langbook3.android.db.DbInsertQuery;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.sdb.StreamedDatabaseConstants;

import static sword.langbook3.android.LangbookDatabase.insertCorrelation;
import static sword.langbook3.android.LangbookDatabase.insertCorrelationArray;
import static sword.langbook3.android.LangbookReadableDatabase.findCorrelation;
import static sword.langbook3.android.LangbookReadableDatabase.findCorrelationArray;
import static sword.langbook3.android.LangbookReadableDatabase.getConversion;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxConceptInAcceptations;
import static sword.langbook3.android.WordEditorActivity.convertText;

public final class CorrelationPickerActivity extends Activity implements View.OnClickListener {

    static final int NO_CONCEPT = 0;

    interface ArgKeys {
        String ALPHABETS = BundleKeys.ALPHABETS;
        String CONCEPT = BundleKeys.CONCEPT;
        String TEXTS = BundleKeys.TEXTS;
    }

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

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

    private ImmutableSet<ImmutableList<ImmutableIntKeyMap<String>>> checkPossibleCorrelationArrays(ImmutableIntKeyMap<String> global) {
        final int globalSize = global.size();
        final IntResultFunction<String> lengthFunc = text -> (text == null)? 0 : text.length();
        final ImmutableIntPairMap lengths = global.map(lengthFunc);
        if (globalSize == 0 || lengths.anyMatch(length -> length <= 0)) {
            return ImmutableSet.empty();
        }

        final ImmutableSet.Builder<ImmutableList<ImmutableIntKeyMap<String>>> builder = new ImmutableSet.Builder<>();
        builder.add(new ImmutableList.Builder<ImmutableIntKeyMap<String>>().add(global).build());

        if (globalSize > 1) {
            checkPossibleCorrelationArraysRecursive(builder, global, ImmutableIntKeyMap.empty(), ImmutableIntKeyMap.empty());
        }
        return builder.build();
    }

    private ImmutableIntValueMap<ImmutableIntKeyMap<String>> findExistingCorrelations() {
        final ImmutableSet.Builder<ImmutableIntKeyMap<String>> correlationsBuilder = new ImmutableSet.Builder<>();
        for (ImmutableList<ImmutableIntKeyMap<String>> option : _options) {
            for (ImmutableIntKeyMap<String> correlation : option) {
                correlationsBuilder.add(correlation);
            }
        }
        final ImmutableSet<ImmutableIntKeyMap<String>> correlations = correlationsBuilder.build();

        final ImmutableIntValueMap.Builder<ImmutableIntKeyMap<String>> builder = new ImmutableIntValueMap.Builder<>();
        for (ImmutableIntKeyMap<String> correlation : correlations) {
            final Integer id = findCorrelation(DbManager.getInstance().getDatabase(), correlation);
            if (id != null) {
                builder.put(correlation, id);
            }
        }

        return builder.build();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.correlation_picker_activity);

        _options = checkPossibleCorrelationArrays(getTexts());
        _knownCorrelations = findExistingCorrelations();

        _listView = findViewById(R.id.listView);
        _listView.setAdapter(new CorrelationPickerAdapter(_options, _knownCorrelations.keySet()));
        _listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        findViewById(R.id.nextButton).setOnClickListener(this);
    }

    private int insertAcceptation(int arrayId, int concept) {
        final DbImporter.Database db = DbManager.getInstance().getDatabase();
        if (concept == NO_CONCEPT) {
            concept = getMaxConceptInAcceptations(db) + 1;
        }

        return LangbookDbInserter.insertAcceptation(db, concept, arrayId);
    }

    private ImmutableIntPairMap findConversions(IntSet alphabets) {
        final LangbookDbSchema.ConversionsTable conversions = LangbookDbSchema.Tables.conversions;

        final DbQuery query = new DbQuery.Builder(conversions)
                .groupBy(conversions.getSourceAlphabetColumnIndex(), conversions.getTargetAlphabetColumnIndex())
                .select(
                        conversions.getSourceAlphabetColumnIndex(),
                        conversions.getTargetAlphabetColumnIndex());

        final MutableIntSet foundAlphabets = MutableIntSet.empty();
        final ImmutableIntPairMap.Builder builder = new ImmutableIntPairMap.Builder();
        for (DbResult.Row row : DbManager.getInstance().attach(query)) {
            final int source = row.get(0).toInt();
            final int target = row.get(1).toInt();

            if (foundAlphabets.contains(target)) {
                throw new AssertionError();
            }
            foundAlphabets.add(target);

            if (alphabets.contains(source)) {
                builder.put(source, target);
            }
        }

        return builder.build();
    }

    private void insertSearchQueries(int accId, ImmutableList<ImmutableIntKeyMap<String>> array) {
        final ImmutableIntSet alphabets = array.get(0).keySet();
        if (array.anyMatch(map -> !map.keySet().equals(alphabets))) {
            throw new AssertionError();
        }

        final ImmutableIntPairMap foundConversions = findConversions(alphabets);
        final ImmutableIntKeyMap.Builder<String> mapBuilder = new ImmutableIntKeyMap.Builder<>();
        for (int alphabet : alphabets) {
            final Function<ImmutableIntKeyMap<String>, String> mapFunc = map -> map.get(alphabet);
            final String text = array.map(mapFunc).reduce((a,b) -> a + b);
            mapBuilder.put(alphabet, text);

            if (foundConversions.keySet().contains(alphabet)) {
                final int targetAlphabet = foundConversions.get(alphabet);
                ImmutableList<ImmutablePair<String, String>> conversion = getConversion(DbManager.getInstance().getDatabase(), new ImmutableIntPair(alphabet, targetAlphabet));
                final String convertedText = convertText(conversion, text);
                if (convertedText == null) {
                    throw new AssertionError();
                }

                mapBuilder.put(targetAlphabet, convertedText);
            }
        }
        final ImmutableIntKeyMap<String> map = mapBuilder.build();

        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        for (int alphabet : map.keySet()) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getMainAcceptationColumnIndex(), accId)
                    .put(table.getDynamicAcceptationColumnIndex(), accId)
                    .put(table.getStringAlphabetColumnIndex(), alphabet)
                    .put(table.getStringColumnIndex(), map.get(alphabet))
                    .put(table.getMainStringColumnIndex(), map.valueAt(0))
                    .build();
            DbManager.getInstance().insert(query);
        }
    }

    @Override
    public void onClick(View view) {
        final int selection = _listView.getCheckedItemPosition();
        if (selection != ListView.INVALID_POSITION) {
            final Database db = DbManager.getInstance().getDatabase();
            ImmutableList<ImmutableIntKeyMap<String>> array = _options.valueAt(selection);
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
            final int arrayId;
            if (!correlationInserted) {
                final Integer arrayIdOpt = findCorrelationArray(db, idArray);
                arrayId = (arrayIdOpt == null)? insertCorrelationArray(db, idArray) : arrayIdOpt;
            }
            else {
                arrayId = insertCorrelationArray(db, idArray);
            }

            final int accId = insertAcceptation(arrayId, getIntent().getIntExtra(ArgKeys.CONCEPT, NO_CONCEPT));
            insertSearchQueries(accId, array);
            Toast.makeText(this, R.string.newAcceptationFeedback, Toast.LENGTH_SHORT).show();

            final Intent intent = new Intent();
            intent.putExtra(ResultKeys.ACCEPTATION, accId);
            setResult(RESULT_OK, intent);
            finish();
        }
        else {
            Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show();
        }
    }
}
