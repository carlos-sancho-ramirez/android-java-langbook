package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import sword.langbook3.android.db.DbQuery;

public final class FixedTextAcceptationPickerActivity extends SearchActivity {

    private static final int REQUEST_CODE_VIEW_DETAILS = 1;

    interface ResultKeys {
        String ACCEPTATION = BundleKeys.ACCEPTATION;
    }

    public static void open(Activity activity, int requestCode, String text) {
        final Intent intent = new Intent(activity, FixedTextAcceptationPickerActivity.class);
        intent.putExtra(ArgKeys.TEXT, text);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    boolean isQueryModifiable() {
        return false;
    }

    @Override
    int getSearchRestrictionType() {
        return DbQuery.RestrictionStringTypes.EXACT;
    }

    @Override
    void openLanguagePicker(int requestCode, String query) {
        LanguagePickerActivity.open(this, requestCode, query);
    }

    @Override
    void onAcceptationSelected(int staticAcceptation, int dynamicAcceptation) {
        AcceptationDetailsActivity.open(this, REQUEST_CODE_VIEW_DETAILS, staticAcceptation, dynamicAcceptation, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            final Intent intent = new Intent();
            final String key = (requestCode == REQUEST_CODE_VIEW_DETAILS)?
                    AcceptationDetailsActivity.ResultKeys.ACCEPTATION :
                    LanguagePickerActivity.ResultKeys.ACCEPTATION;
            intent.putExtra(ResultKeys.ACCEPTATION, data.getIntExtra(key, 0));
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
