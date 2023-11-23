package sword.langbook3.android.activities.delegates;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.database.DbQuery;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.FixedTextAcceptationPickerActivity;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.SearchResultAdapter;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.models.SearchResult;
import sword.langbook3.android.presenters.DefaultPresenter;
import sword.langbook3.android.presenters.Presenter;

public final class FixedTextAcceptationPickerActivityDelegate<Activity extends ActivityExtensions> extends SearchActivityDelegate<Activity> {

    public interface ArgKeys {
        String CONTROLLER = BundleKeys.CONTROLLER;
    }

    private FixedTextAcceptationPickerActivityDelegate.Controller _controller;
    private ImmutableMap<RuleId, String> _ruleTexts;

    @Override
    public void onCreate(@NonNull Activity activity, Bundle savedInstanceState) {
        _controller = activity.getIntent().getParcelableExtra(ArgKeys.CONTROLLER);
        _query = _controller.getText();
        super.onCreate(activity, savedInstanceState);
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
        _controller.createAcceptation(new DefaultPresenter(_activity));
    }

    @Override
    void onAcceptationSelected(AcceptationId acceptation) {
        _controller.selectAcceptation(new DefaultPresenter(_activity), acceptation);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        _controller.onActivityResult(activity, requestCode, resultCode, data);
    }

    public interface Controller extends Parcelable {
        @NonNull
        String getText();
        void createAcceptation(@NonNull Presenter presenter);
        void selectAcceptation(@NonNull Presenter presenter, @NonNull AcceptationId acceptation);
        void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data);
    }
}
