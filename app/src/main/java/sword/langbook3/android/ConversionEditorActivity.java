package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.database.Database;

import static sword.langbook3.android.LangbookReadableDatabase.getConversion;
import static sword.langbook3.android.LangbookReadableDatabase.readConceptText;

public final class ConversionEditorActivity extends Activity implements ListView.OnItemClickListener {

    private interface ArgKeys {
        String SOURCE_ALPHABET = BundleKeys.SOURCE_ALPHABET;
        String TARGET_ALPHABET = BundleKeys.TARGET_ALPHABET;
    }

    private interface SavedKeys {
        String STATE = "st";
    }

    private ImmutableSet<ImmutablePair<String, String>> _conversion;
    private ConversionEditorActivityState _state;

    public static void open(Context context, int sourceAlphabet, int targetAlphabet) {
        final Intent intent = new Intent(context, ConversionEditorActivity.class);
        intent.putExtra(ArgKeys.SOURCE_ALPHABET, sourceAlphabet);
        intent.putExtra(ArgKeys.TARGET_ALPHABET, targetAlphabet);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversion_details_activity);

        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final Database db = DbManager.getInstance().getDatabase();
        final int sourceAlphabet = getIntent().getIntExtra(ArgKeys.SOURCE_ALPHABET, 0);
        final int targetAlphabet = getIntent().getIntExtra(ArgKeys.TARGET_ALPHABET, 0);

        final String sourceText = readConceptText(db, sourceAlphabet, preferredAlphabet);
        final String targetText = readConceptText(db, targetAlphabet, preferredAlphabet);
        setTitle(sourceText + " -> " + targetText);

        _conversion = getConversion(db, new ImmutableIntPair(sourceAlphabet, targetAlphabet));

        if (savedInstanceState == null) {
            _state = new ConversionEditorActivityState();
        }
        else {
            _state = savedInstanceState.getParcelable(SavedKeys.STATE);
        }

        final ListView listView = findViewById(R.id.listView);
        listView.setAdapter(new ConversionEditorAdapter(_conversion, _state.getRemoved(), _state.getAdded()));
        listView.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final ConversionEditorAdapter adapter = (ConversionEditorAdapter) parent.getAdapter();
        final ConversionEditorAdapter.Entry entry = adapter.getItem(position);
        final int convPos = entry.getConversionPosition();
        if (convPos >= 0) {
            _state.toggleRemoved(convPos);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(SavedKeys.STATE, _state);
    }
}
