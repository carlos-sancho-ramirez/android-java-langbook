package sword.langbook3.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.database.DbQuery;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.models.SearchResult;

public final class FixedTextAcceptationPickerActivity extends SearchActivity {

    interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    public static void open(@NonNull Activity activity, int requestCode, @NonNull Controller controller) {
        final Intent intent = new Intent(activity, FixedTextAcceptationPickerActivity.class);
        intent.putExtra(ArgKeys.CONTROLLER, controller);
        activity.startActivityForResult(intent, requestCode);
    }

    private Controller _controller;
    private ImmutableMap<RuleId, String> _ruleTexts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        _controller = getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _query = _controller.getText();
        super.onCreate(savedInstanceState);
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
    ImmutableList<SearchResult<AcceptationId, RuleId>> queryAcceptationResults(String query) {
        return DbManager.getInstance().getManager().findAcceptationAndRulesFromText(query, getSearchRestrictionType(), new ImmutableIntRange(0, MAX_RESULTS - 1));
    }

    @Override
    SearchResultAdapter createAdapter(ImmutableList<SearchResult> results) {
        if (_ruleTexts == null) {
            final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
            _ruleTexts = DbManager.getInstance().getManager().readAllRules(preferredAlphabet);
        }

        return new SearchResultAdapter(results, _ruleTexts);
    }

    @Override
    void openLanguagePicker(String query) {
        _controller.createAcceptation(this);
    }

    @Override
    void onAcceptationSelected(AcceptationId acceptation) {
        _controller.selectAcceptation(this, acceptation);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(this, requestCode, resultCode, data);
    }

    public interface Controller extends Parcelable {
        @NonNull
        String getText();
        void createAcceptation(@NonNull Activity activity);
        void selectAcceptation(@NonNull Activity activity, @NonNull AcceptationId acceptation);
        void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data);
    }
}
