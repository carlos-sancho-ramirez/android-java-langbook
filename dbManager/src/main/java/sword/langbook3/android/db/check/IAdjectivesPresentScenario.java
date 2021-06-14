package sword.langbook3.android.db.check;

import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.ConceptConverter;
import sword.langbook3.android.db.ConceptIdInterface;
import sword.langbook3.android.db.ConceptualizableIdInterface;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookManager;

import static sword.langbook3.android.db.check.CheckUtils.setOf;

public final class IAdjectivesPresentScenario<ConceptId extends ConceptIdInterface, LanguageId extends ConceptualizableIdInterface<ConceptId>, AlphabetId extends ConceptualizableIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId extends ConceptualizableIdInterface<ConceptId>, BunchSetId extends BunchSetIdInterface, RuleId extends ConceptualizableIdInterface<ConceptId>, AgentId, QuizId, SentenceId> {

    final LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager;
    final Logger logger;

    final ConceptConverter<ConceptId, BunchId> bunchIdConverter;
    final ConceptConverter<ConceptId, RuleId> ruleIdConverter;

    final SingleAlphabetCorrelationComposer<AlphabetId> enCorrelationComposer;
    final SingleAlphabetCorrelationArrayComposer<AlphabetId> enCorrelationArrayComposer;

    final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer;
    final DoubleAlphabetCorrelationArrayComposer<AlphabetId> correlationArrayComposer;

    final BunchId iAdjectivesBunch;

    IAdjectivesPresentScenario(
            LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager,
            Logger logger,
            ConceptConverter<ConceptId, BunchId> bunchIdConverter,
            ConceptConverter<ConceptId, RuleId> ruleIdConverter,
            SingleAlphabetCorrelationComposer<AlphabetId> enCorrelationComposer,
            SingleAlphabetCorrelationArrayComposer<AlphabetId> enCorrelationArrayComposer,
            DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer,
            DoubleAlphabetCorrelationArrayComposer<AlphabetId> correlationArrayComposer,
            BunchId iAdjectivesBunch) {
        this.manager = manager;
        this.logger = logger;

        this.bunchIdConverter = bunchIdConverter;
        this.ruleIdConverter = ruleIdConverter;

        this.enCorrelationComposer = enCorrelationComposer;
        this.enCorrelationArrayComposer = enCorrelationArrayComposer;

        this.correlationComposer = correlationComposer;
        this.correlationArrayComposer = correlationArrayComposer;

        this.iAdjectivesBunch = iAdjectivesBunch;

        logger.scenarioReached(getClass().getSimpleName());
    }

    public PastAgentPresentScenario<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> addPastAgent() {
        final ConceptId pastConcept = manager.getNextAvailableConceptId();
        manager.addAcceptation(pastConcept, correlationArrayComposer.compose("過", "か", "去", "こ"));
        manager.addAcceptation(pastConcept, enCorrelationArrayComposer.compose("past"));

        final ConceptId canBePastConcept = manager.getNextAvailableConceptId();
        manager.addAcceptation(canBePastConcept, enCorrelationArrayComposer.compose("can be past changing suffix from i to katta"));
        final BunchId canBePastBunch = bunchIdConverter.getKeyFromConceptId(canBePastConcept);

        final ConceptId wishWordsConcept = manager.getNextAvailableConceptId();
        manager.addAcceptation(wishWordsConcept, enCorrelationArrayComposer.compose("can be wish changing suffix from ru to tai"));
        final BunchId wishWordsBunch = bunchIdConverter.getKeyFromConceptId(wishWordsConcept);

        final ImmutableCorrelation<AlphabetId> iCorrelation = correlationComposer.compose("い", "い");
        final ImmutableCorrelationArray<AlphabetId> kattaCorrelationArray = correlationArrayComposer.compose("かった", "かった");
        final RuleId pastRule = ruleIdConverter.getKeyFromConceptId(pastConcept);
        manager.addAgent(setOf(), setOf(iAdjectivesBunch, canBePastBunch, wishWordsBunch), setOf(), ImmutableCorrelation.empty(), ImmutableCorrelationArray.empty(), iCorrelation, kattaCorrelationArray, pastRule);

        return new PastAgentPresentScenario<>(manager, logger, bunchIdConverter, ruleIdConverter, enCorrelationComposer, enCorrelationArrayComposer, correlationComposer, correlationArrayComposer, iAdjectivesBunch, canBePastBunch, wishWordsBunch);
    }
}
