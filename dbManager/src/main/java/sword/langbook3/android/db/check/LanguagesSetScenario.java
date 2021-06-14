package sword.langbook3.android.db.check;

import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableMap;
import sword.langbook3.android.db.BunchSetIdInterface;
import sword.langbook3.android.db.ConceptConverter;
import sword.langbook3.android.db.ConceptIdInterface;
import sword.langbook3.android.db.ConceptualizableIdInterface;
import sword.langbook3.android.db.LangbookManager;
import sword.langbook3.android.models.Conversion;

public final class LanguagesSetScenario<ConceptId extends ConceptIdInterface, LanguageId extends ConceptualizableIdInterface<ConceptId>, AlphabetId extends ConceptualizableIdInterface<ConceptId>, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId extends ConceptualizableIdInterface<ConceptId>, BunchSetId extends BunchSetIdInterface, RuleId extends ConceptualizableIdInterface<ConceptId>, AgentId, QuizId, SentenceId> {

    final LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager;
    final Logger logger;

    final ConceptConverter<ConceptId, AlphabetId> alphabetIdConverter;
    final ConceptConverter<ConceptId, BunchId> bunchIdConverter;
    final ConceptConverter<ConceptId, RuleId> ruleIdConverter;

    final SingleAlphabetCorrelationComposer<AlphabetId> enCorrelationComposer;
    final SingleAlphabetCorrelationArrayComposer<AlphabetId> enCorrelationArrayComposer;

    final DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer;
    final DoubleAlphabetCorrelationArrayComposer<AlphabetId> correlationArrayComposer;

    final AlphabetId kana;

    LanguagesSetScenario(
            LangbookManager<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> manager,
            Logger logger,
            ConceptConverter<ConceptId, AlphabetId> alphabetIdConverter,
            ConceptConverter<ConceptId, BunchId> bunchIdConverter,
            ConceptConverter<ConceptId, RuleId> ruleIdConverter,
            SingleAlphabetCorrelationComposer<AlphabetId> enCorrelationComposer,
            SingleAlphabetCorrelationArrayComposer<AlphabetId> enCorrelationArrayComposer,
            DoubleAlphabetCorrelationComposer<AlphabetId> correlationComposer,
            DoubleAlphabetCorrelationArrayComposer<AlphabetId> correlationArrayComposer,
            AlphabetId kana) {
        this.manager = manager;
        this.logger = logger;

        this.alphabetIdConverter = alphabetIdConverter;
        this.bunchIdConverter = bunchIdConverter;
        this.ruleIdConverter = ruleIdConverter;

        this.enCorrelationComposer = enCorrelationComposer;
        this.enCorrelationArrayComposer = enCorrelationArrayComposer;

        this.correlationComposer = correlationComposer;
        this.correlationArrayComposer = correlationArrayComposer;

        this.kana = kana;

        logger.scenarioReached(getClass().getSimpleName());
    }

    public RoumajiConversionPresentScenario<ConceptId, LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> addConversion() {
        final ImmutableMap<String, String> conversionMap = new ImmutableHashMap.Builder<String, String>()
                .put("あ", "a")
                .put("い", "i")
                .put("う", "u")
                .put("え", "e")
                .put("お", "o")
                .put("か", "ka")
                .put("き", "ki")
                .put("く", "ku")
                .put("け", "ke")
                .put("こ", "ko")
                .put("が", "ga")
                .put("ぎ", "gi")
                .put("ぐ", "gu")
                .put("げ", "ge")
                .put("ご", "go")
                .put("さ", "sa")
                .put("し", "shi")
                .put("す", "su")
                .put("せ", "se")
                .put("そ", "so")
                .put("ざ", "za")
                .put("じ", "ji")
                .put("ず", "zu")
                .put("ぜ", "ze")
                .put("ぞ", "zo")
                .put("た", "ta")
                .put("った", "tta")
                .put("ち", "chi")
                .put("つ", "tsu")
                .put("て", "te")
                .put("と", "to")
                .put("だ", "da")
                .put("ぢ", "di")
                .put("づ", "du")
                .put("で", "de")
                .put("ど", "do")
                .put("な", "na")
                .put("に", "ni")
                .put("ぬ", "nu")
                .put("ね", "ne")
                .put("の", "no")
                .put("は", "ha")
                .put("ひ", "hi")
                .put("ふ", "hu")
                .put("へ", "he")
                .put("ほ", "ho")
                .put("ば", "ba")
                .put("び", "bi")
                .put("ぶ", "bu")
                .put("べ", "be")
                .put("ぼ", "bo")
                .put("ぱ", "pa")
                .put("ぴ", "pi")
                .put("ぷ", "pu")
                .put("ぺ", "pe")
                .put("ぽ", "po")
                .put("ま", "ma")
                .put("み", "mi")
                .put("む", "mu")
                .put("め", "me")
                .put("も", "mo")
                .put("や", "ya")
                .put("ゆ", "yu")
                .put("よ", "yo")
                .put("ら", "ra")
                .put("り", "ri")
                .put("る", "ru")
                .put("れ", "re")
                .put("ろ", "ro")
                .put("わ", "wa")
                .put("を", "wo")
                .put("ん", "n")
                .build();

        final AlphabetId roumaji = alphabetIdConverter.getKeyFromConceptId(manager.getNextAvailableConceptId());
        manager.addAcceptation(roumaji.getConceptId(), correlationArrayComposer.compose("ろう", "ろう", "ま", "ま", "字", "じ"));

        final Conversion<AlphabetId> conversion = new Conversion<>(kana, roumaji, conversionMap);
        manager.addAlphabetAsConversionTarget(conversion);

        return new RoumajiConversionPresentScenario<>(manager, logger, bunchIdConverter, ruleIdConverter, enCorrelationComposer, enCorrelationArrayComposer, correlationComposer, correlationArrayComposer);
    }
}
