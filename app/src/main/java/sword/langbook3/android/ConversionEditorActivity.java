package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.List;
import sword.collections.Map;
import sword.database.Database;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbUpdateQuery;
import sword.database.DbValue;

import static sword.langbook3.android.EqualUtils.equal;
import static sword.langbook3.android.LangbookDatabase.obtainSymbolArray;
import static sword.langbook3.android.LangbookDatabaseUtils.convertText;
import static sword.langbook3.android.LangbookReadableDatabase.getConversion;
import static sword.langbook3.android.LangbookReadableDatabase.readConceptText;

public final class ConversionEditorActivity extends Activity implements ListView.OnItemClickListener, ListView.OnItemLongClickListener {

    private interface ArgKeys {
        String SOURCE_ALPHABET = BundleKeys.SOURCE_ALPHABET;
        String TARGET_ALPHABET = BundleKeys.TARGET_ALPHABET;
    }

    private interface SavedKeys {
        String STATE = "st";
    }

    private ImmutableSet<ImmutablePair<String, String>> _conversion;
    private ConversionEditorActivityState _state;
    private ConversionEditorAdapter _adapter;

    public static void open(Context context, int sourceAlphabet, int targetAlphabet) {
        final Intent intent = new Intent(context, ConversionEditorActivity.class);
        intent.putExtra(ArgKeys.SOURCE_ALPHABET, sourceAlphabet);
        intent.putExtra(ArgKeys.TARGET_ALPHABET, targetAlphabet);
        context.startActivity(intent);
    }

    private int getSourceAlphabet() {
        return getIntent().getIntExtra(ArgKeys.SOURCE_ALPHABET, 0);
    }

    private int getTargetAlphabet() {
        return getIntent().getIntExtra(ArgKeys.TARGET_ALPHABET, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversion_details_activity);

        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final Database db = DbManager.getInstance().getDatabase();
        final int sourceAlphabet = getSourceAlphabet();
        final int targetAlphabet = getTargetAlphabet();

        final String sourceText = readConceptText(db, sourceAlphabet, preferredAlphabet);
        final String targetText = readConceptText(db, targetAlphabet, preferredAlphabet);
        setTitle(sourceText + " -> " + targetText);

        _conversion = getConversion(db, new ImmutableIntPair(sourceAlphabet, targetAlphabet));

        if (savedInstanceState == null) {
            _state = new ConversionEditorActivityState();
        }
        else {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }

        final ListView listView = findViewById(R.id.listView);
        _adapter = new ConversionEditorAdapter(_conversion, _state.getRemoved(), _state.getAdded(), _state.getDisabled());
        listView.setAdapter(_adapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);

        if (_state.shouldDisplayModificationDialog()) {
            openModificationDialog();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conversion_editor_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemAdd:
                _state.startModification();
                openModificationDialog();
                return true;

            case R.id.menuItemSave:
                if (checkConflicts()) {
                    insertConversionInDatabase();
                    updateStringQueryTable();
                    Toast.makeText(this, "Database updated", Toast.LENGTH_SHORT).show();
                }
                return true;
        }

        return false;
    }

    private void openModificationDialog() {
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    if (_state.applyModification(_conversion)) {
                        _adapter.notifyDataSetChanged();
                    }
                    else {
                        Toast.makeText(ConversionEditorActivity.this, R.string.noChangeRequired, Toast.LENGTH_SHORT).show();
                    }
                })
                .setOnCancelListener(d -> _state.cancelModification())
                .create();

        final View view = LayoutInflater.from(dialog.getContext()).inflate(R.layout.conversion_editor_modification, null);
        final EditText source = view.findViewById(R.id.sourceEditText);
        source.setText(_state.getSourceModificationText());
        source.addTextChangedListener(new SourceTextListener());

        final EditText target = view.findViewById(R.id.targetEditText);
        target.setText(_state.getTargetModificationText());
        target.addTextChangedListener(new TargetTextListener());

        dialog.setView(view);
        dialog.show();
    }

    private final class SourceTextListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Nothing to be done
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Nothing to be done
        }

        @Override
        public void afterTextChanged(Editable s) {
            _state.updateSourceModificationText(s.toString());
        }
    }

    private final class TargetTextListener implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Nothing to be done
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // Nothing to be done
        }

        @Override
        public void afterTextChanged(Editable s) {
            _state.updateTargetModificationText(s.toString());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ConversionEditorAdapter adapter = (ConversionEditorAdapter) parent.getAdapter();
        final ConversionEditorAdapter.Entry entry = adapter.getItem(position);
        if (entry.toggleDisabledOnClick()) {
            _state.toggleEnabled(entry.getSource());
        }
        else {
            final int convPos = entry.getConversionPosition();
            if (convPos < 0) {
                throw new AssertionError();
            }

            _state.toggleRemoved(convPos);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        final ConversionEditorAdapter adapter = (ConversionEditorAdapter) parent.getAdapter();
        final ConversionEditorAdapter.Entry entry = adapter.getItem(position);
        final Map<String, String> added = _state.getAdded();
        final String key = entry.getSource();
        if (added.containsKey(key)) {
            _state.startModification();
            _state.updateSourceModificationText(key);
            _state.updateTargetModificationText(added.get(key));
            openModificationDialog();
        }
        else {
            final int convPos = entry.getConversionPosition();
            if (convPos >= 0) {
                final ImmutablePair<String, String> pair = _conversion.valueAt(position);
                _state.startModification();
                _state.updateSourceModificationText(pair.left);
                _state.updateTargetModificationText(pair.right);
                openModificationDialog();
            }
        }

        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(SavedKeys.STATE, _state);
    }

    private boolean checkConflicts() {
        final ImmutableSet<ImmutablePair<String, String>> newConversion = _state.getResultingConversion(_conversion);
        final Database db = DbManager.getInstance().getDatabase();
        final int sourceAlphabet = getSourceAlphabet();

        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStringAlphabetColumnIndex(), sourceAlphabet)
                .select(table.getStringColumnIndex());

        final String failingWord = db.select(query)
                .map(row -> row.get(0).toText())
                .findFirst(str -> convertText(newConversion, str) == null, null);

        if (failingWord == null) {
            Toast.makeText(this, "All text can be converted", Toast.LENGTH_SHORT).show();
            return true;
        }
        else {
            Toast.makeText(this, "Failing to convert word " + failingWord, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void insertConversionInDatabase() {
        final Database db = DbManager.getInstance().getDatabase();
        final ImmutableIntPair alphabetPair = new ImmutableIntPair(getSourceAlphabet(), getTargetAlphabet());
        final ImmutableSet<ImmutablePair<String, String>> oldConversion = LangbookReadableDatabase.getConversion(db, alphabetPair);
        final ImmutableSet<ImmutablePair<String, String>> newConversion = _state.getResultingConversion(_conversion);

        final ImmutableSet<ImmutablePair<String, String>> pairsToRemove = oldConversion.filterNot(newConversion::contains);
        final ImmutableSet<ImmutablePair<String, String>> pairsToInclude = newConversion.filterNot(oldConversion::contains);

        for (ImmutablePair<String, String> pair : pairsToRemove) {
            // TODO: SymbolArrays should also be removed if not used by any other, just to avoid dirty databases
            final int sourceId = LangbookReadableDatabase.findSymbolArray(db, pair.left);
            final int targetId = LangbookReadableDatabase.findSymbolArray(db, pair.right);
            if (!LangbookDeleter.deleteConversionRegister(db, alphabetPair, sourceId, targetId)) {
                throw new AssertionError();
            }
        }

        for (ImmutablePair<String, String> pair : pairsToInclude) {
            final int sourceId = obtainSymbolArray(db, pair.left);
            final int targetId = obtainSymbolArray(db, pair.right);
            LangbookDbInserter.insertConversion(db, alphabetPair.left, alphabetPair.right, sourceId, targetId);
        }
    }

    private void updateStringQueryTable() {
        final ImmutableSet<ImmutablePair<String, String>> newConversion = _state.getResultingConversion(_conversion);

        final Database db = DbManager.getInstance().getDatabase();
        final LangbookDbSchema.StringQueriesTable table = LangbookDbSchema.Tables.stringQueries;

        final int offset = table.columns().size();
        final DbQuery query = new DbQuery.Builder(table)
                .join(table, table.getDynamicAcceptationColumnIndex(), table.getDynamicAcceptationColumnIndex())
                .where(table.getStringAlphabetColumnIndex(), getTargetAlphabet())
                .where(offset + table.getStringAlphabetColumnIndex(), getSourceAlphabet())
                .select(table.getDynamicAcceptationColumnIndex(), table.getStringColumnIndex(), offset + table.getStringColumnIndex());

        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        final DbResult dbResult = db.select(query);
        while (dbResult.hasNext()) {
            final List<DbValue> row = dbResult.next();
            final int dynAcc = row.get(0).toInt();
            final String source = row.get(2).toText();
            final String currentTarget = row.get(1).toText();
            final String newTarget = convertText(newConversion, source);
            if (!equal(currentTarget, newTarget)) {
                builder.put(dynAcc, newTarget);
            }
        }

        final ImmutableIntKeyMap<String> updateMap = builder.build();
        final int updateCount = updateMap.size();
        for (int i = 0; i < updateCount; i++) {
            final DbUpdateQuery upQuery = new DbUpdateQuery.Builder(table)
                    .where(table.getStringAlphabetColumnIndex(), getTargetAlphabet())
                    .where(table.getDynamicAcceptationColumnIndex(), updateMap.keyAt(i))
                    .put(table.getStringColumnIndex(), updateMap.valueAt(i))
                    .build();

            if (!db.update(upQuery)) {
                throw new AssertionError();
            }
        }
    }
}
