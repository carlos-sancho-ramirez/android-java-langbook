package sword.langbook3.android.db.check;

import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.ConceptConverter;
import sword.langbook3.android.db.ConceptIdInterface;
import sword.langbook3.android.db.ConceptualizableIdInterface;
import sword.langbook3.android.db.LangbookManager;
import sword.langbook3.android.models.LanguageCreationResult;

public final class ClearScenario<ConceptId extends ConceptIdInterface, LanguageId extends ConceptualizableIdInterface<ConceptId>, AlphabetId extends ConceptualizableIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId extends ConceptualizableIdInterface<ConceptId>, BunchSetId extends BunchSetIdInterface, RuleId extends ConceptualizableIdInterface<ConceptId>, AgentId, QuizId, SentenceId> {

    public static <ConceptId extends ConceptIdInterface, LanguageId extends ConceptualizableIdInterface<ConceptId>, AlphabetId extends ConceptualizableIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId extends ConceptualizableIdInterface<ConceptId>, BunchSetId extends BunchSetIdInterface, RuleId extends ConceptualizableIdInterface<ConceptId>, AgentId, QuizId, SentenceId> ClearScenario<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> setUp(LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager, Logger logger, ConceptConverter<ConceptId, AlphabetId> alphabetIdConverter, ConceptConverter<ConceptId, BunchId> bunchIdConverter, ConceptConverter<ConceptId, RuleId> ruleIdConverter) {
        return new ClearScenario<>(manager, logger, alphabetIdConverter, bunchIdConverter, ruleIdConverter);
    }

    final LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager;
    final Logger logger;

    final ConceptConverter<ConceptId, AlphabetId> alphabetIdConverter;
    final ConceptConverter<ConceptId, BunchId> bunchIdConverter;
    final ConceptConverter<ConceptId, RuleId> ruleIdConverter;

    ClearScenario(
            LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager,
            Logger logger,
            ConceptConverter<ConceptId, AlphabetId> alphabetIdConverter,
            ConceptConverter<ConceptId, BunchId> bunchIdConverter,
            ConceptConverter<ConceptId, RuleId> ruleIdConverter) {
        this.manager = manager;
        this.logger = logger;

        this.alphabetIdConverter = alphabetIdConverter;
        this.bunchIdConverter = bunchIdConverter;
        this.ruleIdConverter = ruleIdConverter;

        logger.scenarioReached(getClass().getSimpleName());
    }

    public LanguagesSetScenario<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> setLanguages() {
        final LanguageCreationResult<LanguageId, AlphabetId> enLangCreationResult = manager.addLanguage("en");
        final AlphabetId enAlphabet = enLangCreationResult.mainAlphabet;

        final LanguageCreationResult<LanguageId, AlphabetId> jaLangCreationResult = manager.addLanguage("ja");
        final AlphabetId kanji = jaLangCreationResult.mainAlphabet;
        final AlphabetId kana = alphabetIdConverter.getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAlphabetCopyingFromOther(kana, kanji);

        final SingleAlphabetCorrelationComposer<AlphabetId> enCorrelationComposer = new SingleAlphabetCorrelationComposer<>(enAlphabet);
        final SingleAlphabetCorrelationArrayComposer<AlphabetId> enCorrelationArrayComposer = new SingleAlphabetCorrelationArrayComposer<>(enCorrelationComposer);

        final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer = new DoubleAlphabetCorrelationComposer<>(kanji, kana);
        final DoubleAlphabetCorrelationArrayComposer<AlphabetId> correlationArrayComposer = new DoubleAlphabetCorrelationArrayComposer<>(correlationComposer);

        manager.addAcceptation(enLangCreationResult.language.getConceptId(), enCorrelationArrayComposer.compose("English"));
        manager.addAcceptation(enLangCreationResult.language.getConceptId(), correlationArrayComposer.compose("英", "えい", "語", "ご"));

        manager.addAcceptation(enLangCreationResult.mainAlphabet.getConceptId(), enCorrelationArrayComposer.compose("English"));
        manager.addAcceptation(enLangCreationResult.mainAlphabet.getConceptId(), correlationArrayComposer.compose("英", "えい", "語", "ご"));

        manager.addAcceptation(jaLangCreationResult.language.getConceptId(), enCorrelationArrayComposer.compose("Japanese"));
        manager.addAcceptation(jaLangCreationResult.language.getConceptId(), correlationArrayComposer.compose("日", "に", "本", "ほん", "語", "ご"));

        manager.addAcceptation(kanji.getConceptId(), correlationArrayComposer.compose("漢", "かん", "字", "じ"));
        manager.addAcceptation(kana.getConceptId(), correlationArrayComposer.compose("仮", "か", "名", "な"));

        return new LanguagesSetScenario<>(manager, logger, alphabetIdConverter, bunchIdConverter, ruleIdConverter, enCorrelationComposer, enCorrelationArrayComposer, correlationComposer, correlationArrayComposer, kana);
    }
}
