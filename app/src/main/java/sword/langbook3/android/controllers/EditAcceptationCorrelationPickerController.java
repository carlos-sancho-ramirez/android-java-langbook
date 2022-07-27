package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class EditAcceptationCorrelationPickerController extends AbstractCorrelationPickerController {

    @NonNull
    private final AcceptationId _acceptation;

    public EditAcceptationCorrelationPickerController(
            @NonNull AcceptationId acceptation,
            @NonNull ImmutableCorrelation<AlphabetId> texts) {
        super(texts);
        ensureNonNull(acceptation);
        _acceptation = acceptation;
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        DbManager.getInstance().getManager().updateAcceptationCorrelationArray(_acceptation, selectedOption);
        presenter.finish();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        AcceptationIdParceler.write(dest, _acceptation);
        CorrelationParceler.write(dest, _texts);
    }

    public static final Creator<EditAcceptationCorrelationPickerController> CREATOR = new Creator<EditAcceptationCorrelationPickerController>() {

        @Override
        public EditAcceptationCorrelationPickerController createFromParcel(Parcel source) {
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            final ImmutableCorrelation<AlphabetId> texts = CorrelationParceler.read(source).toImmutable();
            return new EditAcceptationCorrelationPickerController(acceptation, texts);
        }

        @Override
        public EditAcceptationCorrelationPickerController[] newArray(int size) {
            return new EditAcceptationCorrelationPickerController[size];
        }
    };
}
