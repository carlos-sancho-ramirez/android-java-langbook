package sword.langbook3.android.db.check;

import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.ConceptConverter;
import sword.langbook3.android.db.ConceptIdInterface;
import sword.langbook3.android.db.ConceptualizableIdInterface;
import sword.langbook3.android.db.LangbookManager;

public final class RoumajiConversionPresentScenario<ConceptId extends ConceptIdInterface, LanguageId extends ConceptualizableIdInterface<ConceptId>, AlphabetId extends ConceptualizableIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId extends ConceptualizableIdInterface<ConceptId>, BunchSetId extends BunchSetIdInterface, RuleId extends ConceptualizableIdInterface<ConceptId>, AgentId, QuizId, SentenceId> {

    final LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager;
    final Logger logger;

    final ConceptConverter<ConceptId, BunchId> bunchIdConverter;
    final ConceptConverter<ConceptId, RuleId> ruleIdConverter;

    final SingleAlphabetCorrelationComposer<AlphabetId> enCorrelationComposer;
    final SingleAlphabetCorrelationArrayComposer<AlphabetId> enCorrelationArrayComposer;

    final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer;
    final DoubleAlphabetCorrelationArrayComposer<AlphabetId> correlationArrayComposer;

    RoumajiConversionPresentScenario(
            LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager,
            Logger logger,
            ConceptConverter<ConceptId, BunchId> bunchIdConverter,
            ConceptConverter<ConceptId, RuleId> ruleIdConverter,
            SingleAlphabetCorrelationComposer<AlphabetId> enCorrelationComposer,
            SingleAlphabetCorrelationArrayComposer<AlphabetId> enCorrelationArrayComposer,
            DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer,
            DoubleAlphabetCorrelationArrayComposer<AlphabetId> correlationArrayComposer) {
        this.manager = manager;
        this.logger = logger;

        this.bunchIdConverter = bunchIdConverter;
        this.ruleIdConverter = ruleIdConverter;

        this.enCorrelationComposer = enCorrelationComposer;
        this.enCorrelationArrayComposer = enCorrelationArrayComposer;

        this.correlationComposer = correlationComposer;
        this.correlationArrayComposer = correlationArrayComposer;

        logger.scenarioReached(getClass().getSimpleName());
    }

    private final class AcceptationAdder {
        final BunchId bunch;

        AcceptationAdder(BunchId bunch) {
            this.bunch = bunch;
        }

        void addIAdjective(String englishText, String a1, String a2) {
            final ConceptId concept = manager.getNextAvailableConceptId();
            final AcceptationId acceptation = manager.addAcceptation(concept, correlationArrayComposer.compose(a1, a2, "い", "い"));
            manager.addAcceptation(concept, enCorrelationArrayComposer.compose(englishText));
            manager.addAcceptationInBunch(bunch, acceptation);
        }

        void addIAdjective(String englishText, String a1, String a2, String b1, String b2) {
            final ConceptId concept = manager.getNextAvailableConceptId();
            final AcceptationId acceptation = manager.addAcceptation(concept, correlationArrayComposer.compose(a1, a2, b1, b2, "い", "い"));
            manager.addAcceptation(concept, enCorrelationArrayComposer.compose(englishText));
            manager.addAcceptationInBunch(bunch, acceptation);
        }
    }

    public IAdjectivesPresentScenario<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> add4IAdjectives() {
        final ConceptId iAdjectivesConcept = manager.getNextAvailableConceptId();
        manager.addAcceptation(iAdjectivesConcept, enCorrelationArrayComposer.compose("Japanese i-adjectives"));
        final BunchId iAdjectivesBunch = bunchIdConverter.getKeyFromConceptId(iAdjectivesConcept);

        final AcceptationAdder adder = new AcceptationAdder(iAdjectivesBunch);
        adder.addIAdjective("high", "高", "たか");
        adder.addIAdjective("low", "低", "ひく");
        adder.addIAdjective("narrow", "狭", "せま");
        adder.addIAdjective("red", "赤", "あか");

        return new IAdjectivesPresentScenario<>(manager, logger, bunchIdConverter, ruleIdConverter, enCorrelationComposer, enCorrelationArrayComposer, correlationComposer, correlationArrayComposer, iAdjectivesBunch);
    }

    public IAdjectivesPresentScenario<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> add20IAdjectives() {
        final ConceptId iAdjectivesConcept = manager.getNextAvailableConceptId();
        manager.addAcceptation(iAdjectivesConcept, enCorrelationArrayComposer.compose("Japanese i-adjectives"));
        final BunchId iAdjectivesBunch = bunchIdConverter.getKeyFromConceptId(iAdjectivesConcept);

        final AcceptationAdder adder = new AcceptationAdder(iAdjectivesBunch);
        adder.addIAdjective("high", "高", "たか");
        adder.addIAdjective("low", "低", "ひく");
        adder.addIAdjective("narrow", "狭", "せま");
        adder.addIAdjective("red", "赤", "あか");
        adder.addIAdjective("near", "近", "ちか");

        adder.addIAdjective("far", "遠", "とお");
        adder.addIAdjective("blue", "青", "あお");
        adder.addIAdjective("light", "明", "あか", "る", "る");
        adder.addIAdjective("young", "若", "わか");
        adder.addIAdjective("dangerous", "危", "あぶ", "な", "な");

        adder.addIAdjective("strong", "強", "つよ");
        adder.addIAdjective("weak", "弱い", "よわ");
        adder.addIAdjective("big", "大", "おお", "き", "き");
        adder.addIAdjective("small", "小", "ちい", "さ", "さ");
        adder.addIAdjective("suspicious", "怪", "あや", "し", "し");

        adder.addIAdjective("refreshing", "涼", "すず", "し", "し");
        adder.addIAdjective("gentle", "優", "やさ", "し", "し");
        adder.addIAdjective("impressive", "凄", "すご");
        adder.addIAdjective("funny", "面", "おも", "白", "しろ");
        adder.addIAdjective("cheap", "安", "やす");

        return new IAdjectivesPresentScenario<>(manager, logger, bunchIdConverter, ruleIdConverter, enCorrelationComposer, enCorrelationArrayComposer, correlationComposer, correlationArrayComposer, iAdjectivesBunch);
    }
}
