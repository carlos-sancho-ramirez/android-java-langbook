package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.Procedure;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.SentenceEditorActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.db.SentenceIdParceler;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class SentenceEditorController implements SentenceEditorActivity.Controller {

    private final AcceptationId _acceptation;
    private final ConceptId _concept;
    private final SentenceId _sentence;

    public SentenceEditorController(AcceptationId acceptation, ConceptId concept, SentenceId sentence) {
        ensureValidArguments(acceptation != null && concept == null && sentence == null ||
                acceptation == null && concept != null && sentence == null ||
                acceptation == null && concept == null && sentence != null);
        _acceptation = acceptation;
        _concept = concept;
        _sentence = sentence;
    }

    @Override
    public void load(@NonNull Procedure<String> procedure) {
        if (_sentence != null) {
            procedure.apply(DbManager.getInstance().getManager().getSentenceText(_sentence));
        }
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull String text) {
        final SpanEditorController controller = (_acceptation != null)? new SpanEditorController(text, _acceptation, null, null) :
                (_sentence == null)? new SpanEditorController(text, null, _concept, null) :
                        new SpanEditorController(text, null, null, _sentence);
        presenter.openSpanEditor(SentenceEditorActivity.REQUEST_CODE_ADD_SPAN, controller);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data) {
        if (requestCode == SentenceEditorActivity.REQUEST_CODE_ADD_SPAN && resultCode == Activity.RESULT_OK) {
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
        AcceptationIdParceler.write(dest, _acceptation);
        ConceptIdParceler.write(dest, _concept);
        SentenceIdParceler.write(dest, _sentence);
    }

    public static final Parcelable.Creator<SentenceEditorController> CREATOR = new Parcelable.Creator<SentenceEditorController>() {

        @Override
        public SentenceEditorController createFromParcel(Parcel source) {
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            final ConceptId concept = ConceptIdParceler.read(source);
            final SentenceId sentence = SentenceIdParceler.read(source);
            return new SentenceEditorController(acceptation, concept, sentence);
        }

        @Override
        public SentenceEditorController[] newArray(int size) {
            return new SentenceEditorController[size];
        }
    };
}
