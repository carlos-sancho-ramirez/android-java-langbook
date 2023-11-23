package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.AcceptationConfirmationActivity;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.activities.delegates.AcceptationConfirmationActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddAlphabetAcceptationConfirmationController implements AcceptationConfirmationActivityDelegate.Controller {

    @NonNull
    private final LanguageId _language;

    @NonNull
    private final AcceptationId _acceptation;

    public AddAlphabetAcceptationConfirmationController(@NonNull LanguageId language, @NonNull AcceptationId acceptation) {
        ensureNonNull(language, acceptation);
        _language = language;
        _acceptation = acceptation;
    }

    @NonNull
    @Override
    public AcceptationId getAcceptation() {
        return _acceptation;
    }

    @Override
    public void confirm(@NonNull Presenter presenter) {
        final AlphabetId targetAlphabet = AlphabetIdManager.conceptAsAlphabetId(DbManager.getInstance().getManager().conceptFromAcceptation(_acceptation));
        presenter.openSourceAlphabetPicker(AcceptationConfirmationActivityDelegate.REQUEST_CODE_NEXT_STEP, new AddAlphabetSourceAlphabetPickerControllerForSelectedAcceptation(_language, targetAlphabet));
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == AcceptationConfirmationActivityDelegate.REQUEST_CODE_NEXT_STEP && resultCode == Activity.RESULT_OK) {
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
        AcceptationIdParceler.write(dest, _acceptation);
    }

    public static final Creator<AddAlphabetAcceptationConfirmationController> CREATOR = new Creator<AddAlphabetAcceptationConfirmationController>() {

        @Override
        public AddAlphabetAcceptationConfirmationController createFromParcel(Parcel source) {
            final LanguageId language = LanguageIdParceler.read(source);
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            return new AddAlphabetAcceptationConfirmationController(language, acceptation);
        }

        @Override
        public AddAlphabetAcceptationConfirmationController[] newArray(int size) {
            return new AddAlphabetAcceptationConfirmationController[size];
        }
    };
}
