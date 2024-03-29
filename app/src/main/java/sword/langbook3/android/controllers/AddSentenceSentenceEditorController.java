package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.Procedure;
import sword.langbook3.android.SentenceEditorActivity;
import sword.langbook3.android.SpanEditorActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddSentenceSentenceEditorController implements SentenceEditorActivity.Controller {

    @NonNull
    private final AcceptationId _acceptation;

    public AddSentenceSentenceEditorController(@NonNull AcceptationId acceptation) {
        ensureNonNull(acceptation);
        _acceptation = acceptation;
    }

    @Override
    public void load(@NonNull Procedure<String> procedure) {
        // Nothing to be done
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull String text) {
        final SpanEditorActivity.Controller controller = new AddSentenceSpanEditorController(text, _acceptation);
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
    }

    public static final Creator<AddSentenceSentenceEditorController> CREATOR = new Creator<AddSentenceSentenceEditorController>() {

        @Override
        public AddSentenceSentenceEditorController createFromParcel(Parcel source) {
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            return new AddSentenceSentenceEditorController(acceptation);
        }

        @Override
        public AddSentenceSentenceEditorController[] newArray(int size) {
            return new AddSentenceSentenceEditorController[size];
        }
    };
}
