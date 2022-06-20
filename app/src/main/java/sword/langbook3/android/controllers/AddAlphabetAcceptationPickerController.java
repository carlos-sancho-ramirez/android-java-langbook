package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.AcceptationPickerActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdManager;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAlphabetAcceptationPickerController implements AcceptationPickerActivity.Controller {

    @NonNull
    private final LanguageId _language;

    public AddAlphabetAcceptationPickerController(@NonNull LanguageId language) {
        ensureNonNull(language);
        _language = language;
    }

    @Override
    public void createAcceptation(@NonNull Presenter presenter, String query) {
        new AddAlphabetLanguagePickerController(_language, query)
                .fire(presenter, AcceptationPickerActivity.REQUEST_CODE_NEW_ACCEPTATION);
    }

    @Override
    public void selectAcceptation(@NonNull Presenter presenter, @NonNull AcceptationId acceptation) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final AlphabetId targetAlphabet = AlphabetIdManager.conceptAsAlphabetId(checker.conceptFromAcceptation(acceptation));
        if (checker.isAlphabetPresent(targetAlphabet)) {
            presenter.displayFeedback(R.string.alreadyUsedAsAlphabet);
        }
        else {
            presenter.openAcceptationConfirmation(AcceptationPickerActivity.REQUEST_CODE_CONFIRM, new AddAlphabetAcceptationConfirmationController(_language, acceptation));
        }
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data, AcceptationId confirmDynamicAcceptation) {
        if (resultCode == Activity.RESULT_OK && (requestCode == AcceptationPickerActivity.REQUEST_CODE_CONFIRM || requestCode == AcceptationPickerActivity.REQUEST_CODE_NEW_ACCEPTATION)) {
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        LanguageIdParceler.write(dest, _language);
    }

    public static final Creator<AddAlphabetAcceptationPickerController> CREATOR = new Creator<AddAlphabetAcceptationPickerController>() {

        @Override
        public AddAlphabetAcceptationPickerController createFromParcel(Parcel source) {
            final LanguageId language = LanguageIdParceler.read(source);
            return new AddAlphabetAcceptationPickerController(language);
        }

        @Override
        public AddAlphabetAcceptationPickerController[] newArray(int size) {
            return new AddAlphabetAcceptationPickerController[size];
        }
    };
}
