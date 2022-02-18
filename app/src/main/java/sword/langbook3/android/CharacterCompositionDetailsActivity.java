package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CharacterIdBundler;
import sword.langbook3.android.models.CharacterCompositionDetailsModel;

public final class CharacterCompositionDetailsActivity extends Activity implements AdapterView.OnItemClickListener {

    private CharacterCompositionDetailsAdapter _adapter;

    private interface ArgKeys {
        String CHARACTER = BundleKeys.CHARACTER;
    }

    public static void open(Context context, CharacterId characterId) {
        Intent intent = new Intent(context, CharacterCompositionDetailsActivity.class);
        CharacterIdBundler.writeAsIntentExtra(intent, ArgKeys.CHARACTER, characterId);
        context.startActivity(intent);
    }

    private CharacterId _characterId;
    private CharacterCompositionDetailsModel<CharacterId, AcceptationId> _model;

    private void updateModelAndUi() {
        _model = DbManager.getInstance().getManager().getCharacterCompositionDetails(_characterId);

        if (_model != null) {
            _adapter.setModel(_model);
        }
        else {
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.character_details_activity);

        _adapter = new CharacterCompositionDetailsAdapter();
        final ListView listView = findViewById(R.id.listView);
        listView.setAdapter(_adapter);
        listView.setOnItemClickListener(this);
        _characterId = CharacterIdBundler.readAsIntentExtra(getIntent(), ArgKeys.CHARACTER);
        updateModelAndUi();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final AcceptationId acceptation = _model.acceptationsWhereIncluded.keyAt(position - 2);
        AcceptationDetailsActivity.open(this, acceptation);
    }
}
