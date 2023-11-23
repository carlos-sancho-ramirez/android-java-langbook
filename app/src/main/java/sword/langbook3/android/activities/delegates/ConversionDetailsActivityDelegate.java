package sword.langbook3.android.activities.delegates;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.NonNull;

import sword.collections.ImmutablePair;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.ConversionDetailsAdapter;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.Intentions;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdBundler;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.models.Conversion;

public final class ConversionDetailsActivityDelegate<Activity extends ActivityExtensions> extends AbstractActivityDelegate<Activity> {
    private static final int REQUEST_CODE_EDITION = 1;

    public interface ArgKeys {
        String SOURCE_ALPHABET = BundleKeys.SOURCE_ALPHABET;
        String TARGET_ALPHABET = BundleKeys.TARGET_ALPHABET;
    }

    private Activity _activity;
    private boolean _dataJustLoaded;

    private AlphabetId getSourceAlphabet() {
        return AlphabetIdBundler.readAsIntentExtra(_activity.getIntent(), ArgKeys.SOURCE_ALPHABET);
    }

    private AlphabetId getTargetAlphabet() {
        return AlphabetIdBundler.readAsIntentExtra(_activity.getIntent(), ArgKeys.TARGET_ALPHABET);
    }

    private void updateUi() {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final AlphabetId sourceAlphabet = getSourceAlphabet();
        final AlphabetId targetAlphabet = getTargetAlphabet();

        final String sourceText = checker.readConceptText(sourceAlphabet.getConceptId(), preferredAlphabet);
        final String targetText = checker.readConceptText(targetAlphabet.getConceptId(), preferredAlphabet);
        _activity.setTitle(sourceText + " -> " + targetText);

        final Conversion<AlphabetId> conversion = checker.getConversion(new ImmutablePair<>(sourceAlphabet, targetAlphabet));

        final ListView listView = _activity.findViewById(R.id.listView);
        listView.setAdapter(new ConversionDetailsAdapter(conversion));
    }

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _activity = activity;
        activity.setContentView(R.layout.conversion_details_activity);

        updateUi();
        _dataJustLoaded = true;
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EDITION && resultCode == RESULT_OK && !_dataJustLoaded) {
            updateUi();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Activity activity, Menu menu) {
        activity.getMenuInflater().inflate(R.menu.conversion_details_activity, menu);
        return true;
    }

    @Override
    public void onResume(@NonNull Activity activity) {
        _dataJustLoaded = false;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull Activity activity, MenuItem item) {
        if (item.getItemId() == R.id.menuItemEdit) {
            Intentions.editConversion(activity, REQUEST_CODE_EDITION, getSourceAlphabet(), getTargetAlphabet());
            return true;
        }

        return false;
    }
}
