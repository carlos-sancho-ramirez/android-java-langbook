package sword.langbook3.android;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static sword.langbook3.android.DbManager.idColumnName;

public class QuizSelectionActivity extends Activity implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    private static final class BundleKeys {
        static final String BUNCH = "b";
    }

    // Specifies the alphabet the user would like to see if possible.
    // TODO: This should be a shared preference
    static final int preferredAlphabet = AcceptationDetailsActivity.preferredAlphabet;

    public static void open(Context context, int bunch) {
        Intent intent = new Intent(context, QuizSelectionActivity.class);
        intent.putExtra(BundleKeys.BUNCH, bunch);
        context.startActivity(intent);
    }

    private static class QuizTypeAdapter extends BaseAdapter {

        private final String[] _entries;
        private LayoutInflater _inflater;

        QuizTypeAdapter(String[] entries) {
            _entries = entries;
        }

        @Override
        public int getCount() {
            return _entries.length;
        }

        @Override
        public String getItem(int position) {
            return _entries[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                if (_inflater == null) {
                    _inflater = LayoutInflater.from(parent.getContext());
                }

                view = _inflater.inflate(R.layout.quiz_type_item, parent, false);
            }
            else {
                view = convertView;
            }

            final TextView textView = view.findViewById(R.id.itemTextView);
            textView.setText(_entries[position]);

            return view;
        }
    }

    private static final class AdapterItem {

        final int id;
        final String name;

        AdapterItem(int id, String name) {
            if (name == null) {
                throw new IllegalArgumentException();
            }

            this.id = id;
            this.name = name;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof AdapterItem)) {
                return false;
            }

            AdapterItem that = (AdapterItem) other;
            return id == that.id && name.equals(that.name);
        }
    }

    private static class AlphabetAdapter extends BaseAdapter {

        private final AdapterItem[] _entries;
        private LayoutInflater _inflater;

        AlphabetAdapter(AdapterItem[] entries) {
            _entries = entries;
        }

        @Override
        public int getCount() {
            return _entries.length;
        }

        @Override
        public AdapterItem getItem(int position) {
            return _entries[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                if (_inflater == null) {
                    _inflater = LayoutInflater.from(parent.getContext());
                }

                view = _inflater.inflate(R.layout.quiz_type_item, parent, false);
            }
            else {
                view = convertView;
            }

            final TextView textView = view.findViewById(R.id.itemTextView);
            textView.setText(_entries[position].name);

            return view;
        }
    }

    private static final class AlphabetPair {

        final int source;
        final int target;

        AlphabetPair(int source, int target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public int hashCode() {
            return source * 31 + target;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof AlphabetPair)) {
                return false;
            }

            AlphabetPair that = (AlphabetPair) other;
            return source == that.source && target == that.target;
        }
    }

    static final class StringPair {

        final String source;
        final String target;

        StringPair(String source, String target) {
            this.source = source;
            this.target = target;
        }
    }

    private SparseArray<String> readAllAlphabets(SQLiteDatabase db) {
        final DbManager.AlphabetsTable alphabets = DbManager.Tables.alphabets;
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J0." + idColumnName +
                        ",J2." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J2." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + alphabets.getName() + " AS J0" +
                        " JOIN " + acceptations.getName() + " AS J1 ON J0." + idColumnName + "=J1." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J2 ON J1." + idColumnName + "=J2." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                        " ORDER BY J0." + idColumnName, null);

        final SparseArray<String> result = new SparseArray<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int alphabet = cursor.getInt(0);
                    int textAlphabet = cursor.getInt(1);
                    String text = cursor.getString(2);

                    while (cursor.moveToNext()) {
                        if (alphabet == cursor.getInt(0)) {
                            if (textAlphabet != preferredAlphabet && cursor.getInt(1) == preferredAlphabet) {
                                textAlphabet = preferredAlphabet;
                                text = cursor.getString(2);
                            }
                        }
                        else {
                            result.put(alphabet, text);

                            alphabet = cursor.getInt(0);
                            textAlphabet = cursor.getInt(1);
                            text = cursor.getString(2);
                        }
                    }

                    result.put(alphabet, text);
                }
            }
            finally {
                cursor.close();
            }
        }

        return result;
    }

    private Map<AlphabetPair, StringPair> readInterAlphabetPossibleSourceAlphabets(SQLiteDatabase db) {
        final DbManager.AlphabetsTable alphabets = DbManager.Tables.alphabets;
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J0." + idColumnName +
                        ",J1." + idColumnName +
                        ",J3." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J3." + strings.getColumnName(strings.getStringColumnIndex()) +
                        ",J5." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J5." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + alphabets.getName() + " AS J0" +
                        " JOIN " + alphabets.getName() + " AS J1 ON J0." + alphabets.getColumnName(alphabets.getLanguageColumnIndex()) + "=J1." + alphabets.getColumnName(alphabets.getLanguageColumnIndex()) +
                        " JOIN " + acceptations.getName() + " AS J2 ON J0." + idColumnName + "=J2." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J3 ON J2." + idColumnName + "=J3." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                        " JOIN " + acceptations.getName() + " AS J4 ON J1." + idColumnName + "=J4." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J5 ON J4." + idColumnName + "=J5." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                " WHERE J0." + idColumnName + "!=J1." + idColumnName +
                " ORDER BY J0." + idColumnName + ",J1." + idColumnName, null);

        final HashMap<AlphabetPair, StringPair> result = new HashMap<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int sourceAlphabet = cursor.getInt(0);
                    int targetAlphabet = cursor.getInt(1);
                    int sourceTextAlphabet = cursor.getInt(2);
                    String sourceText = cursor.getString(3);
                    int targetTextAlphabet = cursor.getInt(4);
                    String targetText = cursor.getString(5);

                    while (cursor.moveToNext()) {
                        if (sourceAlphabet == cursor.getInt(0) && targetAlphabet == cursor.getInt(1)) {
                            if (sourceTextAlphabet != preferredAlphabet && cursor.getInt(2) == preferredAlphabet) {
                                sourceTextAlphabet = preferredAlphabet;
                                sourceText = cursor.getString(3);
                            }

                            if (targetTextAlphabet != preferredAlphabet && cursor.getInt(4) == preferredAlphabet) {
                                targetTextAlphabet = preferredAlphabet;
                                targetText = cursor.getString(5);
                            }
                        }
                        else {
                            final AlphabetPair key = new AlphabetPair(sourceAlphabet, targetAlphabet);
                            result.put(key, new StringPair(sourceText, targetText));

                            sourceAlphabet = cursor.getInt(0);
                            targetAlphabet = cursor.getInt(1);
                            sourceTextAlphabet = cursor.getInt(2);
                            sourceText = cursor.getString(3);
                            targetTextAlphabet = cursor.getInt(4);
                            targetText = cursor.getString(5);
                        }
                    }

                    final AlphabetPair key = new AlphabetPair(sourceAlphabet, targetAlphabet);
                    result.put(key, new StringPair(sourceText, targetText));
                }
            }
            finally {
                cursor.close();
            }
        }

        return result;
    }

    private SparseArray<String> readAllRules(SQLiteDatabase db) {
        final DbManager.AgentsTable agents = DbManager.Tables.agents;
        final DbManager.AcceptationsTable acceptations = DbManager.Tables.acceptations;
        final DbManager.RuledConceptsTable ruledConcepts = DbManager.Tables.ruledConcepts;
        final DbManager.StringQueriesTable strings = DbManager.Tables.stringQueries;

        Cursor cursor = db.rawQuery(
                "SELECT" +
                        " J1." + agents.getColumnName(agents.getRuleColumnIndex()) +
                        ",J3." + strings.getColumnName(strings.getStringAlphabetColumnIndex()) +
                        ",J3." + strings.getColumnName(strings.getStringColumnIndex()) +
                " FROM " + ruledConcepts.getName() + " AS J0" +
                        " JOIN " + agents.getName() + " AS J1 ON J0." + ruledConcepts.getColumnName(ruledConcepts.getAgentColumnIndex()) + "=J1." + idColumnName +
                        " JOIN " + acceptations.getName() + " AS J2 ON J1." + agents.getColumnName(agents.getRuleColumnIndex()) + "=J2." + acceptations.getColumnName(acceptations.getConceptColumnIndex()) +
                        " JOIN " + strings.getName() + " AS J3 ON J2." + idColumnName + "=J3." + strings.getColumnName(strings.getDynamicAcceptationColumnIndex()) +
                        " ORDER BY J1." + agents.getColumnName(agents.getRuleColumnIndex()), null);

        final SparseArray<String> result = new SparseArray<>();
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int rule = cursor.getInt(0);
                    int textAlphabet = cursor.getInt(1);
                    String text = cursor.getString(2);

                    while (cursor.moveToNext()) {
                        if (rule == cursor.getInt(0)) {
                            if (textAlphabet != preferredAlphabet && cursor.getInt(1) == preferredAlphabet) {
                                textAlphabet = preferredAlphabet;
                                text = cursor.getString(2);
                            }
                        }
                        else {
                            result.put(rule, text);

                            rule = cursor.getInt(0);
                            textAlphabet = cursor.getInt(1);
                            text = cursor.getString(2);
                        }
                    }

                    result.put(rule, text);
                }
            }
            finally {
                cursor.close();
            }
        }

        return result;
    }

    private Spinner _sourceAlphabetSpinner;
    private Spinner _auxSpinner;
    private QuizTypeAdapter _quizTypeAdapter;
    private SparseArray<String> _allAlphabets;
    private SparseArray<String> _allRules;

    static final class QuizTypes {
        static final int interAlphabet = 1;
        static final int translation = 2;
        static final int synonym = 3;
        static final int appliedRule = 4;
    }

    private int _quizType;
    private int _bunch;
    private int _sourceAlphabet;
    private int _aux;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quiz_selector_activity);

        _quizTypeAdapter = new QuizTypeAdapter(new String[] {
                " -- Select type --",
                "inter-alphabet",
                "translation",
                "synonym",
                "applied rule"
        });

        _bunch = getIntent().getIntExtra(BundleKeys.BUNCH, 0);
        final SQLiteDatabase db = DbManager.getInstance().getReadableDatabase();

        if (_bunch != 0) {
            final String bunchText = AcceptationDetailsActivity.readConceptText(db, _bunch);
            final TextView bunchField = findViewById(R.id.bunch);
            bunchField.setText(bunchText);
        }

        final Spinner quizTypeSpinner = findViewById(R.id.quizType);
        quizTypeSpinner.setAdapter(_quizTypeAdapter);
        quizTypeSpinner.setOnItemSelectedListener(this);

        _sourceAlphabetSpinner = findViewById(R.id.sourceAlphabet);
        _sourceAlphabetSpinner.setOnItemSelectedListener(this);

        _auxSpinner = findViewById(R.id.aux);
        _auxSpinner.setOnItemSelectedListener(this);

        findViewById(R.id.startButton).setOnClickListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        switch (adapterView.getId()) {
            case R.id.quizType:
                updateQuizType(position);
                break;

            case R.id.sourceAlphabet:
                _sourceAlphabet = ((AlphabetAdapter) adapterView.getAdapter()).getItem(position).id;
                break;

            case R.id.aux:
                _aux = ((AlphabetAdapter) adapterView.getAdapter()).getItem(position).id;
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        // Nothing to be done
    }

    private void updateQuizType(int quizType) {
        _quizType = quizType;

        switch (quizType) {
            case QuizTypes.interAlphabet:
                do {
                    Map<AlphabetPair, StringPair> pairs = readInterAlphabetPossibleSourceAlphabets(
                            DbManager.getInstance().getReadableDatabase());
                    final Set<AdapterItem> set = new HashSet<>();
                    for (Map.Entry<AlphabetPair, StringPair> entry : pairs.entrySet()) {
                        set.add(new AdapterItem(entry.getKey().source, entry.getValue().source));
                    }

                    final int itemCount = set.size();
                    final AdapterItem[] items = new AdapterItem[itemCount];
                    int i = 0;
                    for (AdapterItem item : set) {
                        items[i++] = item;
                    }

                    _sourceAlphabetSpinner.setAdapter(new AlphabetAdapter(items));
                    _auxSpinner.setAdapter(new AlphabetAdapter(items));
                    _auxSpinner.setVisibility(View.VISIBLE);
                } while(false);
                break;

            case QuizTypes.translation:
                do {
                    if (_allAlphabets == null) {
                        _allAlphabets = readAllAlphabets(DbManager.getInstance().getReadableDatabase());
                    }

                    final int itemCount = _allAlphabets.size();
                    final AdapterItem[] items = new AdapterItem[itemCount];
                    for (int i = 0; i < itemCount; i++) {
                        items[i] = new AdapterItem(_allAlphabets.keyAt(i), _allAlphabets.valueAt(i));
                    }

                    _sourceAlphabetSpinner.setAdapter(new AlphabetAdapter(items));
                    _auxSpinner.setAdapter(new AlphabetAdapter(items));
                    _auxSpinner.setVisibility(View.VISIBLE);
                } while(false);
                break;

            case QuizTypes.synonym:
                do {
                    if (_allAlphabets == null) {
                        _allAlphabets = readAllAlphabets(DbManager.getInstance().getReadableDatabase());
                    }

                    final int itemCount = _allAlphabets.size();
                    final AdapterItem[] items = new AdapterItem[itemCount];
                    for (int i = 0; i < itemCount; i++) {
                        items[i] = new AdapterItem(_allAlphabets.keyAt(i), _allAlphabets.valueAt(i));
                    }

                    _sourceAlphabetSpinner.setAdapter(new AlphabetAdapter(items));
                    _auxSpinner.setVisibility(View.GONE);
                } while(false);
                break;

            case QuizTypes.appliedRule:
                do {
                    if (_allAlphabets == null) {
                        _allAlphabets = readAllAlphabets(DbManager.getInstance().getReadableDatabase());
                    }

                    if (_allRules == null) {
                        _allRules = readAllRules(DbManager.getInstance().getReadableDatabase());
                    }

                    final int alphabetCount = _allAlphabets.size();
                    final AdapterItem[] alphabets = new AdapterItem[alphabetCount];
                    for (int i = 0; i < alphabetCount; i++) {
                        alphabets[i] = new AdapterItem(_allAlphabets.keyAt(i), _allAlphabets.valueAt(i));
                    }

                    _sourceAlphabetSpinner.setAdapter(new AlphabetAdapter(alphabets));

                    final int ruleCount = _allRules.size();
                    final AdapterItem[] rules = new AdapterItem[ruleCount];
                    for (int i = 0; i < ruleCount; i++) {
                        rules[i] = new AdapterItem(_allRules.keyAt(i), _allRules.valueAt(i));
                    }

                    _auxSpinner.setAdapter(new AlphabetAdapter(rules));
                    _auxSpinner.setVisibility(View.VISIBLE);
                } while(false);
                break;

            default:
                _sourceAlphabetSpinner.setAdapter(null);
                _auxSpinner.setVisibility(View.GONE);
        }
    }

    private Integer findQuizDefinition(SQLiteDatabase db) {
        final DbManager.QuizDefinitionsTable quizDefinitions = DbManager.Tables.quizDefinitions;
        final Cursor cursor = db.rawQuery("SELECT " + idColumnName + " FROM " + quizDefinitions.getName() + " WHERE " +
                        quizDefinitions.getColumnName(quizDefinitions.getQuizTypeColumnIndex()) + "=? AND " +
                        quizDefinitions.getColumnName(quizDefinitions.getSourceBunchColumnIndex()) + "=? AND " +
                        quizDefinitions.getColumnName(quizDefinitions.getSourceAlphabetColumnIndex()) + "=? AND " +
                        quizDefinitions.getColumnName(quizDefinitions.getAuxiliarColumnIndex()) + "=?",
                new String[] {Integer.toString(_quizType), Integer.toString(_bunch),
                        Integer.toString(_sourceAlphabet), Integer.toString(_aux)});

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    if (cursor.getCount() != 1) {
                        throw new AssertionError("Duplicated quiz definition found");
                    }

                    return cursor.getInt(0);
                }
            }
            finally {
                cursor.close();
            }
        }

        return null;
    }

    private int insertQuizDefinition(SQLiteDatabase db) {
        final DbManager.QuizDefinitionsTable table = DbManager.Tables.quizDefinitions;
        ContentValues cv = new ContentValues();
        cv.put(table.getColumnName(table.getQuizTypeColumnIndex()), _quizType);
        cv.put(table.getColumnName(table.getSourceBunchColumnIndex()), _bunch);
        cv.put(table.getColumnName(table.getSourceAlphabetColumnIndex()), _sourceAlphabet);
        cv.put(table.getColumnName(table.getAuxiliarColumnIndex()), _aux);

        return (int) db.insert(table.getName(), null, cv);
    }

    private int obtainQuizDefinition() {
        final SQLiteDatabase db = DbManager.getInstance().getWritableDatabase();
        final Integer found = findQuizDefinition(db);
        return (found != null)? found : insertQuizDefinition(db);
    }

    @Override
    public void onClick(View view) {
        final int quizId = obtainQuizDefinition();
        QuestionActivity.open(this, quizId);
    }
}
