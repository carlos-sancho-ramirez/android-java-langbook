package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.ImmutableList;
import sword.collections.ImmutablePair;
import sword.collections.MapGetter;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.presenters.Presenter;

import static android.app.Activity.RESULT_OK;
import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddBunchFromAcceptationWordEditorController implements WordEditorActivity.Controller {

    @NonNull
    private final AcceptationId _acceptationToBeIncluded;

    @NonNull
    private final LanguageId _language;
    private final String _searchQuery;

    public AddBunchFromAcceptationWordEditorController(
            @NonNull AcceptationId acceptationToBeIncluded,
            @NonNull LanguageId language,
            String searchQuery) {
        ensureNonNull(acceptationToBeIncluded, language);
        _acceptationToBeIncluded = acceptationToBeIncluded;
        _language = language;
        _searchQuery = searchQuery;
    }

    @Override
    public void setTitle(@NonNull Activity activity) {
        // Nothing to be done
    }

    @Override
    public void updateConvertedTexts(@NonNull String[] texts, @NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions) {
        WordEditorControllerUtils.updateConvertedTexts(_language, texts, conversions);
    }

    @NonNull
    @Override
    public UpdateFieldsResult updateFields(@NonNull MapGetter<ImmutablePair<AlphabetId, AlphabetId>, Conversion<AlphabetId>> conversions, ImmutableList<String> texts) {
        return WordEditorControllerUtils.updateFields(conversions, _language, _searchQuery, texts);
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull ImmutableCorrelation<AlphabetId> texts) {
        new AddBunchFromAcceptationCorrelationPickerController(_acceptationToBeIncluded, texts)
                .fire(presenter, WordEditorActivity.REQUEST_CODE_CORRELATION_PICKER);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == WordEditorActivity.REQUEST_CODE_CORRELATION_PICKER && resultCode == RESULT_OK) {
            activity.setResult(RESULT_OK, data);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        AcceptationIdParceler.write(dest, _acceptationToBeIncluded);
        LanguageIdParceler.write(dest, _language);
        dest.writeString(_searchQuery);
    }

    public static final Parcelable.Creator<AddBunchFromAcceptationWordEditorController> CREATOR = new Parcelable.Creator<AddBunchFromAcceptationWordEditorController>() {

        @Override
        public AddBunchFromAcceptationWordEditorController createFromParcel(Parcel source) {
            final AcceptationId acceptationToBeIncluded = AcceptationIdParceler.read(source);
            final LanguageId language = LanguageIdParceler.read(source);
            final String searchQuery = source.readString();
            return new AddBunchFromAcceptationWordEditorController(acceptationToBeIncluded, language, searchQuery);
        }

        @Override
        public AddBunchFromAcceptationWordEditorController[] newArray(int size) {
            return new AddBunchFromAcceptationWordEditorController[size];
        }
    };
}
