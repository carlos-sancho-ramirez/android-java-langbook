package sword.langbook3.android.controllers;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import sword.collections.ImmutableList;
import sword.collections.ImmutableSet;
import sword.collections.MutableHashSet;
import sword.collections.MutableSet;
import sword.langbook3.android.AcceptationDefinition;
import sword.langbook3.android.AgentEditorActivity;
import sword.langbook3.android.BundleKeys;
import sword.langbook3.android.DbManager;
import sword.langbook3.android.IntermediateIntentions;
import sword.langbook3.android.R;
import sword.langbook3.android.db.AcceptationId;
import sword.langbook3.android.db.AcceptationIdBundler;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.BunchId;
import sword.langbook3.android.db.BunchIdManager;
import sword.langbook3.android.db.ConceptId;
import sword.langbook3.android.db.Correlation;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.LangbookDbManager;
import sword.langbook3.android.db.ParcelableBunchIdSet;
import sword.langbook3.android.db.ParcelableCorrelationArray;
import sword.langbook3.android.db.RuleId;
import sword.langbook3.android.db.RuleIdManager;
import sword.langbook3.android.presenters.Presenter;

import static sword.langbook3.android.db.BunchIdManager.conceptAsBunchId;
import static sword.langbook3.android.db.RuleIdManager.conceptAsRuleId;

abstract class AbstractAgentEditorController implements AgentEditorActivity.Controller {

    boolean checkState(@NonNull Presenter presenter, @NonNull State state) {
        final ImmutableSet<Object> targets = state.getTargetBunches();
        final ImmutableSet<Object> sources = state.getSourceBunches();
        final ImmutableSet<Object> diffs = state.getDiffBunches();

        final ImmutableSet<BunchId> registeredTargets = targets.filter(obj -> obj instanceof BunchId).map(obj -> (BunchId) obj).toSet();
        final ImmutableSet<BunchId> registeredSources = sources.filter(obj -> obj instanceof BunchId).map(obj -> (BunchId) obj).toSet();
        final ImmutableSet<BunchId> registeredDiffs = diffs.filter(obj -> obj instanceof BunchId).map(obj -> (BunchId) obj).toSet();

        if (registeredDiffs.anyMatch(registeredSources::contains)) {
            presenter.displayFeedback(R.string.sourceAndDiffBunchError);
            return false;
        }

        if (registeredTargets.anyMatch(registeredSources::contains)) {
            presenter.displayFeedback(R.string.targetBunchUsedAsSourceBunchError);
            return false;
        }

        if (registeredTargets.anyMatch(registeredDiffs::contains)) {
            presenter.displayFeedback(R.string.targetBunchUsedAsDiffBunchError);
            return false;
        }

        final MutableSet<AlphabetId> alphabets = MutableHashSet.empty();
        final ImmutableCorrelation.Builder<AlphabetId> startMatcherBuilder = new ImmutableCorrelation.Builder<>();
        for (Correlation.Entry<AlphabetId> entry : state.getStartMatcher()) {
            if (alphabets.contains(entry.alphabet)) {
                presenter.displayFeedback(R.string.duplicateInStartMatcherError);
                return false;
            }
            alphabets.add(entry.alphabet);
            startMatcherBuilder.put(entry.alphabet, entry.text);

            if (TextUtils.isEmpty(entry.text)) {
                presenter.displayFeedback(R.string.emptyMatcherEntryError);
                return false;
            }
        }
        final ImmutableCorrelation<AlphabetId> startMatcher = startMatcherBuilder.build();

        alphabets.clear();
        final ImmutableCorrelation.Builder<AlphabetId> endMatcherBuilder = new ImmutableCorrelation.Builder<>();
        for (Correlation.Entry<AlphabetId> entry : state.getEndMatcher()) {
            if (alphabets.contains(entry.alphabet)) {
                presenter.displayFeedback(R.string.duplicateInEndMatcherError);
                return false;
            }
            alphabets.add(entry.alphabet);
            endMatcherBuilder.put(entry.alphabet, entry.text);

            if (TextUtils.isEmpty(entry.text)) {
                presenter.displayFeedback(R.string.emptyMatcherEntryError);
                return false;
            }
        }
        final ImmutableCorrelation<AlphabetId> endMatcher = endMatcherBuilder.build();

        if (sources.isEmpty() && state.getStartMatcher().isEmpty() && state.getEndMatcher().isEmpty()) {
            // This would select all acceptations from the database, which has no sense
            presenter.displayFeedback(R.string.sourcesAndMatchersEmptyError);
            return false;
        }

        final boolean ruleRequired = !startMatcher.equals(state.getStartAdder().concatenateTexts()) || !endMatcher.equals(state.getEndAdder().concatenateTexts());
        if (ruleRequired && state.getRule() == null) {
            presenter.displayFeedback(R.string.requiredRuleError);
            return false;
        }

        return true;
    }

    static ImmutableCorrelation<AlphabetId> buildCorrelation(@NonNull ImmutableList<Correlation.Entry<AlphabetId>> entries) {
        final ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
        for (Correlation.Entry<AlphabetId> corrEntry : entries) {
            builder.put(corrEntry.alphabet, corrEntry.text);
        }
        return builder.build();
    }

    BunchId ensureBunchIsStored(@NonNull Object object) {
        if (object instanceof BunchId) {
            return (BunchId) object;
        }
        else {
            final LangbookDbManager manager = DbManager.getInstance().getManager();
            final AcceptationDefinition definition = (AcceptationDefinition) object;
            final ConceptId concept = manager.getNextAvailableConceptId();
            final AcceptationId acceptation = manager.addAcceptation(concept, definition.correlationArray);
            for (BunchId bunch : definition.bunchSet) {
                manager.addAcceptationInBunch(bunch, acceptation);
            }
            return BunchIdManager.conceptAsBunchId(concept);
        }
    }

    RuleId ensureRuleIsStored(@NonNull Object object) {
        if (object instanceof RuleId) {
            return (RuleId) object;
        }
        else {
            final LangbookDbManager manager = DbManager.getInstance().getManager();
            final AcceptationDefinition definition = (AcceptationDefinition) object;
            final ConceptId concept = manager.getNextAvailableConceptId();
            final AcceptationId acceptation = manager.addAcceptation(concept, definition.correlationArray);
            for (BunchId bunch : definition.bunchSet) {
                manager.addAcceptationInBunch(bunch, acceptation);
            }
            return RuleIdManager.conceptAsRuleId(concept);
        }
    }

    @Override
    public void pickTargetBunch(@NonNull Presenter presenter) {
        IntermediateIntentions.pickBunch(presenter, AgentEditorActivity.REQUEST_CODE_PICK_TARGET_BUNCH);
    }

    @Override
    public void pickSourceBunch(@NonNull Presenter presenter) {
        IntermediateIntentions.pickBunch(presenter, AgentEditorActivity.REQUEST_CODE_PICK_SOURCE_BUNCH);
    }

    @Override
    public void pickDiffBunch(@NonNull Presenter presenter) {
        IntermediateIntentions.pickBunch(presenter, AgentEditorActivity.REQUEST_CODE_PICK_DIFF_BUNCH);
    }

    @Override
    public void defineStartAdder(@NonNull Presenter presenter) {
        IntermediateIntentions.defineCorrelationArray(presenter, AgentEditorActivity.REQUEST_CODE_DEFINE_START_ADDER);
    }

    @Override
    public void defineEndAdder(@NonNull Presenter presenter) {
        IntermediateIntentions.defineCorrelationArray(presenter, AgentEditorActivity.REQUEST_CODE_DEFINE_END_ADDER);
    }

    @Override
    public void pickRule(@NonNull Presenter presenter) {
        IntermediateIntentions.pickRule(presenter, AgentEditorActivity.REQUEST_CODE_PICK_RULE);
    }

    @Override
    public void onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, Intent data, @NonNull MutableState state) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == AgentEditorActivity.REQUEST_CODE_DEFINE_START_ADDER) {
                final ParcelableCorrelationArray parcelableCorrelationArray = data.getParcelableExtra(BundleKeys.CORRELATION_ARRAY);
                state.setStartAdder(parcelableCorrelationArray.get());
            }
            else if (requestCode == AgentEditorActivity.REQUEST_CODE_DEFINE_END_ADDER) {
                final ParcelableCorrelationArray parcelableCorrelationArray = data.getParcelableExtra(BundleKeys.CORRELATION_ARRAY);
                state.setEndAdder(parcelableCorrelationArray.get());
            }
            else if (requestCode == AgentEditorActivity.REQUEST_CODE_PICK_TARGET_BUNCH || requestCode == AgentEditorActivity.REQUEST_CODE_PICK_SOURCE_BUNCH || requestCode == AgentEditorActivity.REQUEST_CODE_PICK_DIFF_BUNCH) {
                final LangbookDbManager manager = DbManager.getInstance().getManager();
                final AcceptationId acceptation = AcceptationIdBundler.readAsIntentExtra(data, BundleKeys.ACCEPTATION);
                final Object item;
                if (acceptation != null) {
                    item = conceptAsBunchId(manager.conceptFromAcceptation(acceptation));
                }
                else {
                    final ParcelableCorrelationArray parcelableCorrelationArray = data.getParcelableExtra(BundleKeys.CORRELATION_ARRAY);
                    final ParcelableBunchIdSet bunchIdSet = data.getParcelableExtra(BundleKeys.BUNCH_SET);
                    item = new AcceptationDefinition(parcelableCorrelationArray.get(), bunchIdSet.get());
                }

                if (requestCode == AgentEditorActivity.REQUEST_CODE_PICK_TARGET_BUNCH) {
                    state.setTargetBunches(state.getTargetBunches().add(item));
                }
                else if (requestCode == AgentEditorActivity.REQUEST_CODE_PICK_SOURCE_BUNCH) {
                    state.setSourceBunches(state.getSourceBunches().add(item));
                }
                else {
                    state.setDiffBunches(state.getDiffBunches().add(item));
                }
            }
            else if (requestCode == AgentEditorActivity.REQUEST_CODE_PICK_RULE) {
                final LangbookDbManager manager = DbManager.getInstance().getManager();
                final AcceptationId acceptation = AcceptationIdBundler.readAsIntentExtra(data, BundleKeys.ACCEPTATION);
                final Object item;
                if (acceptation != null) {
                    item = conceptAsRuleId(manager.conceptFromAcceptation(acceptation));
                }
                else {
                    final ParcelableCorrelationArray parcelableCorrelationArray = data.getParcelableExtra(BundleKeys.CORRELATION_ARRAY);
                    final ParcelableBunchIdSet bunchIdSet = data.getParcelableExtra(BundleKeys.BUNCH_SET);
                    item = new AcceptationDefinition(parcelableCorrelationArray.get(), bunchIdSet.get());
                }
                state.setRule(item);
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
