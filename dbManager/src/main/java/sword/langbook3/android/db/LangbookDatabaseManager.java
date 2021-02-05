package sword.langbook3.android.db;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.Set;
import sword.database.Database;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.LanguageCreationResult;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.SentenceSpan;

public class LangbookDatabaseManager<LanguageId extends LanguageIdInterface, AlphabetId extends AlphabetIdInterface, SymbolArrayId extends SymbolArrayIdInterface, CorrelationId extends CorrelationIdInterface, CorrelationArrayId extends CorrelationArrayIdInterface, AcceptationId extends AcceptationIdInterface> extends LangbookDatabaseChecker<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId> implements LangbookManager<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId> {

    private final Database _db;
    private final IntSetter<LanguageId> _languageIdSetter;
    private final IntSetter<AlphabetId> _alphabetIdSetter;
    private final IntSetter<SymbolArrayId> _symbolArrayIdSetter;
    private final IntSetter<CorrelationId> _correlationIdSetter;
    private final IntSetter<CorrelationArrayId> _correlationArrayIdSetter;
    private final IntSetter<AcceptationId> _acceptationIdSetter;

    public LangbookDatabaseManager(Database db, IntSetter<LanguageId> languageIdManager, IntSetter<AlphabetId> alphabetIdManager, IntSetter<SymbolArrayId> symbolArrayIdManager, IntSetter<CorrelationId> correlationIdSetter, IntSetter<CorrelationArrayId> correlationArrayIdSetter, IntSetter<AcceptationId> acceptationIdSetter) {
        if (db == null || languageIdManager == null || alphabetIdManager == null || symbolArrayIdManager == null || correlationIdSetter == null || correlationArrayIdSetter == null || acceptationIdSetter == null) {
            throw new IllegalArgumentException();
        }

        _db = db;
        _languageIdSetter = languageIdManager;
        _alphabetIdSetter = alphabetIdManager;
        _symbolArrayIdSetter = symbolArrayIdManager;
        _correlationIdSetter = correlationIdSetter;
        _correlationArrayIdSetter = correlationArrayIdSetter;
        _acceptationIdSetter = acceptationIdSetter;
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
    IntSetter<AcceptationId> getAcceptationIdSetter() {
        return _acceptationIdSetter;
    }

    @Override
    public Integer addAgent(
            ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches, ImmutableIntSet diffBunches,
            ImmutableCorrelation<AlphabetId> startMatcher, ImmutableCorrelation<AlphabetId> startAdder,
            ImmutableCorrelation<AlphabetId> endMatcher, ImmutableCorrelation<AlphabetId> endAdder, int rule) {
        return LangbookDatabase.addAgent(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, _acceptationIdSetter, targetBunches, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    @Override
    public boolean updateAgent(
            int agentId, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches, ImmutableIntSet diffBunches,
            ImmutableCorrelation<AlphabetId> startMatcher, ImmutableCorrelation<AlphabetId> startAdder,
            ImmutableCorrelation<AlphabetId> endMatcher, ImmutableCorrelation<AlphabetId> endAdder, int rule) {
        return LangbookDatabase.updateAgent(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, _acceptationIdSetter, agentId, targetBunches, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    @Override
    public void removeAgent(int agentId) {
        LangbookDatabase.removeAgent(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, _acceptationIdSetter, agentId);
    }

    @Override
    public boolean addAcceptationInBunch(int bunch, AcceptationId acceptation) {
        return LangbookDatabase.addAcceptationInBunch(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, _acceptationIdSetter, bunch, acceptation);
    }

    @Override
    public boolean removeAcceptationFromBunch(int bunch, AcceptationId acceptation) {
        return LangbookDatabase.removeAcceptationFromBunch(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, _acceptationIdSetter, bunch, acceptation);
    }

    @Override
    public LanguageCreationResult<LanguageId, AlphabetId> addLanguage(String code) {
        return LangbookDatabase.addLanguage(_db, _languageIdSetter, _alphabetIdSetter, code);
    }

    @Override
    public boolean removeLanguage(LanguageId language) {
        return LangbookDatabase.removeLanguage(_db, _languageIdSetter, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, _acceptationIdSetter, language);
    }

    @Override
    public boolean addAlphabetCopyingFromOther(AlphabetId alphabet, AlphabetId sourceAlphabet) {
        return LangbookDatabase.addAlphabetCopyingFromOther(_db, _languageIdSetter, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _acceptationIdSetter, alphabet, sourceAlphabet);
    }

    @Override
    public boolean addAlphabetAsConversionTarget(Conversion<AlphabetId> conversion) {
        return LangbookDatabase.addAlphabetAsConversionTarget(_db, _languageIdSetter, _symbolArrayIdSetter, _acceptationIdSetter, conversion);
    }

    @Override
    public boolean removeAlphabet(AlphabetId alphabet) {
        return LangbookDatabase.removeAlphabet(_db, _alphabetIdSetter, alphabet);
    }

    @Override
    public AcceptationId addAcceptation(int concept, ImmutableCorrelationArray<AlphabetId> correlationArray) {
        return LangbookDatabase.addAcceptation(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, _acceptationIdSetter, concept, correlationArray);
    }

    @Override
    public boolean updateAcceptationCorrelationArray(AcceptationId acceptation, ImmutableCorrelationArray<AlphabetId> newCorrelationArray) {
        return LangbookDatabase.updateAcceptationCorrelationArray(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, _acceptationIdSetter, acceptation, newCorrelationArray);
    }

    @Override
    public boolean removeAcceptation(AcceptationId acceptation) {
        return LangbookDatabase.removeAcceptation(_db, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, _acceptationIdSetter, acceptation);
    }

    @Override
    public boolean shareConcept(AcceptationId linkedAcceptation, int oldConcept) {
        return LangbookDatabase.shareConcept(_db, _correlationArrayIdSetter, _acceptationIdSetter, linkedAcceptation, oldConcept);
    }

    @Override
    public void duplicateAcceptationWithThisConcept(AcceptationId linkedAcceptation, int concept) {
        LangbookDatabase.duplicateAcceptationWithThisConcept(_db, _alphabetIdSetter, _symbolArrayIdSetter, _correlationIdSetter, _correlationArrayIdSetter, _acceptationIdSetter, linkedAcceptation, concept);
    }

    @Override
    public boolean replaceConversion(Conversion<AlphabetId> conversion) {
        return LangbookDatabase.replaceConversion(_db, _languageIdSetter, _symbolArrayIdSetter, _acceptationIdSetter, conversion);
    }

    @Override
    public Integer obtainQuiz(int bunch, ImmutableList<QuestionFieldDetails<AlphabetId>> fields) {
        return LangbookDatabase.obtainQuiz(_db, _alphabetIdSetter, _acceptationIdSetter, bunch, fields);
    }

    @Override
    public void removeQuiz(int quizId) {
        LangbookDatabase.removeQuiz(_db, quizId);
    }

    @Override
    public void updateScore(int quizId, AcceptationId acceptation, int score) {
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
    public void updateSearchHistory(AcceptationId dynamicAcceptation) {
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
    public Integer addSentence(int concept, String text, Set<SentenceSpan<AcceptationId>> spans) {
        return LangbookDatabase.addSentence(_db, _symbolArrayIdSetter, concept, text, spans);
    }

    @Override
    public boolean updateSentenceTextAndSpans(int sentenceId, String newText, Set<SentenceSpan<AcceptationId>> newSpans) {
        return LangbookDatabase.updateSentenceTextAndSpans(_db, _symbolArrayIdSetter, _acceptationIdSetter, sentenceId, newText, newSpans);
    }
}
