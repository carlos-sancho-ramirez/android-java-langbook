package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.database.DbQuery;
import sword.langbook3.android.AcceptationPickerActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.FixedTextAcceptationPickerActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.models.SearchResult;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class PickSentenceSpanFixedTextAcceptationPickerController implements FixedTextAcceptationPickerActivity.Controller, Fireable {

    @NonNull
    private final String _text;

    public PickSentenceSpanFixedTextAcceptationPickerController(@NonNull String text) {
        ensureNonNull(text);
        _text = text;
    }

    @Override
    public void fire(@NonNull Presenter presenter, int requestCode) {
        // This can be optimised, as we are only interested in checking if
        // there is at least 1 acceptation matching exactly the text. We do not
        // need rules nor the actual acceptations
        // TODO: Optimise this database query
        final ImmutableList<SearchResult<AcceptationId, RuleId>> results = DbManager.getInstance().getManager().findAcceptationAndRulesFromText(_text, DbQuery.RestrictionStringTypes.EXACT, new ImmutableIntRange(0, 0));

        if (results.isEmpty()) {
            createAcceptation(presenter, requestCode);
        }
        else {
            presenter.openFixedTextAcceptationPicker(requestCode, this);
        }
    }

    @NonNull
    @Override
    public String getText() {
        return _text;
    }

    private void createAcceptation(@NonNull Presenter presenter, int requestCode) {
        new PickSentenceSpanLanguagePickerController(_text)
                .fire(presenter, requestCode);
    }

    @Override
    public void createAcceptation(@NonNull Presenter presenter) {
        createAcceptation(presenter, FixedTextAcceptationPickerActivity.REQUEST_CODE_NEW_ACCEPTATION);
    }

    @Override
    public void selectAcceptation(@NonNull Presenter presenter, @NonNull AcceptationId acceptation) {
        presenter.openAcceptationConfirmation(AcceptationPickerActivity.REQUEST_CODE_CONFIRM, new AcceptationConfirmationController(acceptation));
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && (requestCode == AcceptationPickerActivity.REQUEST_CODE_NEW_ACCEPTATION || requestCode == AcceptationPickerActivity.REQUEST_CODE_CONFIRM)) {
            activity.setResult(Activity.RESULT_OK, data);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_text);
    }

    public static final Creator<PickSentenceSpanFixedTextAcceptationPickerController> CREATOR = new Creator<PickSentenceSpanFixedTextAcceptationPickerController>() {

        @Override
        public PickSentenceSpanFixedTextAcceptationPickerController createFromParcel(Parcel source) {
            final String text = source.readString();
            return new PickSentenceSpanFixedTextAcceptationPickerController(text);
        }

        @Override
        public PickSentenceSpanFixedTextAcceptationPickerController[] newArray(int size) {
            return new PickSentenceSpanFixedTextAcceptationPickerController[size];
        }
    };
}
