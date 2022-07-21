package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.Map;
import sword.langbook3.android.AcceptationDefinition;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.models.SentenceSpan;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddSentenceSpanEditorController extends AbstractSpanEditorController {

    @NonNull
    private final AcceptationId _acceptation;

    public AddSentenceSpanEditorController(@NonNull String text, @NonNull AcceptationId acceptation) {
        super(text);
        ensureNonNull(acceptation);
        _acceptation = acceptation;
    }

    @Override
    public void setup(@NonNull MutableState state) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final ImmutableMap<String, AcceptationId> map = checker.readTextAndDynamicAcceptationsMapFromAcceptation(_acceptation);
        for (Map.Entry<String, AcceptationId> entry : map.entries()) {
            final int index = _text.indexOf(entry.key());
            if (index >= 0) {
                final ImmutableIntRange range = new ImmutableIntRange(index, index + entry.key().length() - 1);
                state.putSpan(new SentenceSpan<>(range, entry.value()));
                return;
            }
        }
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull State state) {
        final ImmutableSet<SentenceSpan<Object>> rawSpans = state.getSpans().filter(v -> v != 0).keySet().toImmutable();

        if (rawSpans.isEmpty()) {
            presenter.displayFeedback(R.string.spanEditorNoSpanPresentError);
        }
        else {
            final LangbookDbManager manager = DbManager.getInstance().getManager();
            final ImmutableSet<SentenceSpan<AcceptationId>> spans = rawSpans.map(span ->
                    (span.acceptation instanceof AcceptationId)? (SentenceSpan<AcceptationId>) ((SentenceSpan) span) :
                            new SentenceSpan<>(span.range, storeAcceptationDefinition((AcceptationDefinition) span.acceptation))).toSet();

            final ConceptId concept = manager.getNextAvailableConceptId();

            final SentenceId newSentenceId = manager.addSentence(concept, _text, spans);
            presenter.displayFeedback(R.string.includeSentenceFeedback);
            presenter.finish(newSentenceId);
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_text);
        AcceptationIdParceler.write(dest, _acceptation);
    }

    public static final Creator<AddSentenceSpanEditorController> CREATOR = new Creator<AddSentenceSpanEditorController>() {

        @Override
        public AddSentenceSpanEditorController createFromParcel(Parcel source) {
            final String text = source.readString();
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            return new AddSentenceSpanEditorController(text, acceptation);
        }

        @Override
        public AddSentenceSpanEditorController[] newArray(int size) {
            return new AddSentenceSpanEditorController[size];
        }
    };
}
