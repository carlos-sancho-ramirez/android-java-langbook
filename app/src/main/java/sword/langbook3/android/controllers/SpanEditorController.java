package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.Map;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntList;
import sword.collections.Traverser;
import sword.langbook3.android.AcceptationDefinition;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.R;
import sword.langbook3.android.SpanEditorActivity;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AcceptationIdParceler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.ConceptIdParceler;
import sword.langbook3.android.db.CorrelationId;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookDbChecker;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.ParcelableBunchIdSet;
import sword.langbook3.android.db.ParcelableCorrelationArray;
import sword.langbook3.android.db.SentenceId;
import sword.langbook3.android.db.SentenceIdParceler;
import sword.langbook3.android.models.SentenceSpan;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.util.PreconditionUtils.ensureNonNull;
import static sword.langbook3.android.util.PreconditionUtils.ensureValidArguments;

public final class SpanEditorController implements SpanEditorActivity.Controller {

    @NonNull
    private final String _text;
    private final AcceptationId _acceptation;
    private final ConceptId _concept;
    private final SentenceId _sentence;

    public SpanEditorController(@NonNull String text, AcceptationId acceptation, ConceptId concept, SentenceId sentence) {
        ensureNonNull(text);
        ensureValidArguments(acceptation != null && concept == null && sentence == null ||
                acceptation == null && concept != null && sentence == null ||
                acceptation == null && concept == null && sentence != null);
        _text = text;
        _acceptation = acceptation;
        _concept = concept;
        _sentence = sentence;
    }

    // We should prevent having sentences without neither spans nor other sentence sharing the same meaning,
    // as it will be not possible to reference them within the app.
    private boolean shouldAllowNoSpans() {
        return _sentence != null || _concept != null;
    }

    private void insertInitialSpans(@NonNull MutableState state, @NonNull SentenceId sentenceId) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final ImmutableSet<SentenceSpan<AcceptationId>> spans = checker.getSentenceSpans(sentenceId);
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
                final ImmutableIntRange range = matchingRanges.valueAt(0);
                final SentenceSpan<Object> newSpan = range.equals(span.range)? ((SentenceSpan<Object>) ((SentenceSpan) span)) :
                        new SentenceSpan<>(range, span.acceptation);
                state.putSpan(newSpan);
            }
        }
    }

    private void insertSuggestedSpans(@NonNull MutableState state, @NonNull AcceptationId acceptation) {
        final LangbookDbChecker checker = DbManager.getInstance().getManager();
        final ImmutableMap<String, AcceptationId> map = checker.readTextAndDynamicAcceptationsMapFromAcceptation(acceptation);
        for (Map.Entry<String, AcceptationId> entry : map.entries()) {
            final int index = _text.indexOf(entry.key());
            if (index >= 0) {
                final ImmutableIntRange range = new ImmutableIntRange(index, index + entry.key().length() - 1);
                state.putSpan(new SentenceSpan<>(range, entry.value()));
                return;
            }
        }
    }

    @NonNull
    @Override
    public String getText() {
        return _text;
    }

    @Override
    public void setup(@NonNull MutableState state) {
        if (_sentence != null) {
            insertInitialSpans(state, _sentence);
        }
        else if (_acceptation != null) {
            insertSuggestedSpans(state, _acceptation);
        }
    }

    private void setPickedAcceptation(@NonNull MutableState state, @NonNull Object item) {
        state.putSpan(new SentenceSpan<>(state.getSelection(), item));
    }

    @Override
    public void pickAcceptation(@NonNull Presenter presenter, @NonNull MutableState state, @NonNull String query) {
        final Object immediateResult = presenter.fireFixedTextAcceptationPicker(SpanEditorActivity.REQUEST_CODE_PICK_ACCEPTATION, query);
        if (immediateResult != null) {
            setPickedAcceptation(state, immediateResult);
        }
    }

    private AcceptationId storeAcceptationDefinition(@NonNull AcceptationDefinition definition) {
        final LangbookDbManager manager = DbManager.getInstance().getManager();
        final ConceptId concept = manager.getNextAvailableConceptId();
        final AcceptationId acceptation = manager.addAcceptation(concept, definition.correlationArray);
        for (BunchId bunch : definition.bunchSet) {
            manager.addAcceptationInBunch(bunch, acceptation);
        }
        return acceptation;
    }

    @Override
    public void complete(@NonNull Presenter presenter, @NonNull State state) {
        final ImmutableSet<SentenceSpan<Object>> rawSpans = state.getSpans().filter(v -> v != 0).keySet().toImmutable();

        if (rawSpans.isEmpty() && !shouldAllowNoSpans()) {
            presenter.displayFeedback(R.string.spanEditorNoSpanPresentError);
        }
        else {
            final LangbookDbManager manager = DbManager.getInstance().getManager();
            final ImmutableSet<SentenceSpan<AcceptationId>> spans = rawSpans.map(span ->
                    (span.acceptation instanceof AcceptationId)? (SentenceSpan<AcceptationId>) ((SentenceSpan) span) :
                            new SentenceSpan<>(span.range, storeAcceptationDefinition((AcceptationDefinition) span.acceptation))).toSet();

            if (_sentence == null) {
                ConceptId concept = _concept;
                if (concept == null) {
                    concept = manager.getNextAvailableConceptId();
                }

                final SentenceId newSentenceId = manager.addSentence(concept, _text, spans);
                presenter.displayFeedback(R.string.includeSentenceFeedback);
                presenter.finish(newSentenceId);
            }
            else {
                if (!manager.updateSentenceTextAndSpans(_sentence, _text, spans)) {
                    throw new AssertionError();
                }

                presenter.displayFeedback(R.string.updateSentenceFeedback);
                presenter.finish();
            }
        }
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data, @NonNull MutableState state) {
        if (requestCode == SpanEditorActivity.REQUEST_CODE_PICK_ACCEPTATION && resultCode == Activity.RESULT_OK && data != null) {
            final AcceptationId acceptation = AcceptationIdBundler.readAsIntentExtra(data, BundleKeys.ACCEPTATION);
            final Object item;
            if (acceptation != null) {
                item = acceptation;
            }
            else {
                final ParcelableCorrelationArray parcelableCorrelationArray = data.getParcelableExtra(BundleKeys.CORRELATION_ARRAY);
                final ParcelableBunchIdSet bunchIdSet = data.getParcelableExtra(BundleKeys.BUNCH_SET);
                item = new AcceptationDefinition(parcelableCorrelationArray.get(), bunchIdSet.get());
            }

            setPickedAcceptation(state, item);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_text);
        AcceptationIdParceler.write(dest, _acceptation);
        ConceptIdParceler.write(dest, _concept);
        SentenceIdParceler.write(dest, _sentence);
    }

    public static final Parcelable.Creator<SpanEditorController> CREATOR = new Parcelable.Creator<SpanEditorController>() {

        @Override
        public SpanEditorController createFromParcel(Parcel source) {
            final String text = source.readString();
            final AcceptationId acceptation = AcceptationIdParceler.read(source);
            final ConceptId concept = ConceptIdParceler.read(source);
            final SentenceId sentence = SentenceIdParceler.read(source);
            return new SpanEditorController(text, acceptation, concept, sentence);
        }

        @Override
        public SpanEditorController[] newArray(int size) {
            return new SpanEditorController[size];
        }
    };
}
