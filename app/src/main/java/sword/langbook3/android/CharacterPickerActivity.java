package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import sword.collections.ImmutableList;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.models.CharacterPickerItem;

public final class CharacterPickerActivity extends Activity {

    private interface ArgKeys {
        String CHARACTER_STRING = BundleKeys.CHARACTER_STRING;
    }

    public static void open(Context context, String characters) {
        final Intent intent = new Intent(context, CharacterPickerActivity.class);
        intent.putExtra(ArgKeys.CHARACTER_STRING, characters);
        context.startActivity(intent);
    }

    private String getCharacterString() {
        return getIntent().getStringExtra(ArgKeys.CHARACTER_STRING);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.character_picker_activity);

        final ImmutableList<CharacterPickerItem<CharacterId>> items = DbManager.getInstance().getManager().getCharacterPickerItems(getCharacterString());
        final ListView listView = findViewById(R.id.listView);
        listView.setAdapter(new CharacterPickerAdapter(items));
        listView.setOnItemClickListener((parent, view, position, id) ->
                CharacterCompositionDetailsActivity.open(CharacterPickerActivity.this, items.valueAt(position).id));
    }
}
