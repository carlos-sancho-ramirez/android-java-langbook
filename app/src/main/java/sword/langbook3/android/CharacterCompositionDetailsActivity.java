package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import sword.langbook3.android.db.CharacterId;
import sword.langbook3.android.db.CharacterIdBundler;
import sword.langbook3.android.models.CharacterCompositionDetailsModel;

import static sword.langbook3.android.models.CharacterCompositionDetailsModel.Part.INVALID_CHARACTER;

public final class CharacterCompositionDetailsActivity extends Activity {

    private interface ArgKeys {
        String CHARACTER = BundleKeys.CHARACTER;
    }

    public static void open(Context context, CharacterId characterId) {
        Intent intent = new Intent(context, CharacterCompositionDetailsActivity.class);
        CharacterIdBundler.writeAsIntentExtra(intent, ArgKeys.CHARACTER, characterId);
        context.startActivity(intent);
    }

    private CharacterId _characterId;
    private CharacterCompositionDetailsModel<CharacterId> _model;

    private String representChar(char ch) {
        return (ch == INVALID_CHARACTER)? "?" : "" + ch;
    }

    private void updateModelAndUi() {
        _model = DbManager.getInstance().getManager().getCharacterCompositionDetails(_characterId);

        if (_model != null) {
            String mainText = representChar(_model.character);
            setTitle(mainText);
            this.<TextView>findViewById(R.id.charBigScale).setText(mainText);

            final TextView firstTextView = findViewById(R.id.first);
            firstTextView.setText(representChar(_model.first.character));

            if (_model.first.isComposition) {
                final CharacterId firstId = _model.first.id;
                firstTextView.setOnClickListener(v -> CharacterCompositionDetailsActivity.open(this, firstId));
            }

            final TextView secondTextView = findViewById(R.id.second);
            secondTextView.setText(representChar(_model.second.character));

            if (_model.second.isComposition) {
                final CharacterId secondId = _model.second.id;
                secondTextView.setOnClickListener(v -> CharacterCompositionDetailsActivity.open(this, secondId));
            }

            this.<TextView>findViewById(R.id.compositionTypeInfo).setText(getString(R.string.characterCompositionType, Integer.toString(_model.compositionType)));
        }
        else {
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.character_details_activity);

        _characterId = CharacterIdBundler.readAsIntentExtra(getIntent(), ArgKeys.CHARACTER);
        updateModelAndUi();
    }
}
