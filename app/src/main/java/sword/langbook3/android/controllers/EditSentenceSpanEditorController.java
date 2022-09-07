package sword.langbook3.android.controllers;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntList;
import sword.collections.Traverser;
import sword.langbook3.android.AcceptationDefinition;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.db.SentenceIdParceler;
import sword.langbook3.android.models.SentenceSpan;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;

public final class EditSentenceSpanEditorController extends AbstractSpanEditorController {

    @NonNull
    private final SentenceId _sentence;

    public EditSentenceSpanEditorController(@NonNull String text, @NonNull SentenceId sentence) {
        super(text);
        ensureNonNull(sentence);
        _sentence = sentence;
    }

    @Override
    public void setup(@NonNull MutableState state) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final ImmutableSet<SentenceSpan<AcceptationId>> spans = checker.getSentenceSpans(_sentence);
        for (SentenceSpan<AcceptationId> span : spans) {
            final ImmutableList<CorrelationId> correlationIds = checker.getAcceptationCorrelationArray(span.acceptation);
            final Traverser<CorrelationId> traverser = correlationIds.iterator();

            final ImmutableCorrelation<AlphabetId> firstCorrelation = checker.getCorrelationWithText(traverser.next());
            final MutableHashSet<ImmutableIntRange> matchingRanges = MutableHashSet.empty();
            for (String correlationText : firstCorrelation.toSet()) {
                final MutableIntList matchingIndexes = MutableIntList.empty();
                int index = 0;
                while (index >= 0) {
                    index = _text.indexOf(correlationText, index);
                    if (index >= 0) {
                        matchingIndexes.append(index);
                        index++;
                    }
                }
                matchingRanges.addAll(matchingIndexes.map(start -> new ImmutableIntRange(start, start + correlationText.length() - 1)));
            }

            while (traverser.hasNext() && !matchingRanges.isEmpty()) {
                final ImmutableSet<String> correlationTexts = checker.getCorrelationWithText(traverser.next()).toSet();
                for (ImmutableIntRange range : matchingRanges.donate()) {
                    for (String correlationText : correlationTexts) {
                        if (_text.substring(range.max() + 1).startsWith(correlationText)) {
                            matchingRanges.add(new ImmutableIntRange(range.min(), range.max() + correlationText.length()));
                        }
                    }
                }
            }

            if (matchingRanges.size() == 1) {
                final ImmutableIntRange range = matchingRanges.first();
                final SentenceSpan<Object> newSpan = range.equals(span.range)? ((SentenceSpan<Object>) ((SentenceSpan) span)) :
                        new SentenceSpan<>(range, span.acceptation);
                state.putSpan(newSpan);
            }
        }
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull State state) {
        final ImmutableSet<SentenceSpan<Object>> rawSpans = state.getSpans().filter(v -> v != 0).keySet().toImmutable();

        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ImmutableSet<SentenceSpan<AcceptationId>> spans = rawSpans.map(span ->
                (span.acceptation instanceof AcceptationId)? (SentenceSpan<AcceptationId>) ((SentenceSpan) span) :
                        new SentenceSpan<>(span.range, storeAcceptationDefinition((AcceptationDefinition) span.acceptation))).toSet();

        if (!manager.updateSentenceTextAndSpans(_sentence, _text, spans)) {
            throw new AssertionError();
        }

        presenter.displayFeedback(R.string.updateSentenceFeedback);
        presenter.finish();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_text);
        SentenceIdParceler.write(dest, _sentence);
    }

    public static final Parcelable.Creator<EditSentenceSpanEditorController> CREATOR = new Parcelable.Creator<EditSentenceSpanEditorController>() {

        @Override
        public EditSentenceSpanEditorController createFromParcel(Parcel source) {
            final String text = source.readString();
            final SentenceId sentence = SentenceIdParceler.read(source);
            return new EditSentenceSpanEditorController(text, sentence);
        }

        @Override
        public EditSentenceSpanEditorController[] newArray(int size) {
            return new EditSentenceSpanEditorController[size];
        }
    };
}
