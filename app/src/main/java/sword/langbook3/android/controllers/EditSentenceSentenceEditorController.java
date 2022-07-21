package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.Procedure;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.SentenceEditorActivity;
import sword.langbook3.android.SpanEditorActivity;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.db.SentenceIdParceler;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class EditSentenceSentenceEditorController implements SentenceEditorActivity.Controller {

    @NonNull
    private final SentenceId _sentence;

    public EditSentenceSentenceEditorController(@NonNull SentenceId sentence) {
        ensureNonNull(sentence);
        _sentence = sentence;
    }

    @Override
    public void load(@NonNull Procedure<String> procedure) {
        procedure.apply(DbManager.getInstance().getManager().getSentenceText(_sentence));
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull String text) {
        final SpanEditorActivity.Controller controller = new EditSentenceSpanEditorController(text, _sentence);
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
        SentenceIdParceler.write(dest, _sentence);
    }

    public static final Parcelable.Creator<EditSentenceSentenceEditorController> CREATOR = new Parcelable.Creator<EditSentenceSentenceEditorController>() {

        @Override
        public EditSentenceSentenceEditorController createFromParcel(Parcel source) {
            final SentenceId sentence = SentenceIdParceler.read(source);
            return new EditSentenceSentenceEditorController(sentence);
        }

        @Override
        public EditSentenceSentenceEditorController[] newArray(int size) {
            return new EditSentenceSentenceEditorController[size];
        }
    };
}
