package sword.langbook3.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import sword.langbook3.android.collections.ImmutableIntPair;
import sword.langbook3.android.db.LangbookChecker;
import sword.langbook3.android.models.Conversion;

public final class ConversionDetailsActivity extends Activity {

    private static final int REQUEST_CODE_EDITION = 1;

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

    public static void open(Activity activity, int requestCode, int sourceAlphabet, int targetAlphabet) {
        final Intent intent = new Intent(activity, ConversionDetailsActivity.class);
        intent.putExtra(ArgKeys.SOURCE_ALPHABET, sourceAlphabet);
        intent.putExtra(ArgKeys.TARGET_ALPHABET, targetAlphabet);
        activity.startActivityForResult(intent, requestCode);
    }

    private boolean _dataJustLoaded;

    private int getSourceAlphabet() {
        return getIntent().getIntExtra(ArgKeys.SOURCE_ALPHABET, 0);
    }

    private int getTargetAlphabet() {
        return getIntent().getIntExtra(ArgKeys.TARGET_ALPHABET, 0);
    }

    private void updateUi() {
        final int preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final LangbookChecker checker = DbManager.getInstance().getManager();
        final int sourceAlphabet = getSourceAlphabet();
        final int targetAlphabet = getTargetAlphabet();

        final String sourceText = checker.readConceptText(sourceAlphabet, preferredAlphabet);
        final String targetText = checker.readConceptText(targetAlphabet, preferredAlphabet);
        setTitle(sourceText + " -> " + targetText);

        final Conversion conversion = checker.getConversion(new ImmutableIntPair(sourceAlphabet, targetAlphabet));

        final ListView listView = findViewById(R.id.listView);
        listView.setAdapter(new ConversionDetailsAdapter(conversion));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversion_details_activity);

        updateUi();
        _dataJustLoaded = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EDITION && resultCode == RESULT_OK && !_dataJustLoaded) {
            updateUi();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.conversion_details_activity, menu);
        return true;
    }

    @Override
    public void onResume() {
        super.onResume();
        _dataJustLoaded = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemEdit:
                ConversionEditorActivity.open(this, REQUEST_CODE_EDITION, getSourceAlphabet(), getTargetAlphabet());
                return true;
        }

        return false;
    }
}
