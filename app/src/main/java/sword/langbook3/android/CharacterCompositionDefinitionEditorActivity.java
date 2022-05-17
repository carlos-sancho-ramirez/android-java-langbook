package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import sword.collections.Procedure;
import sword.langbook3.android.collections.Supplier;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CharacterCompositionTypeId;
import sword.langbook3.android.db.CharacterCompositionTypeIdBundler;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.models.CharacterCompositionDefinitionArea;
import sword.langbook3.android.models.CharacterCompositionDefinitionAreaInterface;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;

import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class CharacterCompositionDefinitionEditorActivity extends Activity {

    private interface ArgKeys {
        String ID = "id";
    }

    public static void open(@NonNull Context context, @NonNull CharacterCompositionTypeId id) {
        ensureNonNull(context, id);
        final Intent intent = new Intent(context, CharacterCompositionDefinitionEditorActivity.class);
        CharacterCompositionTypeIdBundler.writeAsIntentExtra(intent, ArgKeys.ID, id);
        context.startActivity(intent);
    }

    public static void open(@NonNull Activity activity, int requestCode, @NonNull CharacterCompositionTypeId id) {
        ensureNonNull(activity, id);
        final Intent intent = new Intent(activity, CharacterCompositionDefinitionEditorActivity.class);
        CharacterCompositionTypeIdBundler.writeAsIntentExtra(intent, ArgKeys.ID, id);
        activity.startActivityForResult(intent, requestCode);
    }

    private CharacterCompositionDefinitionEditorView _editorView;

    private CharacterCompositionTypeId getCharacterCompositionTypeId() {
        return CharacterCompositionTypeIdBundler.readAsIntentExtra(getIntent(), ArgKeys.ID);
    }

    private static void updateAreaDetailsEntry(@NonNull ViewGroup areaEntry, int value) {
        areaEntry.<TextView>findViewById(R.id.entryValue).setText(Integer.toString(value));
    }

    private static void setAreaDetailsEntry(@NonNull ViewGroup areaEntry, @StringRes int name, int value, View.OnClickListener minusListener, View.OnClickListener plusListener) {
        areaEntry.<TextView>findViewById(R.id.entryName).setText(name);
        areaEntry.findViewById(R.id.entryMinusButton).setOnClickListener(minusListener);
        areaEntry.findViewById(R.id.entryPlusButton).setOnClickListener(plusListener);
        updateAreaDetailsEntry(areaEntry, value);
    }

    private CharacterCompositionDefinitionEditorView.OnAreaChanged areaDetailsUpdater(@NonNull ViewGroup areaDetails) {
        return area -> {
            updateAreaDetailsEntry(areaDetails.findViewById(R.id.areaDetailsX), area.getX());
            updateAreaDetailsEntry(areaDetails.findViewById(R.id.areaDetailsY), area.getY());
            updateAreaDetailsEntry(areaDetails.findViewById(R.id.areaDetailsWidth), area.getWidth());
            updateAreaDetailsEntry(areaDetails.findViewById(R.id.areaDetailsHeight), area.getHeight());
        };
    }

    private void setAreaDetails(
            @NonNull ViewGroup areaDetails,
            @StringRes int headerText,
            @NonNull CharacterCompositionDefinitionArea area,
            @NonNull Supplier<CharacterCompositionDefinitionAreaInterface> areaSupplier,
            @NonNull Procedure<CharacterCompositionDefinitionArea> areaSetter) {
        areaDetails.<TextView>findViewById(R.id.header).setText(headerText);
        setAreaDetailsEntry(areaDetails.findViewById(R.id.areaDetailsX), R.string.characterCompositionEditorXLabel, area.x,
                view -> {
                    final CharacterCompositionDefinitionAreaInterface vArea = areaSupplier.supply();
                    if (vArea.getX() > 0) {
                        areaSetter.apply(new CharacterCompositionDefinitionArea(vArea.getX() - 1, vArea.getY(), vArea.getWidth(), vArea.getHeight()));
                    }
                },
                view -> {
                    final CharacterCompositionDefinitionAreaInterface vArea = areaSupplier.supply();
                    if (vArea.getX() + vArea.getWidth() < CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) {
                        areaSetter.apply(new CharacterCompositionDefinitionArea(vArea.getX() + 1, vArea.getY(), vArea.getWidth(), vArea.getHeight()));
                    }
                });
        setAreaDetailsEntry(areaDetails.findViewById(R.id.areaDetailsY), R.string.characterCompositionEditorYLabel, area.y,
                view -> {
                    final CharacterCompositionDefinitionAreaInterface vArea = areaSupplier.supply();
                    if (vArea.getY() > 0) {
                        areaSetter.apply(new CharacterCompositionDefinitionArea(vArea.getX(), vArea.getY() - 1, vArea.getWidth(), vArea.getHeight()));
                    }
                },
                view -> {
                    final CharacterCompositionDefinitionAreaInterface vArea = areaSupplier.supply();
                    if (vArea.getY() + vArea.getHeight() < CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) {
                        areaSetter.apply(new CharacterCompositionDefinitionArea(vArea.getX(), vArea.getY() + 1, vArea.getWidth(), vArea.getHeight()));
                    }
                });
        setAreaDetailsEntry(areaDetails.findViewById(R.id.areaDetailsWidth), R.string.characterCompositionEditorWidthLabel, area.width,
                view -> {
                    final CharacterCompositionDefinitionAreaInterface vArea = areaSupplier.supply();
                    if (vArea.getWidth() > 1) {
                        areaSetter.apply(new CharacterCompositionDefinitionArea(vArea.getX(), vArea.getY(), vArea.getWidth() - 1, vArea.getHeight()));
                    }
                },
                view -> {
                    final CharacterCompositionDefinitionAreaInterface vArea = areaSupplier.supply();
                    if (vArea.getX() + vArea.getWidth() < CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) {
                        areaSetter.apply(new CharacterCompositionDefinitionArea(vArea.getX(), vArea.getY(), vArea.getWidth() + 1, vArea.getHeight()));
                    }
                });
        setAreaDetailsEntry(areaDetails.findViewById(R.id.areaDetailsHeight), R.string.characterCompositionEditorHeightLabel, area.height,
                view -> {
                    final CharacterCompositionDefinitionAreaInterface vArea = areaSupplier.supply();
                    if (vArea.getHeight() > 1) {
                        areaSetter.apply(new CharacterCompositionDefinitionArea(vArea.getX(), vArea.getY(), vArea.getWidth(), vArea.getHeight() - 1));
                    }
                },
                view -> {
                    final CharacterCompositionDefinitionAreaInterface vArea = areaSupplier.supply();
                    if (vArea.getY() + vArea.getHeight() < CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT) {
                        areaSetter.apply(new CharacterCompositionDefinitionArea(vArea.getX(), vArea.getY(), vArea.getWidth(), vArea.getHeight() + 1));
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.character_composition_definition_editor_activity);

        final CharacterCompositionTypeId typeId = getCharacterCompositionTypeId();
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final String text = checker.readConceptText(typeId.getConceptId(), preferredAlphabet);
        setTitle(text);

        CharacterCompositionDefinitionRegister register = checker.getCharacterCompositionDefinition(typeId);
        if (register == null) {
            final CharacterCompositionDefinitionArea defaultArea = new CharacterCompositionDefinitionArea(0, 0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT);
            register = new CharacterCompositionDefinitionRegister(defaultArea, defaultArea);
        }

        _editorView = findViewById(R.id.editorView);
        _editorView.setRegister(register);

        final LinearLayout firstAreaDetails = findViewById(R.id.firstAreaDetails);
        setAreaDetails(firstAreaDetails, R.string.characterCompositionEditorFirstLabel, register.first, _editorView::getFirstArea, _editorView::setFirstRegisterArea);

        final LinearLayout secondAreaDetails = findViewById(R.id.secondAreaDetails);
        setAreaDetails(secondAreaDetails, R.string.characterCompositionEditorSecondLabel, register.second, _editorView::getSecondArea, _editorView::setSecondRegisterArea);

        _editorView.setOnFirstAreaChanged(areaDetailsUpdater(firstAreaDetails));
        _editorView.setOnSecondAreaChanged(areaDetailsUpdater(secondAreaDetails));

        findViewById(R.id.saveButton).setOnClickListener(view -> saveAndClose());
    }

    private void saveAndClose() {
        final CharacterCompositionDefinitionArea first = CharacterCompositionDefinitionArea.cloneFrom(_editorView.getFirstArea());
        final CharacterCompositionDefinitionArea second = CharacterCompositionDefinitionArea.cloneFrom(_editorView.getSecondArea());
        final CharacterCompositionDefinitionRegister newRegister = new CharacterCompositionDefinitionRegister(first, second);
        if (DbManager.getInstance().getManager().updateCharacterCompositionDefinition(getCharacterCompositionTypeId(), newRegister)) {
            setResult(RESULT_OK);
            finish();
        }
        else {
            Toast.makeText(this, R.string.updateCharacterCompositionDefinitionError, Toast.LENGTH_SHORT).show();
        }
    }
}
