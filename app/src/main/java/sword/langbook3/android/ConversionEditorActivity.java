package sword.langbook3.android;

import android.app.Activity;
import android.app.AlertDialog;
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

import sword.collections.ImmutableSet;
import sword.collections.Map;
import sword.database.Database;
import sword.database.DbExporter;
import sword.langbook3.android.LangbookReadableDatabase.Conversion;

import static sword.langbook3.android.LangbookReadableDatabase.findConversionConflictWords;
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

    private Conversion _conversion;
    private ConversionEditorActivityState _state;
    private ConversionEditorAdapter _adapter;

    public static void open(Activity activity, int requestCode, int sourceAlphabet, int targetAlphabet) {
        final Intent intent = new Intent(activity, ConversionEditorActivity.class);
        intent.putExtra(ArgKeys.SOURCE_ALPHABET, sourceAlphabet);
        intent.putExtra(ArgKeys.TARGET_ALPHABET, targetAlphabet);
        activity.startActivityForResult(intent, requestCode);
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
                final Conversion newConversion = _state.getResultingConversion(_conversion);
                final Database db = DbManager.getInstance().getDatabase();
                if (checkConflicts(db, newConversion)) {
                    if (!LangbookDatabase.replaceConversion(db, newConversion)) {
                        throw new AssertionError();
                    }

                    Toast.makeText(this, R.string.updateConversionFeedback, Toast.LENGTH_SHORT).show();

                    setResult(RESULT_OK);
                    finish();
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
                final Map<String, String> map = _conversion.getMap();
                _state.startModification();
                _state.updateSourceModificationText(map.keyAt(position));
                _state.updateTargetModificationText(map.valueAt(position));
                openModificationDialog();
            }
        }

        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(SavedKeys.STATE, _state);
    }

    private boolean checkConflicts(DbExporter.Database db, Conversion newConversion) {
        final ImmutableSet<String> wordsInConflict = findConversionConflictWords(db, newConversion);

        if (wordsInConflict.isEmpty()) {
            return true;
        }
        else {
            final String firstWord = wordsInConflict.valueAt(0);
            final String text = (wordsInConflict.size() == 1)? "Failing to convert word " + firstWord :
                    (wordsInConflict.size() == 2)? "Failing to convert words " + firstWord + wordsInConflict.valueAt(1) :
                            "Failing to convert word " + firstWord + " and other " + (wordsInConflict.size() - 1) + " words";
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
