package sword.langbook3.android.activities.delegates;

import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import sword.collections.Procedure;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.CharacterCompositionDefinitionEditorView;
import sword.langbook3.android.R;
import sword.langbook3.android.collections.Procedure2;
import sword.langbook3.android.collections.Supplier;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.models.CharacterCompositionDefinitionArea;
import sword.langbook3.android.models.CharacterCompositionDefinitionAreaInterface;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;

public final class CharacterCompositionDefinitionEditorActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> {
    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    public interface ResultKeys {
        String CHARACTER_COMPOSITION_TYPE_ID = BundleKeys.CHARACTER_COMPOSITION_TYPE_ID;
    }

    private Activity _activity;
    private Controller _controller;
    private CharacterCompositionDefinitionEditorView _editorView;

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
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        activity.setContentView(R.layout.character_composition_definition_editor_activity);

        _controller = activity.getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _controller.load(activity, (title, register) -> {
            activity.setTitle(title);

            _editorView = activity.findViewById(R.id.editorView);
            _editorView.setRegister(register);

            final LinearLayout firstAreaDetails = activity.findViewById(R.id.firstAreaDetails);
            setAreaDetails(firstAreaDetails, R.string.characterCompositionEditorFirstLabel, register.first, _editorView::getFirstArea, _editorView::setFirstRegisterArea);

            final LinearLayout secondAreaDetails = activity.findViewById(R.id.secondAreaDetails);
            setAreaDetails(secondAreaDetails, R.string.characterCompositionEditorSecondLabel, register.second, _editorView::getSecondArea, _editorView::setSecondRegisterArea);

            _editorView.setOnFirstAreaChanged(areaDetailsUpdater(firstAreaDetails));
            _editorView.setOnSecondAreaChanged(areaDetailsUpdater(secondAreaDetails));

            activity.findViewById(R.id.saveButton).setOnClickListener(view -> saveAndClose());
        });
    }

    private void saveAndClose() {
        final CharacterCompositionDefinitionArea first = CharacterCompositionDefinitionArea.cloneFrom(_editorView.getFirstArea());
        final CharacterCompositionDefinitionArea second = CharacterCompositionDefinitionArea.cloneFrom(_editorView.getSecondArea());
        final CharacterCompositionDefinitionRegister newRegister = new CharacterCompositionDefinitionRegister(first, second);
        _controller.save(_activity, newRegister);
    }

    public interface Controller extends Parcelable {
        void load(@NonNull ActivityInterface activity, @NonNull Procedure2<String, CharacterCompositionDefinitionRegister> procedure);
        void save(@NonNull ActivityExtensions activity, @NonNull CharacterCompositionDefinitionRegister register);
    }
}
