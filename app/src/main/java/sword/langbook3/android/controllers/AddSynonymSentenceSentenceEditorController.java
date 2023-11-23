package sword.langbook3.android.controllers;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;

import sword.collections.Procedure;
import sword.langbook3.android.activities.delegates.SentenceEditorActivityDelegate;
import sword.langbook3.android.activities.delegates.SpanEditorActivityDelegate;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.interf.ActivityInterface;
import sword.langbook3.android.presenters.Presenter;

public final class AddSynonymSentenceSentenceEditorController implements SentenceEditorActivityDelegate.Controller {

    @NonNull
    private final ConceptId _concept;

    public AddSynonymSentenceSentenceEditorController(@NonNull ConceptId concept) {
        ensureNonNull(concept);
        _concept = concept;
    }

    @Override
    public void load(@NonNull Procedure<String> procedure) {
        // Nothing to be done
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull String text) {
        final SpanEditorActivityDelegate.Controller controller = new AddSynonymSentenceSpanEditorController(text, _concept);
        presenter.openSpanEditor(SentenceEditorActivityDelegate.REQUEST_CODE_ADD_SPAN, controller);
    }

    @Override
    public void onActivityResult(@NonNull ActivityInterface activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == SentenceEditorActivityDelegate.REQUEST_CODE_ADD_SPAN && resultCode == Activity.RESULT_OK) {
            activity.setResult(Activity.RESULT_OK, data);
            activity.finish();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        ConceptIdParceler.write(dest, _concept);
    }

    public static final Creator<AddSynonymSentenceSentenceEditorController> CREATOR = new Creator<AddSynonymSentenceSentenceEditorController>() {

        @Override
        public AddSynonymSentenceSentenceEditorController createFromParcel(Parcel source) {
            final ConceptId concept = ConceptIdParceler.read(source);
            return new AddSynonymSentenceSentenceEditorController(concept);
        }

        @Override
        public AddSynonymSentenceSentenceEditorController[] newArray(int size) {
            return new AddSynonymSentenceSentenceEditorController[size];
        }
    };
}
