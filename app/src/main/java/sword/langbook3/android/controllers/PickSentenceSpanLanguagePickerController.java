package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class PickSentenceSpanLanguagePickerController extends AbstractLanguagePickerController {

    @NonNull
    private final String _text;

    public PickSentenceSpanLanguagePickerController(@NonNull String text) {
        ensureNonNull(text);
        _text = text;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_text);
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull LanguageId language) {
        new PickSentenceSpanWordEditorController(language, _text)
                .fire(presenter, requestCode);
    }

    public static final Creator<PickSentenceSpanLanguagePickerController> CREATOR = new Creator<PickSentenceSpanLanguagePickerController>() {

        @Override
        public PickSentenceSpanLanguagePickerController createFromParcel(Parcel source) {
            final String text = source.readString();
            return new PickSentenceSpanLanguagePickerController(text);
        }

        @Override
        public PickSentenceSpanLanguagePickerController[] newArray(int size) {
            return new PickSentenceSpanLanguagePickerController[size];
        }
    };
}
