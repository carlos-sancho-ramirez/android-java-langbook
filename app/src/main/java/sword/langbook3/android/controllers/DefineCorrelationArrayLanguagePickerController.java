package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.presenters.Presenter;

public final class DefineCorrelationArrayLanguagePickerController extends AbstractLanguagePickerController {

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Nothing to be done
    }

    @Override
    void complete(@NonNull Presenter presenter, int requestCode, @NonNull LanguageId language) {
        final WordEditorActivityDelegate.Controller controller = new DefineCorrelationArrayWordEditorController(language);
        presenter.openWordEditor(requestCode, controller);
    }

    public static final Creator<DefineCorrelationArrayLanguagePickerController> CREATOR = new Creator<DefineCorrelationArrayLanguagePickerController>() {

        @Override
        public DefineCorrelationArrayLanguagePickerController createFromParcel(Parcel source) {
            return new DefineCorrelationArrayLanguagePickerController();
        }

        @Override
        public DefineCorrelationArrayLanguagePickerController[] newArray(int size) {
            return new DefineCorrelationArrayLanguagePickerController[size];
        }
    };
}
