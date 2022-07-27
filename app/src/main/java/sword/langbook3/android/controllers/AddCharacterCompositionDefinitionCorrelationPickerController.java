package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CorrelationParceler;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.presenters.Presenter;

public final class AddCharacterCompositionDefinitionCorrelationPickerController extends AbstractCorrelationPickerController {

    public AddCharacterCompositionDefinitionCorrelationPickerController(
            @NonNull ImmutableCorrelation<AlphabetId> texts) {
        super(texts);
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull ImmutableCorrelationArray<AlphabetId> selectedOption) {
        new AddCharacterCompositionDefinitionMatchingBunchesPickerController(_texts, selectedOption)
                .fire(presenter, requestCode);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        CorrelationParceler.write(dest, _texts);
    }

    public static final Creator<AddCharacterCompositionDefinitionCorrelationPickerController> CREATOR = new Creator<AddCharacterCompositionDefinitionCorrelationPickerController>() {

        @Override
        public AddCharacterCompositionDefinitionCorrelationPickerController createFromParcel(Parcel source) {
            final ImmutableCorrelation<AlphabetId> texts = CorrelationParceler.read(source).toImmutable();
            return new AddCharacterCompositionDefinitionCorrelationPickerController(texts);
        }

        @Override
        public AddCharacterCompositionDefinitionCorrelationPickerController[] newArray(int size) {
            return new AddCharacterCompositionDefinitionCorrelationPickerController[size];
        }
    };
}
