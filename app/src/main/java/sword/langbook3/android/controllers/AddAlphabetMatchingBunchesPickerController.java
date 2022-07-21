package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.Set;
import sword.langbook3.android.MatchingBunchesPickerActivity;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.CorrelationArrayParceler;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdParceler;
import sword.langbook3.android.presenters.Presenter;

public final class AddAlphabetMatchingBunchesPickerController extends AbstractMatchingBunchesPickerController {

    @NonNull
    private final LanguageId _alphabetLanguage;

    public AddAlphabetMatchingBunchesPickerController(
            @NonNull LanguageId alphabetLanguage,
            @NonNull ImmutableCorrelationArray<AlphabetId> correlationArray) {
        super(correlationArray);
        _alphabetLanguage = alphabetLanguage;
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull Set<BunchId> selectedBunches) {
        presenter.openSourceAlphabetPicker(requestCode, new AddAlphabetSourceAlphabetPickerControllerForNewAcceptation(_alphabetLanguage, _correlationArray, selectedBunches));
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == MatchingBunchesPickerActivity.REQUEST_CODE_NEXT_STEP && resultCode == Activity.RESULT_OK) {
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        LanguageIdParceler.write(dest, _alphabetLanguage);
        CorrelationArrayParceler.write(dest, _correlationArray);
    }

    public static final Creator<AddAlphabetMatchingBunchesPickerController> CREATOR = new Creator<AddAlphabetMatchingBunchesPickerController>() {

        @Override
        public AddAlphabetMatchingBunchesPickerController createFromParcel(Parcel source) {
            final LanguageId alphabetLanguage = LanguageIdParceler.read(source);
            final ImmutableCorrelationArray<AlphabetId> correlationArray = CorrelationArrayParceler.read(source);
            return new AddAlphabetMatchingBunchesPickerController(alphabetLanguage, correlationArray);
        }

        @Override
        public AddAlphabetMatchingBunchesPickerController[] newArray(int size) {
            return new AddAlphabetMatchingBunchesPickerController[size];
        }
    };
}
