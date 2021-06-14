package sword.langbook3.android.db.check;

import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.ConceptConverter;
import sword.langbook3.android.db.ConceptIdInterface;
import sword.langbook3.android.db.ConceptualizableIdInterface;
import sword.langbook3.android.db.LangbookManager;

public final class TwoChainedAgentsPresentScenario<ConceptId extends ConceptIdInterface, LanguageId extends ConceptualizableIdInterface<ConceptId>, AlphabetId extends ConceptualizableIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId extends ConceptualizableIdInterface<ConceptId>, BunchSetId extends BunchSetIdInterface, RuleId extends ConceptualizableIdInterface<ConceptId>, AgentId, QuizId, SentenceId> {

    final LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager;
    final Logger logger;

    final ConceptConverter<ConceptId, BunchId> bunchIdConverter;
    final ConceptConverter<ConceptId, RuleId> ruleIdConverter;

    final SingleAlphabetCorrelationComposer<AlphabetId> enCorrelationComposer;
    final SingleAlphabetCorrelationArrayComposer<AlphabetId> enCorrelationArrayComposer;

    final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer;
    final DoubleAlphabetCorrelationArrayComposer<AlphabetId> correlationArrayComposer;

    final BunchId wishWordsBunch;

    TwoChainedAgentsPresentScenario(
            LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager,
            Logger logger,
            ConceptConverter<ConceptId, BunchId> bunchIdConverter,
            ConceptConverter<ConceptId, RuleId> ruleIdConverter,
            SingleAlphabetCorrelationComposer<AlphabetId> enCorrelationComposer,
            SingleAlphabetCorrelationArrayComposer<AlphabetId> enCorrelationArrayComposer,
            DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer,
            DoubleAlphabetCorrelationArrayComposer<AlphabetId> correlationArrayComposer,
            BunchId wishWordsBunch) {
        this.manager = manager;
        this.logger = logger;

        this.bunchIdConverter = bunchIdConverter;
        this.ruleIdConverter = ruleIdConverter;

        this.enCorrelationComposer = enCorrelationComposer;
        this.enCorrelationArrayComposer = enCorrelationArrayComposer;

        this.correlationComposer = correlationComposer;
        this.correlationArrayComposer = correlationArrayComposer;

        this.wishWordsBunch = wishWordsBunch;

        logger.scenarioReached(getClass().getSimpleName());
    }

    private final class IchidanVerbAdder {
        final BunchId bunch;

        IchidanVerbAdder(BunchId bunch) {
            this.bunch = bunch;
        }

        void addVerb(String englishText, String a1, String a2) {
            final ConceptId concept = manager.getNextAvailableConceptId();
            final AcceptationId acceptation = manager.addAcceptation(concept, correlationArrayComposer.compose(a1, a2, "る", "る"));
            manager.addAcceptation(concept, enCorrelationArrayComposer.compose(englishText));
            manager.addAcceptationInBunch(bunch, acceptation);
        }

        void addVerb(String englishText, String a1, String a2, String b1, String b2) {
            final ConceptId concept = manager.getNextAvailableConceptId();
            final AcceptationId acceptation = manager.addAcceptation(concept, correlationArrayComposer.compose(a1, a2, b1, b2, "る", "る"));
            manager.addAcceptation(concept, enCorrelationArrayComposer.compose(englishText));
            manager.addAcceptationInBunch(bunch, acceptation);
        }
    }

    public IchidanVerbsPresentScenario<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> add20IchidanVerbs() {
        final ConceptId ichidanVerbConcept = manager.getNextAvailableConceptId();
        manager.addAcceptation(ichidanVerbConcept, enCorrelationArrayComposer.compose("Japanese ichidan verbs"));
        final BunchId ichidanVerbBunch = bunchIdConverter.getKeyFromConceptId(ichidanVerbConcept);

        final IchidanVerbAdder adder = new IchidanVerbAdder(ichidanVerbBunch);
        adder.addVerb("delay", "遅", "おく", "れ", "れ");
        adder.addVerb("see", "見", "み");
        adder.addVerb("go out", "出", "で");
        adder.addVerb("put in", "入", "い", "れ", "れ");
        adder.addVerb("ride", "乗", "の", "せ", "せ");

        adder.addVerb("get off", "降", "お", "り", "り");
        adder.addVerb("wear", "着", "き");
        adder.addVerb("stop", "止", "や", "め", "め");
        adder.addVerb("turn", "曲", "ま", "げ", "げ");
        adder.addVerb("detach", "外", "はず", "れ", "れ");

        adder.addVerb("think", "考", "かんが", "え", "え");
        adder.addVerb("get with", "慣", "な", "れ", "れ");
        adder.addVerb("investigate", "調", "しら", "べ", "べ");
        adder.addVerb("learn", "修", "おさ", "め", "め");
        adder.addVerb("research", "究", "きわ", "め", "め");

        adder.addVerb("answer", "答", "こた", "え", "え");
        adder.addVerb("guide", "連", "つ", "れ", "れ");
        adder.addVerb("live", "生", "い", "き", "き");
        adder.addVerb("give", "上", "あ", "げ", "げ");
        adder.addVerb("burn", "燃", "も", "え", "え");

        return new IchidanVerbsPresentScenario<>(manager, logger, ruleIdConverter, enCorrelationComposer, enCorrelationArrayComposer, correlationComposer, correlationArrayComposer, wishWordsBunch, ichidanVerbBunch);
    }
}
