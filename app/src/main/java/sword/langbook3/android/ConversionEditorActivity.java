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

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.Map;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdBundler;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.ParcelableConversion;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.ConversionProposal;

public final class ConversionEditorActivity extends Activity implements ListView.OnItemClickListener, ListView.OnItemLongClickListener {

    private interface ArgKeys {
        String SOURCE_ALPHABET = BundleKeys.SOURCE_ALPHABET;
        String TARGET_ALPHABET = BundleKeys.TARGET_ALPHABET;
    }

    private interface SavedKeys {
        String STATE = "st";
    }

    interface ResultKeys {
        String CONVERSION = "c";
    }

    private Conversion<AlphabetId> _conversion;
    private ConversionEditorActivityState _state;
    private ConversionEditorAdapter _adapter;

    /**
     * Allow editing an existing conversion or include a new one.
     *
     * The conversion will be inserted into the database before finishing this activity,
     * except if the user cancel the process by clicking back, or the target alphabet
     * provided is not defined in the database as alphabet.
     *
     * In the latter case, the result will be sent back through the {@link android.os.Bundle} using a
     * {@link ParcelableConversion} with key {@link ResultKeys#CONVERSION}.
     *
     * @param activity Activity used to open this new activity, and where it will return when finished.
     * @param requestCode Identifier for this call.
     * @param sourceAlphabet Source alphabet for this conversion. This alphabet must be registered as alphabet in the database.
     * @param targetAlphabet Target alphabet for this conversion. This alphabet may or not be registered as alphabet.
     *                       If so, the conversion will be stored into the database before finishing the activity.
     *                       If not, the conversion will be send back through the bundle.
     */
    public static void open(Activity activity, int requestCode, AlphabetId sourceAlphabet, AlphabetId targetAlphabet) {
        final Intent intent = new Intent(activity, ConversionEditorActivity.class);
        AlphabetIdBundler.writeAsIntentExtra(intent, ArgKeys.SOURCE_ALPHABET, sourceAlphabet);
        AlphabetIdBundler.writeAsIntentExtra(intent, ArgKeys.TARGET_ALPHABET, targetAlphabet);
        activity.startActivityForResult(intent, requestCode);
    }

    private AlphabetId getSourceAlphabet() {
        return AlphabetIdBundler.readAsIntentExtra(getIntent(), ArgKeys.SOURCE_ALPHABET);
    }

    private AlphabetId getTargetAlphabet() {
        return AlphabetIdBundler.readAsIntentExtra(getIntent(), ArgKeys.TARGET_ALPHABET);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversion_details_activity);

        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final AlphabetId sourceAlphabet = getSourceAlphabet();
        final AlphabetId targetAlphabet = getTargetAlphabet();

        final String sourceText = checker.readConceptText(sourceAlphabet.getConceptId(), preferredAlphabet);
        final String targetText = (targetAlphabet != null)? checker.readConceptText(targetAlphabet.getConceptId(), preferredAlphabet) : "?";
        setTitle(sourceText + " -> " + targetText);

        _conversion = (targetAlphabet != null)? checker.getConversion(new ImmutablePair<>(sourceAlphabet, targetAlphabet)) :
                new Conversion<>(sourceAlphabet, null, ImmutableHashMap.empty());

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
                final Conversion<AlphabetId> newConversion = _state.getResultingConversion(_conversion);
                final LangbookDbManager manager = DbManager.getInstance().getManager();
                if (checkConflicts(manager, newConversion)) {
                    if (manager.isAlphabetPresent(getTargetAlphabet())) {
                        if (manager.replaceConversion(newConversion)) {
                            Toast.makeText(this, R.string.updateConversionFeedback, Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                        }
                    }
                    else {
                        final Intent intent = new Intent();
                        intent.putExtra(ResultKeys.CONVERSION, new ParcelableConversion(newConversion));
                        setResult(RESULT_OK, intent);
                    }

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

    private boolean checkConflicts(LangbookDbChecker checker, ConversionProposal<AlphabetId> newConversion) {
        final ImmutableSet<String> wordsInConflict = checker.findConversionConflictWords(newConversion);

        if (wordsInConflict.isEmpty()) {
            return true;
        }
        else {
            final String firstWord = wordsInConflict.valueAt(0);
            final String text = (wordsInConflict.size() == 1)? "Failing to convert word " + firstWord :
                    (wordsInConflict.size() == 2)? "Failing to convert words " + firstWord + " and " + wordsInConflict.valueAt(1) :
                            "Failing to convert word " + firstWord + " and other " + (wordsInConflict.size() - 1) + " words";
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
