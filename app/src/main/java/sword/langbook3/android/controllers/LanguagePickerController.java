package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.langbook3.android.LanguagePickerActivity;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.db.LanguageId;

public final class LanguagePickerController implements LanguagePickerActivity.Controller {

    private final ConceptId _concept;
    private final String _searchQuery;

    public LanguagePickerController(ConceptId concept, String searchQuery) {
        _concept = concept;
        _searchQuery = searchQuery;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ConceptIdParceler.write(dest, _concept);
        dest.writeString(_searchQuery);
    }

    @Override
    public void complete(@NonNull Activity activity, @NonNull LanguageId language) {
        final WordEditorActivity.Controller controller;
        if (_searchQuery != null) {
            controller = new WordEditorController(null, _concept, null, null, language, _searchQuery, true);
        }
        else {
            controller = new WordEditorController(null, null, null, null, language, null, false);
        }
        WordEditorActivity.open(activity, LanguagePickerActivity.REQUEST_CODE_NEW_WORD, controller);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == LanguagePickerActivity.REQUEST_CODE_NEW_WORD && resultCode == Activity.RESULT_OK) {
            activity.setResult(Activity.RESULT_OK, data);
            activity.finish();
        }
    }

    public static final Parcelable.Creator<LanguagePickerController> CREATOR = new Parcelable.Creator<LanguagePickerController>() {

        @Override
        public LanguagePickerController createFromParcel(Parcel source) {
            final ConceptId concept = ConceptIdParceler.read(source);
            final String searchQuery = source.readString();
            return new LanguagePickerController(concept, searchQuery);
        }

        @Override
        public LanguagePickerController[] newArray(int size) {
            return new LanguagePickerController[size];
        }
    };
}
