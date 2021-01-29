package sword.langbook3.android.db;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.Set;
import sword.database.Database;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.LanguageCreationResult;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.SentenceSpan;

public class LangbookDatabaseManager<LanguageId extends LanguageIdInterface, AlphabetId extends AlphabetIdInterface, SymbolArrayId extends SymbolArrayIdInterface, CorrelationId extends CorrelationIdInterface, CorrelationArrayId extends CorrelationArrayIdInterface> extends LangbookDatabaseChecker<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId> implements LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId> {

    private final Database _db;
    private final IntSetter<LanguageId> _languageIdSetter;
    private final IntSetter<AlphabetId> _alphabetIdSetter;
    private final IntSetter<SymbolArrayId> _symbolArrayIdSetter;
    private final IntSetter<CorrelationId> _correlationIdSetter;
    private final IntSetter<CorrelationArrayId> _correlationArrayIdSetter;

    public LangbookDatabaseManager(Database db, IntSetter<LanguageId> languageIdManager, IntSetter<AlphabetId> alphabetIdManager, IntSetter<SymbolArrayId> symbolArrayIdManager, IntSetter<CorrelationId> correlationIdSetter, IntSetter<CorrelationArrayId> correlationArrayIdSetter) {
        if (db == null || languageIdManager == null || alphabetIdManager == null || symbolArrayIdManager == null || correlationIdSetter == null || correlationArrayIdSetter == null) {
            throw new IllegalArgumentException();
        }

        _db = db;
        _languageIdSetter = languageIdManager;
        _alphabetIdSetter = alphabetIdManager;
        _symbolArrayIdSetter = symbolArrayIdManager;
        _correlationIdSetter = correlationIdSetter;
        _correlationArrayIdSetter = correlationArrayIdSetter;
    }

    @Override
    Database getDatabase() {
        return _db;
    }

    @Override
    IntSetter<LanguageId> getLanguageIdSetter() {
        return _languageIdSetter;
    }

    @Override
    IntSetter<AlphabetId> getAlphabetIdSetter() {
        return _alphabetIdSetter;
    }

    @Override
    IntSetter<CorrelationId> getCorrelationIdSetter() {
        return _correlationIdSetter;
    }

    @Override
    IntSetter<CorrelationArrayId> getCorrelationArrayIdSetter() {
        return _correlationArrayIdSetter;
    }

    @Override
    public Integer addAgent(
            ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches, ImmutableIntSet diffBunches,
            ImmutableCorrelation<AlphabetId> startMatcher, ImmutableCorrelation<AlphabetId> startAdder,
            ImmutableCorrelation<AlphabetId> endMatcher, ImmutableCorrelation<AlphabetId> endAdder, int rule) {
        return LangbookDatabase.addAgent(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, targetBunches, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    @Override
    public boolean updateAgent(
            int agentId, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches, ImmutableIntSet diffBunches,
            ImmutableCorrelation<AlphabetId> startMatcher, ImmutableCorrelation<AlphabetId> startAdder,
            ImmutableCorrelation<AlphabetId> endMatcher, ImmutableCorrelation<AlphabetId> endAdder, int rule) {
        return LangbookDatabase.updateAgent(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, agentId, targetBunches, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    @Override
    public void removeAgent(int agentId) {
        LangbookDatabase.removeAgent(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, agentId);
    }

    @Override
    public boolean addAcceptationInBunch(int bunch, int acceptation) {
        return LangbookDatabase.addAcceptationInBunch(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, bunch, acceptation);
    }

    @Override
    public boolean removeAcceptationFromBunch(int bunch, int acceptation) {
        return LangbookDatabase.removeAcceptationFromBunch(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, bunch, acceptation);
    }

    @Override
    public LanguageCreationResult<LanguageId, AlphabetId> addLanguage(String code) {
        return LangbookDatabase.addLanguage(_db, _languageIdSetter, _alphabetIdSetter, code);
    }

    @Override
    public boolean removeLanguage(LanguageId language) {
        return LangbookDatabase.removeLanguage(_db, _languageIdSetter, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, language);
    }

    @Override
    public boolean addAlphabetCopyingFromOther(AlphabetId alphabet, AlphabetId sourceAlphabet) {
        return LangbookDatabase.addAlphabetCopyingFromOther(_db, _languageIdSetter, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, alphabet, sourceAlphabet);
    }

    @Override
    public boolean addAlphabetAsConversionTarget(Conversion<AlphabetId> conversion) {
        return LangbookDatabase.addAlphabetAsConversionTarget(_db, _languageIdSetter, _symbolArrayIdSetter, conversion);
    }

    @Override
    public boolean removeAlphabet(AlphabetId alphabet) {
        return LangbookDatabase.removeAlphabet(_db, _alphabetIdSetter, alphabet);
    }

    @Override
    public Integer addAcceptation(int concept, ImmutableCorrelationArray<AlphabetId> correlationArray) {
        return LangbookDatabase.addAcceptation(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, concept, correlationArray);
    }

    @Override
    public boolean updateAcceptationCorrelationArray(int acceptation, ImmutableCorrelationArray<AlphabetId> newCorrelationArray) {
        return LangbookDatabase.updateAcceptationCorrelationArray(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, acceptation, newCorrelationArray);
    }

    @Override
    public boolean removeAcceptation(int acceptation) {
        return LangbookDatabase.removeAcceptation(_db, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, acceptation);
    }

    @Override
    public boolean shareConcept(int linkedAcceptation, int oldConcept) {
        return LangbookDatabase.shareConcept(_db, linkedAcceptation, oldConcept);
    }

    @Override
    public void duplicateAcceptationWithThisConcept(int linkedAcceptation, int concept) {
        LangbookDatabase.duplicateAcceptationWithThisConcept(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, linkedAcceptation, concept);
    }

    @Override
    public boolean replaceConversion(Conversion<AlphabetId> conversion) {
        return LangbookDatabase.replaceConversion(_db, _languageIdSetter, _symbolArrayIdSetter, conversion);
    }

    @Override
    public Integer obtainQuiz(int bunch, ImmutableList<QuestionFieldDetails<AlphabetId>> fields) {
        return LangbookDatabase.obtainQuiz(_db, _alphabetIdSetter, bunch, fields);
    }

    @Override
    public void removeQuiz(int quizId) {
        LangbookDatabase.removeQuiz(_db, quizId);
    }

    @Override
    public void updateScore(int quizId, int acceptation, int score) {
        LangbookDatabase.updateScore(_db, quizId, acceptation, score);
    }

    @Override
    public void addDefinition(int baseConcept, int concept, ImmutableIntSet complements) {
        LangbookDatabase.addDefinition(_db, baseConcept, concept, complements);
    }

    @Override
    public boolean removeDefinition(int complementedConcept) {
        return LangbookDatabase.removeDefinition(_db, complementedConcept);
    }

    @Override
    public void updateSearchHistory(int dynamicAcceptation) {
        LangbookDatabase.updateSearchHistory(_db, dynamicAcceptation);
    }

    @Override
    public boolean copySentenceConcept(int sourceSentenceId, int targetSentenceId) {
        return LangbookDatabase.copySentenceConcept(_db, sourceSentenceId, targetSentenceId);
    }

    @Override
    public boolean removeSentence(int sentenceId) {
        return LangbookDatabase.removeSentence(_db, _symbolArrayIdSetter, sentenceId);
    }

    @Override
    public Integer addSentence(int concept, String text, Set<SentenceSpan> spans) {
        return LangbookDatabase.addSentence(_db, _symbolArrayIdSetter, concept, text, spans);
    }

    @Override
    public boolean updateSentenceTextAndSpans(int sentenceId, String newText, Set<SentenceSpan> newSpans) {
        return LangbookDatabase.updateSentenceTextAndSpans(_db, _symbolArrayIdSetter, sentenceId, newText, newSpans);
    }
}
