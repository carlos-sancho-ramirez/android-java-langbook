package sword.langbook3.android.controllers;

import android.os.Parcel;

import androidx.annotation.NonNull;
import sword.collections.ImmutableSet;
import sword.langbook3.android.AcceptationDefinition;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.models.SentenceSpan;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class AddSynonymSentenceSpanEditorController extends AbstractSpanEditorController {

    @NonNull
    private final ConceptId _concept;

    public AddSynonymSentenceSpanEditorController(@NonNull String text, @NonNull ConceptId concept) {
        super(text);
        ensureNonNull(concept);
        _concept = concept;
    }

    @Override
    public void setup(@NonNull MutableState state) {
        // Nothing to be done
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull State state) {
        final ImmutableSet<SentenceSpan<Object>> rawSpans = state.getSpans().filter(v -> v != 0).keySet().toImmutable();

        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ImmutableSet<SentenceSpan<AcceptationId>> spans = rawSpans.map(span ->
                (span.acceptation instanceof AcceptationId)? (SentenceSpan<AcceptationId>) ((SentenceSpan) span) :
                        new SentenceSpan<>(span.range, storeAcceptationDefinition((AcceptationDefinition) span.acceptation))).toSet();

        final SentenceId newSentenceId = manager.addSentence(_concept, _text, spans);
        presenter.displayFeedback(R.string.includeSentenceFeedback);
        presenter.finish(newSentenceId);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_text);
        ConceptIdParceler.write(dest, _concept);
    }

    public static final Creator<AddSynonymSentenceSpanEditorController> CREATOR = new Creator<AddSynonymSentenceSpanEditorController>() {

        @Override
        public AddSynonymSentenceSpanEditorController createFromParcel(Parcel source) {
            final String text = source.readString();
            final ConceptId concept = ConceptIdParceler.read(source);
            return new AddSynonymSentenceSpanEditorController(text, concept);
        }

        @Override
        public AddSynonymSentenceSpanEditorController[] newArray(int size) {
            return new AddSynonymSentenceSpanEditorController[size];
        }
    };
}
