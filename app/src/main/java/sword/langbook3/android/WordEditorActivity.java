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
import sword.collections.IntKeyMap;
import sword.collections.MapGetter;
import sword.langbook3.android.collections.SyncCacheMap;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.presenters.Presenter;
import sword.langbook3.android.presenters.DefaultPresenter;

import static sword.collections.SortUtils.equal;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class WordEditorActivity extends Activity implements View.OnClickListener {

    public static final int REQUEST_CODE_CORRELATION_PICKER = 1;
    private static final int REQUEST_CODE_CHECK_CONVERSION = 2;

    public interface ArgKeys {
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
    private final SyncCacheMap<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> _conversions =
            new SyncCacheMap<>(DbManager.getInstance().getManager()::getConversion);

    public static void open(@NonNull Activity activity, int requestCode, @NonNull Controller controller) {
        ensureNonNull(controller);
        final Intent intent = new Intent(activity, WordEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void open(@NonNull Context context, Controller controller) {
        final Intent intent = new Intent(context, WordEditorActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.word_editor_activity);

        _controller = getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _controller.setTitle(this);

        _formPanel = findViewById(R.id.formPanel);
        findViewById(R.id.nextButton).setOnClickListener(this);

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
            _controller.updateConvertedTexts(_texts, _conversions);
            updateFields();
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

            _controller.complete(new DefaultPresenter(this), builder.build());
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
        void setTitle(@NonNull Activity activity);
        void updateConvertedTexts(@NonNull String[] texts, @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions);
        @NonNull
        UpdateFieldsResult updateFields(@NonNull Activity activity, @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions, ImmutableList<String> texts);
        void complete(@NonNull Presenter presenter, @NonNull ImmutableCorrelation<AlphabetId> texts);
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
