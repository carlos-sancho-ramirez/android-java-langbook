package sword.langbook3.android.db.check;

import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.ConceptConverter;
import sword.langbook3.android.db.ConceptIdInterface;
import sword.langbook3.android.db.ConceptualizableIdInterface;
import sword.langbook3.android.db.ImmutableCorrelation;
import sword.langbook3.android.db.ImmutableCorrelationArray;
import sword.langbook3.android.db.LangbookManager;

import static sword.langbook3.android.db.check.CheckUtils.setOf;

public final class IchidanVerbsPresentScenario<ConceptId extends ConceptIdInterface, LanguageId extends ConceptualizableIdInterface<ConceptId>, AlphabetId extends ConceptualizableIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId extends ConceptualizableIdInterface<ConceptId>, BunchSetId extends BunchSetIdInterface, RuleId extends ConceptualizableIdInterface<ConceptId>, AgentId, QuizId, SentenceId> {

    final LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager;
    final Logger logger;

    final ConceptConverter<ConceptId, RuleId> ruleIdConverter;

    final SingleAlphabetCorrelationComposer<AlphabetId> enCorrelationComposer;
    final SingleAlphabetCorrelationArrayComposer<AlphabetId> enCorrelationArrayComposer;

    final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer;
    final DoubleAlphabetCorrelationArrayComposer<AlphabetId> correlationArrayComposer;

    final BunchId wishWordsBunch;
    final BunchId ichidanVerbsBunch;

    IchidanVerbsPresentScenario(
            LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager,
            Logger logger,
            ConceptConverter<ConceptId, RuleId> ruleIdConverter,
            SingleAlphabetCorrelationComposer<AlphabetId> enCorrelationComposer,
            SingleAlphabetCorrelationArrayComposer<AlphabetId> enCorrelationArrayComposer,
            DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer,
            DoubleAlphabetCorrelationArrayComposer<AlphabetId> correlationArrayComposer,
            BunchId wishWordsBunch,
            BunchId ichidanVerbsBunch) {
        this.manager = manager;
        this.logger = logger;

        this.ruleIdConverter = ruleIdConverter;

        this.enCorrelationComposer = enCorrelationComposer;
        this.enCorrelationArrayComposer = enCorrelationArrayComposer;

        this.correlationComposer = correlationComposer;
        this.correlationArrayComposer = correlationArrayComposer;

        this.wishWordsBunch = wishWordsBunch;
        this.ichidanVerbsBunch = ichidanVerbsBunch;

        logger.scenarioReached(getClass().getSimpleName());
    }

    public ThreeChainedAgentsPresentScenario<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> addWishAgent() {
        final ConceptId wishConcept = manager.getNextAvailableConceptId();
        manager.addAcceptation(wishConcept, correlationArrayComposer.compose("願", "ねが", "い", "い"));
        manager.addAcceptation(wishConcept, enCorrelationArrayComposer.compose("wish"));

        final ImmutableCorrelation<AlphabetId> ruCorrelation = correlationComposer.compose("る", "る");
        final ImmutableCorrelationArray<AlphabetId> taiCorrelationArray = correlationArrayComposer.compose("た", "た", "い", "い");
        final RuleId wishRule = ruleIdConverter.getKeyFromConceptId(wishConcept);
        manager.addAgent(setOf(wishWordsBunch), setOf(ichidanVerbsBunch), setOf(), ImmutableCorrelation.empty(), ImmutableCorrelationArray.empty(), ruCorrelation, taiCorrelationArray, wishRule);

        return new ThreeChainedAgentsPresentScenario<>(manager, logger);
    }
}
