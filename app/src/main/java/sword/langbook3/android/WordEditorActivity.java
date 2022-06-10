package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.MapGetter;
import sword.langbook3.android.collections.SyncCacheMap;
import sword.langbook3.android.controllers.WordEditorController;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.Correlation;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdBundler;
import sword.langbook3.android.models.Conversion;

import static sword.collections.SortUtils.equal;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class WordEditorActivity extends Activity implements View.OnClickListener {

    public static final int REQUEST_CODE_CORRELATION_PICKER = 1;
    private static final int REQUEST_CODE_CHECK_CONVERSION = 2;

    public interface ArgKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
        String LANGUAGE = BundleKeys.LANGUAGE;
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    private interface SavedKeys {
        String TEXTS = "texts";
    }

    private Controller _controller;
    private LinearLayout _formPanel;
    private ImmutableIntKeyMap<Controller.FieldConversion> _fieldConversions;
    private String[] _texts;
    private ImmutableIntKeyMap<AlphabetId> _fieldIndexAlphabetRelationMap;
    private AcceptationId _existingAcceptation;
    private final SyncCacheMap<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> _conversions =
            new SyncCacheMap<>(DbManager.getInstance().getManager()::getConversion);

    /**
     * Open a wizard to fulfill the plain texts for an specific language, and select an specific correlation array.
     * Using this method will not take into account the conversions. This method is intended to define agent adders.
     */
    public static void open(Activity activity, int requestCode, LanguageId language) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        LanguageIdBundler.writeAsIntentExtra(intent, ArgKeys.LANGUAGE, language);
        intent.putExtra(ArgKeys.CONTROLLER, new WordEditorController(null, null, null, null, language, null, false));
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Activity activity, int requestCode, LanguageId language, String searchQuery, ConceptId concept) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        LanguageIdBundler.writeAsIntentExtra(intent, ArgKeys.LANGUAGE, language);
        intent.putExtra(ArgKeys.CONTROLLER, new WordEditorController(null, concept, null, null, null, searchQuery, true));
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Activity activity, int requestCode, LanguageId language, ConceptId concept) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        LanguageIdBundler.writeAsIntentExtra(intent, ArgKeys.LANGUAGE, language);
        intent.putExtra(ArgKeys.CONTROLLER, new WordEditorController(null, concept, null, null, language, null, true));activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Activity activity, int requestCode, String title, Correlation<AlphabetId> correlation, ConceptId concept) {
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, new WordEditorController(title, concept, null, correlation.toImmutable(), null, null, true));
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(Context context, AcceptationId acceptation) {
        final Intent intent = new Intent(context, WordEditorActivity.class);
        AcceptationIdBundler.writeAsIntentExtra(intent, ArgKeys.ACCEPTATION, acceptation);
        intent.putExtra(ArgKeys.CONTROLLER, new WordEditorController(null, null, acceptation, null, null, null, true));
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_editor_activity);

        _controller = getIntent().getParcelableExtra(MatchingBunchesPickerActivity.ArgKeys.CONTROLLER);
        final String givenTitle = _controller.getTitle();
        if (givenTitle != null) {
            setTitle(givenTitle);
        }

        _formPanel = findViewById(R.id.formPanel);
        findViewById(R.id.nextButton).setOnClickListener(this);

        _existingAcceptation = AcceptationIdBundler.readAsIntentExtra(getIntent(), ArgKeys.ACCEPTATION);

        if (savedInstanceState != null) {
            _texts = savedInstanceState.getStringArray(SavedKeys.TEXTS);
        }

        updateFields();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(this, requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CHECK_CONVERSION) {
            _conversions.clear();
            updateConvertedTexts();
            updateFields();
        }
    }

    private LanguageId getLanguage(LangbookDbChecker checker) {
        if (_existingAcceptation != null) {
            final ImmutablePair<ImmutableCorrelation<AlphabetId>, LanguageId> result = checker.readAcceptationTextsAndLanguage(_existingAcceptation);
            return result.right;
        }
        else {
            return LanguageIdBundler.readAsIntentExtra(getIntent(), ArgKeys.LANGUAGE);
        }
    }

    private void updateConvertedTexts() {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final LanguageId language = getLanguage(manager);
        final ImmutableSet<AlphabetId> alphabets = manager.findAlphabetsByLanguage(language);
        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = manager.findConversions(alphabets);

        final int alphabetCount = alphabets.size();
        for (int targetFieldIndex = 0; targetFieldIndex < alphabetCount; targetFieldIndex++) {
            final AlphabetId targetAlphabet = alphabets.valueAt(targetFieldIndex);
            final AlphabetId sourceAlphabet = conversionMap.get(targetAlphabet, null);
            final ImmutablePair<AlphabetId, AlphabetId> alphabetPair = new ImmutablePair<>(sourceAlphabet, targetAlphabet);
            final int sourceFieldIndex = (sourceAlphabet != null)? alphabets.indexOf(sourceAlphabet) : -1;
            if (sourceFieldIndex >= 0) {
                final String sourceText = _texts[sourceFieldIndex];
                _texts[targetFieldIndex] = (sourceText != null)? _conversions.get(alphabetPair).convert(sourceText) : null;
            }
        }
    }

    private static void setConversionText(EditText editText, String text) {
        if (text == null) {
            editText.setText(R.string.wordEditorWrongConversionFieldText);
        }
        else {
            editText.setText(text);
        }
    }

    private void updateFields() {
        _formPanel.removeAllViews();
        final ImmutableList<String> givenTexts = (_texts != null)? ImmutableList.from(_texts) : null;
        final Controller.UpdateFieldsResult result = _controller.updateFields(this, _conversions, givenTexts);
        if (result.texts != givenTexts) {
            final int count = result.texts.size();
            _texts = new String[count];
            for (int i = 0; i < count; i++) {
                _texts[i] = result.texts.valueAt(i);
            }
        }

        _fieldConversions = result.fieldConversions;
        _fieldIndexAlphabetRelationMap = result.fieldIndexAlphabetRelationMap;

        final LayoutInflater inflater = getLayoutInflater();
        final ImmutableIntSet editableFields = _fieldIndexAlphabetRelationMap.keySet();
        final int fieldCount = result.fieldNames.size();
        for (int fieldIndex = 0; fieldIndex < fieldCount; fieldIndex++) {
            final boolean isEditable = editableFields.contains(fieldIndex);
            final int layoutId = isEditable? R.layout.word_editor_field_entry : R.layout.word_editor_converted_entry;
            inflater.inflate(layoutId, _formPanel, true);
            View fieldEntry = _formPanel.getChildAt(fieldIndex);

            final TextView textView = fieldEntry.findViewById(R.id.fieldName);
            textView.setText(result.fieldNames.valueAt(fieldIndex));

            final EditText editText = fieldEntry.findViewById(R.id.fieldValue);
            final String text = _texts[fieldIndex];
            editText.setText(text);

            if (isEditable) {
                if (result.autoSelectText && text != null) {
                    editText.setSelection(0, text.length());
                }

                editText.addTextChangedListener(new FieldTextWatcher(fieldIndex));
            }
            else {
                final AlphabetId target = result.fieldNames.keyAt(fieldIndex);
                final AlphabetId source = result.fieldConversionsMap.get(target);
                fieldEntry.findViewById(R.id.checkConversionButton).setOnClickListener(view ->
                        ConversionDetailsActivity.open(this, REQUEST_CODE_CHECK_CONVERSION, source, target));
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putStringArray(SavedKeys.TEXTS, _texts);
    }

    @Override
    public void onClick(View v) {
        boolean allValid = true;
        for (String text : _texts) {
            if (text == null || text.length() == 0) {
                allValid = false;
                break;
            }
        }

        if (allValid) {
            final ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
            for (IntKeyMap.Entry<AlphabetId> entry : _fieldIndexAlphabetRelationMap.entries()) {
                builder.put(entry.value(), _texts[entry.key()]);
            }

            _controller.complete(this, builder.build());
        }
        else {
            Toast.makeText(this, R.string.wordEditorWrongTextError, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateText(int fieldIndex, String newText) {
        String oldText = _texts[fieldIndex];
        _texts[fieldIndex] = newText;

        for (IntKeyMap.Entry<Controller.FieldConversion> entry : _fieldConversions.entries()) {
            if (entry.value().sourceField == fieldIndex && !equal(oldText, newText)) {
                String convertedText = entry.value().conversion.convert(newText);
                _texts[entry.key()] = convertedText;

                final EditText editText = _formPanel.getChildAt(entry.key()).findViewById(R.id.fieldValue);
                setConversionText(editText, convertedText);
            }
        }
    }

    private final class FieldTextWatcher implements TextWatcher {

        final int fieldIndex;

        FieldTextWatcher(int fieldIndex) {
            this.fieldIndex = fieldIndex;
        }

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
            updateText(fieldIndex, s.toString());
        }
    }

    public interface Controller extends Parcelable {
        String getTitle();
        @NonNull
        UpdateFieldsResult updateFields(@NonNull Activity activity, @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions, ImmutableList<String> texts);
        void complete(@NonNull Activity activity, @NonNull ImmutableCorrelation<AlphabetId> texts);
        void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data);

        final class FieldConversion {
            final int sourceField;
            final Conversion<AlphabetId> conversion;

            public FieldConversion(int sourceField, Conversion<AlphabetId> conversion) {
                this.sourceField = sourceField;
                this.conversion = conversion;
            }
        }

        final class UpdateFieldsResult {
            @NonNull
            public final ImmutableMap<AlphabetId, String> fieldNames;

            @NonNull
            public final ImmutableList<String> texts;

            @NonNull
            public final ImmutableIntKeyMap<FieldConversion> fieldConversions;

            @NonNull
            public final ImmutableMap<AlphabetId, AlphabetId> fieldConversionsMap;

            @NonNull
            public final ImmutableIntKeyMap<AlphabetId> fieldIndexAlphabetRelationMap;
            public final boolean autoSelectText;

            public UpdateFieldsResult(
                    @NonNull ImmutableMap<AlphabetId, String> fieldNames,
                    @NonNull ImmutableList<String> texts,
                    @NonNull ImmutableIntKeyMap<FieldConversion> fieldConversions,
                    @NonNull ImmutableMap<AlphabetId, AlphabetId> fieldConversionsMap,
                    @NonNull ImmutableIntKeyMap<AlphabetId> fieldIndexAlphabetRelationMap,
                    boolean autoSelectText) {
                ensureNonNull(fieldNames, texts, fieldConversions, fieldConversionsMap, fieldIndexAlphabetRelationMap);
                this.fieldNames = fieldNames;
                this.texts = texts;
                this.fieldConversions = fieldConversions;
                this.fieldConversionsMap = fieldConversionsMap;
                this.fieldIndexAlphabetRelationMap = fieldIndexAlphabetRelationMap;
                this.autoSelectText = autoSelectText;
            }
        }
    }
}
