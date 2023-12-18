package sword.langbook3.android.controllers;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.activities.WordEditorActivity;
import sword.langbook3.android.activities.delegates.LanguageAdderActivityDelegate;
import sword.langbook3.android.activities.delegates.WordEditorActivityDelegate;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdManager;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdManager;
import sword.langbook3.android.interf.ActivityExtensions;
import sword.langbook3.android.interf.ActivityInterface;

public final class AddLanguageLanguageAdderController implements LanguageAdderActivityDelegate.Controller {

    @Override
    public void complete(@NonNull ActivityExtensions activity, String languageCode, int alphabetCount) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final ImmutableSet<ConceptId> concepts = checker.getNextAvailableConceptIds(alphabetCount + 1);
        final LanguageId languageId = LanguageIdManager.conceptAsLanguageId(concepts.first());
        final ImmutableList<AlphabetId> alphabets = concepts.skip(1).map(AlphabetIdManager::conceptAsAlphabetId);
        final WordEditorActivityDelegate.Controller controller = new AddLanguageWordEditorControllerForLanguage(languageCode, languageId, alphabets);
        WordEditorActivity.open(activity, LanguageAdderActivityDelegate.REQUEST_CODE_NEXT_STEP, controller);
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == LanguageAdderActivityDelegate.REQUEST_CODE_NEXT_STEP && resultCode == RESULT_OK) {
            activity.setResult(RESULT_OK);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        // Nothing to be done
    }

    public static final Parcelable.Creator<AddLanguageLanguageAdderController> CREATOR = new Parcelable.Creator<AddLanguageLanguageAdderController>() {

        @Override
        public AddLanguageLanguageAdderController createFromParcel(Parcel source) {
            return new AddLanguageLanguageAdderController();
        }

        @Override
        public AddLanguageLanguageAdderController[] newArray(int size) {
            return new AddLanguageLanguageAdderController[size];
        }
    };
}
