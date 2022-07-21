package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.NonNull;
import sword.collections.ImmutableMap;
import sword.collections.Procedure;
import sword.collections.Set;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.MatchingBunchesPickerActivity;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

abstract class AbstractMatchingBunchesPickerController implements MatchingBunchesPickerActivity.Controller, Fireable {

    @NonNull
    final ImmutableCorrelationArray<AlphabetId> _correlationArray;

    AbstractMatchingBunchesPickerController(
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        ensureValidArguments(!correlationArray.isEmpty());
        _correlationArray = correlationArray;
    }

    @Override
    public void fire(@NonNull Presenter presenter, int requestCode) {
        // TODO: This can be optimised as no texts are required
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlationArray.concatenateTexts(), preferredAlphabet);

        if (bunches.isEmpty()) {
            complete(presenter, requestCode, bunches.keySet());
        }
        else {
            presenter.openMatchingBunchesPicker(requestCode, this);
        }
    }

    @Override
    public void loadBunches(@NonNull Presenter presenter, @NonNull Procedure<ImmutableMap<BunchId, String>> procedure) {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        ImmutableMap<BunchId, String> bunches = DbManager.getInstance().getManager().readAllMatchingBunches(_correlationArray.concatenateTexts(), preferredAlphabet);
        procedure.apply(bunches);
    }

    abstract void complete(@NonNull Presenter presenter, int requestCode, @NonNull Set<BunchId> selectedBunches);

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull Set<BunchId> selectedBunches) {
        complete(presenter, MatchingBunchesPickerActivity.REQUEST_CODE_NEXT_STEP, selectedBunches);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == MatchingBunchesPickerActivity.REQUEST_CODE_NEXT_STEP && resultCode == Activity.RESULT_OK) {
            activity.setResult(Activity.RESULT_OK, data);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
