package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableMap;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LangbookPreferences;
import sword.langbook3.android.LanguagePickerActivity;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.LanguageId;

public final class DefineCorrelationArrayLanguagePickerController implements LanguagePickerActivity.Controller {

    public void fire(@NonNull Activity activity, int requestCode) {
        // TODO: This can be optimized, as we are only interested in checking if there is just one language or not.
        final AlphabetId preferredAlphabet = LangbookPreferences.getInstance().getPreferredAlphabet();
        final ImmutableMap<LanguageId, String> languages = DbManager.getInstance().getManager().readAllLanguages(preferredAlphabet);

        if (languages.size() == 1) {
            complete(activity, requestCode, languages.keyAt(0));
        }
        else {
            LanguagePickerActivity.open(activity, requestCode, this);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Nothing to be done
    }

    private void complete(@NonNull Activity activity, int requestCode, @NonNull LanguageId language) {
        final WordEditorActivity.Controller controller = new DefineCorrelationArrayWordEditorController(language);
        WordEditorActivity.open(activity, requestCode, controller);
    }

    @Override
    public void complete(@NonNull Activity activity, @NonNull LanguageId language) {
        complete(activity, LanguagePickerActivity.REQUEST_CODE_NEW_WORD, language);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == LanguagePickerActivity.REQUEST_CODE_NEW_WORD && resultCode == Activity.RESULT_OK) {
            activity.setResult(Activity.RESULT_OK, data);
            activity.finish();
        }
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
