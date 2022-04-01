package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import sword.langbook3.android.db.CharacterCompositionTypeId;
import sword.langbook3.android.db.CharacterCompositionTypeIdBundler;
import sword.langbook3.android.models.CharacterCompositionDefinitionRegister;

public final class CharacterCompositionDefinitionEditorActivity extends Activity {

    private interface ArgKeys {
        String ID = "id";
    }

    public static void open(Context context, CharacterCompositionTypeId id) {
        final Intent intent = new Intent(context, CharacterCompositionDefinitionEditorActivity.class);
        CharacterCompositionTypeIdBundler.writeAsIntentExtra(intent, ArgKeys.ID, id);
        context.startActivity(intent);
    }

    private CharacterCompositionTypeId getCharacterCompositionTypeId() {
        return CharacterCompositionTypeIdBundler.readAsIntentExtra(getIntent(), ArgKeys.ID);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.character_composition_definition_editor_activity);

        final CharacterCompositionDefinitionRegister register = DbManager.getInstance().getManager().getCharacterCompositionDefinition(getCharacterCompositionTypeId());
        final CharacterCompositionDefinitionEditorView view = findViewById(R.id.editorView);
        view.setRegister(register);
    }
}
