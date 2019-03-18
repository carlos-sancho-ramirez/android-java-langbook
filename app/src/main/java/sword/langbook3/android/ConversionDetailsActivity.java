package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.database.Database;

import static sword.langbook3.android.LangbookReadableDatabase.getConversion;
import static sword.langbook3.android.LangbookReadableDatabase.readConceptText;

public final class ConversionDetailsActivity extends Activity {

    private interface ArgKeys {
        String SOURCE_ALPHABET = BundleKeys.SOURCE_ALPHABET;
        String TARGET_ALPHABET = BundleKeys.TARGET_ALPHABET;
    }

    public static void open(Context context, int sourceAlphabet, int targetAlphabet) {
        final Intent intent = new Intent(context, ConversionDetailsActivity.class);
        intent.putExtra(ArgKeys.SOURCE_ALPHABET, sourceAlphabet);
        intent.putExtra(ArgKeys.TARGET_ALPHABET, targetAlphabet);
        context.startActivity(intent);
    }

    private int getSourceAlphabet() {
        return getIntent().getIntExtra(ArgKeys.SOURCE_ALPHABET, 0);
    }

    private int getTargetAlphabet() {
        return getIntent().getIntExtra(ArgKeys.TARGET_ALPHABET, 0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversion_details_activity);

        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final Database db = DbManager.getInstance().getDatabase();
        final int sourceAlphabet = getSourceAlphabet();
        final int targetAlphabet = getTargetAlphabet();

        final String sourceText = readConceptText(db, sourceAlphabet, preferredAlphabet);
        final String targetText = readConceptText(db, targetAlphabet, preferredAlphabet);
        setTitle(sourceText + " -> " + targetText);

        final ImmutableSet<ImmutablePair<String, String>> conversion = getConversion(db, new ImmutableIntPair(sourceAlphabet, targetAlphabet));

        final ListView listView = findViewById(R.id.listView);
        listView.setAdapter(new ConversionDetailsAdapter(conversion));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conversion_details_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemEdit:
                ConversionEditorActivity.open(this, getSourceAlphabet(), getTargetAlphabet());
                return true;
        }

        return false;
    }
}
