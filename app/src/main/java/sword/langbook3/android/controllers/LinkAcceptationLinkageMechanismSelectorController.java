package sword.langbook3.android.controllers;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.R;
import sword.langbook3.android.activities.delegates.LinkageMechanismSelectorActivityDelegate;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.presenters.Presenter;

public final class LinkAcceptationLinkageMechanismSelectorController implements LinkageMechanismSelectorActivityDelegate.Controller {

    @NonNull
    private final AcceptationId _sourceAcceptation;

    @NonNull
    private final AcceptationId _targetAcceptation;

    public LinkAcceptationLinkageMechanismSelectorController(
            @NonNull AcceptationId sourceAcceptation,
            @NonNull AcceptationId targetAcceptation) {
        ensureNonNull(sourceAcceptation, targetAcceptation);
        ensureValidArguments(!DbManager.getInstance().getManager().conceptFromAcceptation(sourceAcceptation).equals(DbManager.getInstance().getManager().conceptFromAcceptation(targetAcceptation)));
        _sourceAcceptation = sourceAcceptation;
        _targetAcceptation = targetAcceptation;
    }

    @NonNull
    @Override
    public String getSourceWord() {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        return DbManager.getInstance().getManager().getAcceptationDisplayableText(_sourceAcceptation, preferredAlphabet);
    }

    @NonNull
    @Override
    public String getTargetWord() {
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        return DbManager.getInstance().getManager().getAcceptationDisplayableText(_targetAcceptation, preferredAlphabet);
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull LinkageOption option) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ConceptId concept = manager.conceptFromAcceptation(_sourceAcceptation);
        final boolean ok;
        final @StringRes int message;
        if (option == LinkageOption.SHARE_CONCEPT) {
            ok = manager.shareConcept(_targetAcceptation, concept);
            message = ok? R.string.conceptSharedFeedback : R.string.conceptSharedKo;
        }
        else {
            ok = true;
            manager.duplicateAcceptationWithThisConcept(_targetAcceptation, concept);
            message = R.string.acceptationLinkedFeedback;
        }

        presenter.displayFeedback(message);
        if (ok) {
            presenter.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        AcceptationIdParceler.write(dest, _sourceAcceptation);
        AcceptationIdParceler.write(dest, _targetAcceptation);
    }

    public static final Parcelable.Creator<LinkAcceptationLinkageMechanismSelectorController> CREATOR = new Parcelable.Creator<LinkAcceptationLinkageMechanismSelectorController>() {

        @Override
        public LinkAcceptationLinkageMechanismSelectorController createFromParcel(Parcel source) {
            final AcceptationId sourceAcceptation = AcceptationIdParceler.read(source);
            final AcceptationId targetAcceptation = AcceptationIdParceler.read(source);
            return new LinkAcceptationLinkageMechanismSelectorController(sourceAcceptation, targetAcceptation);
        }

        @Override
        public LinkAcceptationLinkageMechanismSelectorController[] newArray(int size) {
            return new LinkAcceptationLinkageMechanismSelectorController[size];
        }
    };
}
