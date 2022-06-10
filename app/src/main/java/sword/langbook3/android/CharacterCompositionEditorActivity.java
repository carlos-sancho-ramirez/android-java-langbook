package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import sword.collections.ImmutableList;
import sword.langbook3.android.controllers.AcceptationPickerController;
import sword.langbook3.android.controllers.CharacterCompositionDefinitionEditorController;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CharacterCompositionTypeId;
import sword.langbook3.android.db.CharacterCompositionTypeIdBundler;
import sword.langbook3.android.db.CharacterCompositionTypeIdManager;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CharacterIdBundler;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.models.CharacterCompositionEditorModel;
import sword.langbook3.android.models.CharacterCompositionPart;
import sword.langbook3.android.models.CharacterCompositionRepresentation;
import sword.langbook3.android.models.IdentifiableCharacterCompositionResult;

import static sword.langbook3.android.models.CharacterCompositionRepresentation.INVALID_CHARACTER;

public final class CharacterCompositionEditorActivity extends Activity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private static final int REQUEST_CODE_ADD_COMPOSITION_TYPE = 1;
    private static final int REQUEST_CODE_EDIT_COMPOSITION_TYPE = 2;

    static final char TOKEN_START_CHARACTER = '{';
    static final char TOKEN_END_CHARACTER = '}';
    static final String TOKEN_START_STRING = "" + TOKEN_START_CHARACTER;
    private static final String TOKEN_END_STRING = "" + TOKEN_END_CHARACTER;

    private interface ArgKeys {
        String CHARACTER = BundleKeys.CHARACTER;
    }

    private interface SavedKeys {
        String SELECTED_TYPE_ID = "sti";
    }

    public static void open(Activity activity, int requestCode, CharacterId characterId) {
        final Intent intent = new Intent(activity, CharacterCompositionEditorActivity.class);
        CharacterIdBundler.writeAsIntentExtra(intent, ArgKeys.CHARACTER, characterId);
        activity.startActivityForResult(intent, requestCode);
    }

    private CharacterId _characterId;
    private CharacterCompositionTypeId _selectedTypeId;

    private AlphabetId _preferredAlphabet;
    private AutoCompleteTextView _firstField;
    private AutoCompleteTextView _secondField;
    private Spinner _compositionTypeSpinner;

    private static void setPartText(CharacterCompositionPart<CharacterId> part, TextView textView) {
        final String text = (part == null)? null :
                (part.representation.character != INVALID_CHARACTER)? "" + part.representation.character :
                (part.representation.token != null)? TOKEN_START_STRING + part.representation.token + TOKEN_END_CHARACTER : null;
        textView.setText(text);
    }

    private void updateSpinner(@NonNull ImmutableList<IdentifiableCharacterCompositionResult<CharacterCompositionTypeId>> compositionTypes) {
        _compositionTypeSpinner.setAdapter(new CompositionTypesAdapter(compositionTypes));
        final int index = compositionTypes.indexWhere(result -> result.id.equals(_selectedTypeId));

        if (index >= 0) {
            _compositionTypeSpinner.setSelection(index);
        }
        else {
            _selectedTypeId = compositionTypes.isEmpty()? null : compositionTypes.valueAt(0).id;
        }
    }

    private void updateSpinner() {
        updateSpinner(DbManager.getInstance().getManager().getCharacterCompositionTypes(_preferredAlphabet));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        final CompositionTypesAdapter adapter = (CompositionTypesAdapter) parent.getAdapter();
        _selectedTypeId = adapter.getItem(position).id;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Nothing to be done
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.character_composition_editor_activity);

        _characterId = CharacterIdBundler.readAsIntentExtra(getIntent(), ArgKeys.CHARACTER);
        _preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final CharacterCompositionEditorModel<CharacterId, CharacterCompositionTypeId> model = DbManager.getInstance().getManager().getCharacterCompositionDetails(_characterId);

        if (model == null) {
            finish();
        }
        else {
            final String representation = CharacterDetailsAdapter.representChar(model.representation);
            setTitle(getString(R.string.characterCompositionEditorActivityTitle, representation));

            final TokenSuggestionsAdapter firstAdapter = new TokenSuggestionsAdapter();
            _firstField = findViewById(R.id.firstField);
            _firstField.setAdapter(firstAdapter);
            _firstField.setOnItemClickListener((parent, view, position, id) -> {
                final String text = firstAdapter.getItem(position);
                _firstField.setText(text);
                _firstField.setSelection(text.length());
            });

            final TokenSuggestionsAdapter secondAdapter = new TokenSuggestionsAdapter();
            _secondField = findViewById(R.id.secondField);
            _secondField.setAdapter(secondAdapter);
            _secondField.setOnItemClickListener((parent, view, position, id) -> {
                final String text = secondAdapter.getItem(position);
                _secondField.setText(text);
                _secondField.setSelection(text.length());
            });

            _compositionTypeSpinner = findViewById(R.id.compositionTypeSpinner);
            _compositionTypeSpinner.setOnItemSelectedListener(this);

            final ImmutableList<IdentifiableCharacterCompositionResult<CharacterCompositionTypeId>> compositionTypes = DbManager.getInstance().getManager().getCharacterCompositionTypes(_preferredAlphabet);

            if (model.compositionType != null) {
                _selectedTypeId = model.compositionType;
            }

            if (savedInstanceState != null) {
                final CharacterCompositionTypeId savedTypeId = CharacterCompositionTypeIdBundler.read(savedInstanceState, SavedKeys.SELECTED_TYPE_ID);
                if (compositionTypes.anyMatch(result -> result.id.equals(savedTypeId))) {
                    _selectedTypeId = savedTypeId;
                }
            }
            updateSpinner(compositionTypes);

            findViewById(R.id.addCompositionTypeButton).setOnClickListener(this);
            findViewById(R.id.saveButton).setOnClickListener(this);

            setPartText(model.first, _firstField);
            setPartText(model.second, _secondField);
        }
    }

    static boolean isInvalidRepresentation(String text) {
        final int textLength = text.length();
        return textLength > 1 && !(
                text.startsWith(TOKEN_START_STRING) &&
                text.endsWith(TOKEN_END_STRING) &&
                LangbookDbSchema.CharacterTokensTable.isValidToken(text.substring(1, textLength - 1)));
    }

    private void addCompositionType() {
        AcceptationPickerActivity.open(this, REQUEST_CODE_ADD_COMPOSITION_TYPE, new AcceptationPickerController(null));
    }

    @Override
    public void onClick(View v) {
        final int viewId = v.getId();
        if (viewId == R.id.addCompositionTypeButton) {
            addCompositionType();
        }
        else {
            save();
        }
    }

    private void save() {
        final String firstText = _firstField.getText().toString();
        final String secondText = _secondField.getText().toString();

        String errorMessage = null;
        if (firstText.isEmpty()) {
            errorMessage = getString(R.string.characterCompositionEditorFirstEmptyError);
        }
        else if (secondText.isEmpty()) {
            errorMessage = getString(R.string.characterCompositionEditorSecondEmptyError);
        }
        else if (isInvalidRepresentation(firstText)) {
            errorMessage = getString(R.string.characterCompositionEditorFirstHasBraceError);
        }
        else if (isInvalidRepresentation(secondText)) {
            errorMessage = getString(R.string.characterCompositionEditorSecondHasBraceError);
        }
        else if (_selectedTypeId == null) {
            errorMessage = getString(R.string.characterCompositionEditorNoTypeSelectedError);
        }

        if (errorMessage != null) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
        else {
            final CharacterCompositionRepresentation firstRepresentation = (firstText.length() > 1)?
                    new CharacterCompositionRepresentation(INVALID_CHARACTER, firstText.substring(1, firstText.length() - 1)) :
                    new CharacterCompositionRepresentation(firstText.charAt(0), null);

            final CharacterCompositionRepresentation secondRepresentation = (secondText.length() > 1)?
                    new CharacterCompositionRepresentation(INVALID_CHARACTER, secondText.substring(1, secondText.length() - 1)) :
                    new CharacterCompositionRepresentation(secondText.charAt(0), null);

            final LangbookDbManager manager = DbManager.getInstance().getManager();
            manager.updateCharacterComposition(_characterId, firstRepresentation, secondRepresentation, _selectedTypeId);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_COMPOSITION_TYPE && data != null) {
            final AcceptationId acceptation = AcceptationIdBundler.readAsIntentExtra(data, AcceptationPickerActivity.ResultKeys.STATIC_ACCEPTATION);
            if (acceptation != null) {
                final LangbookDbManager manager = DbManager.getInstance().getManager();
                final ConceptId concept = manager.conceptFromAcceptation(acceptation);
                _selectedTypeId = CharacterCompositionTypeIdManager.conceptAsCharacterCompositionTypeId(concept);
                CharacterCompositionDefinitionEditorActivity.open(this, REQUEST_CODE_EDIT_COMPOSITION_TYPE, new CharacterCompositionDefinitionEditorController(_selectedTypeId));
            }
        }
        else if (requestCode == REQUEST_CODE_EDIT_COMPOSITION_TYPE) {
            updateSpinner();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        CharacterCompositionTypeIdBundler.write(outState, SavedKeys.SELECTED_TYPE_ID, _selectedTypeId);
    }

    private final class TokenSuggestionsAdapter extends BaseAdapter implements Filterable {

        private ImmutableList<String> _entries = ImmutableList.empty();
        private LayoutInflater _inflater;
        private Filter _filter;

        @Override
        public int getCount() {
            return _entries.size();
        }

        @Override
        public String getItem(int position) {
            return _entries.valueAt(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                if (_inflater == null) {
                    _inflater = LayoutInflater.from(parent.getContext());
                }

                view = _inflater.inflate(R.layout.character_composition_token_suggestion_entry, parent, false);
            }

            final TextView tokenTextView = view.findViewById(R.id.tokenText);
            tokenTextView.setText(_entries.valueAt(position));
            return view;
        }

        @Override
        public Filter getFilter() {
            if (_filter == null) {
                _filter = new TokenFilter(this);
            }

            return _filter;
        }

        void setEntries(ImmutableList<String> entries) {
            _entries = (entries == null)? ImmutableList.empty() : entries;
            notifyDataSetChanged();
        }
    }

    private final class TokenFilter extends Filter {

        private final TokenSuggestionsAdapter _adapter;

        TokenFilter(TokenSuggestionsAdapter adapter) {
            _adapter = adapter;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            final FilterResults result = new FilterResults();
            if (constraint.length() >= 1 && constraint.charAt(0) == TOKEN_START_CHARACTER) {
                final ImmutableList<String> suggestions = DbManager.getInstance().getManager().suggestCharacterTokens(constraint.toString().substring(1));
                result.values = suggestions.map(str -> TOKEN_START_STRING + str + TOKEN_END_CHARACTER);
                result.count = suggestions.size();
            }

            return result;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            _adapter.setEntries((ImmutableList<String>) results.values);
        }
    }

    public static void bindCompositionType(@NonNull View view, @NonNull IdentifiableCharacterCompositionResult<?> entry) {
        view.<TextView>findViewById(R.id.text).setText(entry.text);
        view.findViewById(R.id.drawableHolder).setBackground(
                new CharacterCompositionDefinitionDrawable(entry.register));
    }

    private static final class CompositionTypesAdapter extends BaseAdapter {

        private final ImmutableList<IdentifiableCharacterCompositionResult<CharacterCompositionTypeId>> _entries;
        private LayoutInflater _inflater;

        CompositionTypesAdapter(ImmutableList<IdentifiableCharacterCompositionResult<CharacterCompositionTypeId>> entries) {
            _entries = entries;
        }

        @Override
        public int getCount() {
            return _entries.size();
        }

        @Override
        public IdentifiableCharacterCompositionResult<CharacterCompositionTypeId> getItem(int position) {
            return _entries.valueAt(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null) {
                if (_inflater == null) {
                    _inflater = LayoutInflater.from(parent.getContext());
                }

                view = _inflater.inflate(R.layout.character_composition_definition_entry, parent, false);
            }

            bindCompositionType(view, _entries.valueAt(position));
            return view;
        }
    }
}
