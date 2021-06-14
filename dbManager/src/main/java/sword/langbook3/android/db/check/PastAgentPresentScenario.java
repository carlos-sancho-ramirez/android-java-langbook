package sword.langbook3.android.db.check;

import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.ConceptConverter;
import sword.langbook3.android.db.ConceptIdInterface;
import sword.langbook3.android.db.ConceptualizableIdInterface;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookManager;

import static sword.langbook3.android.db.check.CheckUtils.setOf;

public final class PastAgentPresentScenario<ConceptId extends ConceptIdInterface, LanguageId extends ConceptualizableIdInterface<ConceptId>, AlphabetId extends ConceptualizableIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId extends ConceptualizableIdInterface<ConceptId>, BunchSetId extends BunchSetIdInterface, RuleId extends ConceptualizableIdInterface<ConceptId>, AgentId, QuizId, SentenceId> {

    final LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager;
    final Logger logger;

    final ConceptConverter<ConceptId, BunchId> bunchIdConverter;
    final ConceptConverter<ConceptId, RuleId> ruleIdConverter;

    final SingleAlphabetCorrelationComposer<AlphabetId> enCorrelationComposer;
    final SingleAlphabetCorrelationArrayComposer<AlphabetId> enCorrelationArrayComposer;

    final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer;
    final DoubleAlphabetCorrelationArrayComposer<AlphabetId> correlationArrayComposer;

    final BunchId iAdjectivesBunch;
    final BunchId canBePastBunch;
    final BunchId wishWordsBunch;

    PastAgentPresentScenario(
            LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager,
            Logger logger,
            ConceptConverter<ConceptId, BunchId> bunchIdConverter,
            ConceptConverter<ConceptId, RuleId> ruleIdConverter,
            SingleAlphabetCorrelationComposer<AlphabetId> enCorrelationComposer,
            SingleAlphabetCorrelationArrayComposer<AlphabetId> enCorrelationArrayComposer,
            DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer,
            DoubleAlphabetCorrelationArrayComposer<AlphabetId> correlationArrayComposer,
            BunchId iAdjectivesBunch,
            BunchId canBePastBunch,
            BunchId wishWordsBunch) {
        this.manager = manager;
        this.logger = logger;

        this.bunchIdConverter = bunchIdConverter;
        this.ruleIdConverter = ruleIdConverter;

        this.enCorrelationComposer = enCorrelationComposer;
        this.enCorrelationArrayComposer = enCorrelationArrayComposer;

        this.correlationComposer = correlationComposer;
        this.correlationArrayComposer = correlationArrayComposer;

        this.iAdjectivesBunch = iAdjectivesBunch;
        this.canBePastBunch = canBePastBunch;
        this.wishWordsBunch = wishWordsBunch;

        logger.scenarioReached(getClass().getSimpleName());
    }

    public TwoChainedAgentsPresentScenario<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> addNegativeAgent() {
        final ConceptId negativeConcept = manager.getNextAvailableConceptId();
        manager.addAcceptation(negativeConcept, correlationArrayComposer.compose("反", "はん", "対", "たい"));
        manager.addAcceptation(negativeConcept, enCorrelationArrayComposer.compose("negative"));

        final ImmutableCorrelation<AlphabetId> iCorrelation = correlationComposer.compose("い", "い");
        final ImmutableCorrelationArray<AlphabetId> kunaiCorrelationArray = correlationArrayComposer.compose("く", "く", "無", "な", "い", "い");
        final RuleId negativeRule = ruleIdConverter.getKeyFromConceptId(negativeConcept);
        manager.addAgent(setOf(canBePastBunch), setOf(iAdjectivesBunch, wishWordsBunch), setOf(), ImmutableCorrelation.empty(), ImmutableCorrelationArray.empty(), iCorrelation, kunaiCorrelationArray, negativeRule);

        return new TwoChainedAgentsPresentScenario<>(manager, logger,
                bunchIdConverter,
                ruleIdConverter,
                enCorrelationComposer,
                enCorrelationArrayComposer,
                correlationComposer,
                correlationArrayComposer, wishWordsBunch);
    }
}
