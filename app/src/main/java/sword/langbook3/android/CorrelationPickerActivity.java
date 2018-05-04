package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Iterator;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntResultFunction;
import sword.langbook3.android.db.DbImporter;
import sword.langbook3.android.db.DbInsertQuery;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.sdb.StreamedDatabaseConstants;
import sword.langbook3.android.sdb.StreamedDatabaseReader;

public final class CorrelationPickerActivity extends Activity implements View.OnClickListener {

    private interface BundleKeys {
        String ALPHABETS = "alphabets";
        String TEXTS = "texts";
    }

    private ListView _listView;
    private ImmutableSet<ImmutableList<ImmutableIntKeyMap<String>>> _options;
    private ImmutableIntValueMap<ImmutableIntKeyMap<String>> _knownCorrelations;

    public static void open(Activity activity, int requestCode, IntKeyMap<String> texts) {
        final int mapSize = texts.size();
        final int[] alphabets = new int[mapSize];
        final String[] str = new String[mapSize];

        for (int i = 0; i < mapSize; i++) {
            alphabets[i] = texts.keyAt(i);
            str[i] = texts.valueAt(i);
        }

        final Intent intent = new Intent(activity, CorrelationPickerActivity.class);
        intent.putExtra(BundleKeys.ALPHABETS, alphabets);
        intent.putExtra(BundleKeys.TEXTS, str);
        activity.startActivityForResult(intent, requestCode);
    }

    private ImmutableIntKeyMap<String> getTexts() {
        final Bundle extras = getIntent().getExtras();
        final int[] alphabets = extras.getIntArray(BundleKeys.ALPHABETS);
        final String[] texts = extras.getStringArray(BundleKeys.TEXTS);

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

    private static Integer findCorrelation(IntKeyMap<String> correlation) {
        if (correlation.size() == 0) {
            return StreamedDatabaseConstants.nullCorrelationId;
        }
        final ImmutableIntKeyMap<String> corr = correlation.toImmutable();

        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbolArrays = LangbookDbSchema.Tables.symbolArrays;

        final int offset = table.columns().size();
        final int offset2 = offset + symbolArrays.columns().size();
        final int offset3 = offset2 + table.columns().size();

        final DbQuery query = new DbQuery.Builder(table)
                .join(symbolArrays, table.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .where(table.getAlphabetColumnIndex(), corr.keyAt(0))
                .where(offset + symbolArrays.getStrColumnIndex(), corr.valueAt(0))
                .join(table, table.getCorrelationIdColumnIndex(), table.getCorrelationIdColumnIndex())
                .join(symbolArrays, offset2 + table.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .select(
                        table.getCorrelationIdColumnIndex(),
                        offset2 + table.getAlphabetColumnIndex(),
                        offset3 + symbolArrays.getStrColumnIndex());
        final DbResult result = DbManager.getInstance().attach(query).iterator();
        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                int correlationId = row.get(0).toInt();
                ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
                builder.put(row.get(1).toInt(), row.get(2).toText());

                while (result.hasNext()) {
                    row = result.next();
                    int newCorrelationId = row.get(0).toInt();
                    if (newCorrelationId != correlationId) {
                        if (builder.build().equals(corr)) {
                            return correlationId;
                        }

                        correlationId = newCorrelationId;
                        builder = new ImmutableIntKeyMap.Builder<>();
                    }

                    builder.put(row.get(1).toInt(), row.get(2).toText());
                }

                if (builder.build().equals(corr)) {
                    return correlationId;
                }
            }
        }
        finally {
            result.close();
        }

        return null;
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
            final Integer id = findCorrelation(correlation);
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

    private int insertCorrelation(IntKeyMap<String> correlation) {
        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbManager manager = DbManager.getInstance();
        final DbImporter.Database db = manager.getDatabase();
        final DbQuery maxQuery = new DbQuery.Builder(table)
                .select(DbQuery.max(table.getCorrelationIdColumnIndex()));
        final Iterator<DbResult.Row> it = db.select(maxQuery);
        final int id = it.next().get(0).toInt() + 1;

        if (it.hasNext()) {
            throw new AssertionError();
        }

        for (IntKeyMap.Entry<String> entry : correlation.entries()) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getCorrelationIdColumnIndex(), id)
                    .put(table.getAlphabetColumnIndex(), entry.key())
                    .put(table.getSymbolArrayColumnIndex(), StreamedDatabaseReader.obtainSymbolArray(db, entry.value()))
                    .build();
            manager.insert(query);
        }

        return id;
    }

    private Integer findCorrelationArray(ImmutableIntList array) {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getArrayIdColumnIndex(), table.getArrayIdColumnIndex())
                .where(table.getArrayPositionColumnIndex(), 0)
                .where(table.getCorrelationColumnIndex(), array.get(0))
                .select(table.getArrayIdColumnIndex(), table.columns().size() + table.getCorrelationColumnIndex());
        final DbResult result = DbManager.getInstance().getDatabase().select(query);
        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                int arrayId = row.get(0).toInt();
                ImmutableIntList.Builder builder = new ImmutableIntList.Builder();
                builder.add(row.get(1).toInt());

                while (result.hasNext()) {
                    row = result.next();
                    int newArrayId = row.get(0).toInt();
                    if (arrayId != newArrayId) {
                        if (builder.build().equals(array)) {
                            return arrayId;
                        }

                        arrayId = newArrayId;
                        builder = new ImmutableIntList.Builder();
                    }
                    builder.add(row.get(1).toInt());
                }

                if (builder.build().equals(array)) {
                    return arrayId;
                }
            }
        }
        finally {
            result.close();
        }

        return null;
    }

    private int insertCorrelationArray(ImmutableIntList array) {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbManager manager = DbManager.getInstance();
        final DbImporter.Database db = manager.getDatabase();
        final DbQuery maxQuery = new DbQuery.Builder(table)
                .select(DbQuery.max(table.getArrayIdColumnIndex()));
        final Iterator<DbResult.Row> it = db.select(maxQuery);
        final int id = it.next().get(0).toInt() + 1;

        if (it.hasNext()) {
            throw new AssertionError();
        }

        int index = 0;
        for (int value : array) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getArrayIdColumnIndex(), id)
                    .put(table.getArrayPositionColumnIndex(), index++)
                    .put(table.getCorrelationColumnIndex(), value)
                    .build();
            manager.insert(query);
        }

        return id;
    }

    private int insertAcceptation(int arrayId) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbManager manager = DbManager.getInstance();
        final DbImporter.Database db = manager.getDatabase();
        final DbQuery maxQuery = new DbQuery.Builder(table)
                .select(DbQuery.max(table.getWordColumnIndex()), DbQuery.max(table.getConceptColumnIndex()));
        final Iterator<DbResult.Row> it = db.select(maxQuery);
        final DbResult.Row maxRow = it.next();
        final int word = maxRow.get(0).toInt() + 1;
        final int concept = maxRow.get(1).toInt() + 1;

        if (it.hasNext()) {
            throw new AssertionError();
        }

        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getWordColumnIndex(), word)
                .put(table.getConceptColumnIndex(), concept)
                .put(table.getCorrelationArrayColumnIndex(), arrayId)
                .build();
        return manager.insert(query);
    }

    private void insertSearchQueries(int accId, ImmutableList<ImmutableIntKeyMap<String>> array) {
        final ImmutableIntSet alphabets = array.get(0).keySet();
        if (array.anyMatch(map -> !map.keySet().equals(alphabets))) {
            throw new AssertionError();
        }

        final ImmutableIntKeyMap.Builder<String> mapBuilder = new ImmutableIntKeyMap.Builder<>();
        for (int alphabet : alphabets) {
            mapBuilder.put(alphabet, array.map(map -> map.get(alphabet)).reduce((a,b) -> a + b));
        }
        final ImmutableIntKeyMap<String> map = mapBuilder.build();

        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        for (int alphabet : alphabets) {
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
            ImmutableList<ImmutableIntKeyMap<String>> array = _options.valueAt(selection);
            boolean correlationInserted = false;
            final ImmutableIntList.Builder arrayBuilder = new ImmutableIntList.Builder();
            for (ImmutableIntKeyMap<String> correlation : array) {
                int id = _knownCorrelations.get(correlation, StreamedDatabaseConstants.nullCorrelationId);
                if (id == StreamedDatabaseConstants.nullCorrelationId) {
                    id = insertCorrelation(correlation);
                    correlationInserted = true;
                }
                arrayBuilder.add(id);
            }

            final ImmutableIntList idArray = arrayBuilder.build();
            final int arrayId;
            if (!correlationInserted) {
                final Integer arrayIdOpt = findCorrelationArray(idArray);
                arrayId = (arrayIdOpt == null)? insertCorrelationArray(idArray) : arrayIdOpt;
            }
            else {
                arrayId = insertCorrelationArray(idArray);
            }

            final int accId = insertAcceptation(arrayId);
            insertSearchQueries(accId, array);
            Toast.makeText(this, "New acceptation inserted: " + accId, Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, "Please select an option", Toast.LENGTH_SHORT).show();
        }
    }
}
