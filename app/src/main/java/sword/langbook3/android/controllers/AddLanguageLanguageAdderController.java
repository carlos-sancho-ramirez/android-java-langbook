package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.LanguageAdderActivity;
import sword.langbook3.android.WordEditorActivity;
import sword.langbook3.android.collections.TransformableUtils;
import sword.langbook3.android.collections.TraversableUtils;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdManager;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdManager;

import static android.app.Activity.RESULT_OK;

public final class AddLanguageLanguageAdderController implements LanguageAdderActivity.Controller {

    @Override
    public void complete(@NonNull Activity activity, String languageCode, int alphabetCount) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final ImmutableSet<ConceptId> concepts = checker.getNextAvailableConceptIds(alphabetCount + 1);
        final LanguageId languageId = LanguageIdManager.conceptAsLanguageId(TraversableUtils.first(concepts));
        final ImmutableList<AlphabetId> alphabets = TransformableUtils.skip(concepts, 1).map(AlphabetIdManager::conceptAsAlphabetId);
        final WordEditorActivity.Controller controller = new AddLanguageWordEditorControllerForLanguage(languageCode, languageId, alphabets);
        WordEditorActivity.open(activity, LanguageAdderActivity.REQUEST_CODE_NEXT_STEP, controller);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == LanguageAdderActivity.REQUEST_CODE_NEXT_STEP && resultCode == RESULT_OK) {
            activity.setResult(RESULT_OK);
            activity.finish();
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
