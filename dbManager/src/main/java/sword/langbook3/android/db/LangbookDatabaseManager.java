package sword.langbook3.android.db;

import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.Set;
import sword.database.Database;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.LanguageCreationResult;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.SentenceSpan;

public final class LangbookDatabaseManager<LanguageId extends LanguageIdInterface, AlphabetId extends AlphabetIdInterface> extends LangbookDatabaseChecker<LanguageId, AlphabetId> implements LangbookManager<LanguageId, AlphabetId> {

    private final Database _db;
    private final IntSetter<LanguageId> _languageIdSetter;
    private final IntSetter<AlphabetId> _alphabetIdSetter;

    public LangbookDatabaseManager(Database db, IntSetter<LanguageId> languageIdManager, IntSetter<AlphabetId> alphabetIdManager) {
        if (db == null || languageIdManager == null || alphabetIdManager == null) {
            throw new IllegalArgumentException();
        }

        _db = db;
        _languageIdSetter = languageIdManager;
        _alphabetIdSetter = alphabetIdManager;
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
    public Integer addAgent(
            ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches, ImmutableIntSet diffBunches,
            ImmutableCorrelation<AlphabetId> startMatcher, ImmutableCorrelation<AlphabetId> startAdder,
            ImmutableCorrelation<AlphabetId> endMatcher, ImmutableCorrelation<AlphabetId> endAdder, int rule) {
        return LangbookDatabase.addAgent(_db, _alphabetIdSetter, targetBunches, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    @Override
    public boolean updateAgent(
            int agentId, ImmutableIntSet targetBunches, ImmutableIntSet sourceBunches, ImmutableIntSet diffBunches,
            ImmutableCorrelation<AlphabetId> startMatcher, ImmutableCorrelation<AlphabetId> startAdder,
            ImmutableCorrelation<AlphabetId> endMatcher, ImmutableCorrelation<AlphabetId> endAdder, int rule) {
        return LangbookDatabase.updateAgent(_db, _alphabetIdSetter, agentId, targetBunches, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    @Override
    public void removeAgent(int agentId) {
        LangbookDatabase.removeAgent(_db, _alphabetIdSetter, agentId);
    }

    @Override
    public boolean addAcceptationInBunch(int bunch, int acceptation) {
        return LangbookDatabase.addAcceptationInBunch(_db, _alphabetIdSetter, bunch, acceptation);
    }

    @Override
    public boolean removeAcceptationFromBunch(int bunch, int acceptation) {
        return LangbookDatabase.removeAcceptationFromBunch(_db, _alphabetIdSetter, bunch, acceptation);
    }

    @Override
    public LanguageCreationResult<LanguageId, AlphabetId> addLanguage(String code) {
        return LangbookDatabase.addLanguage(_db, _languageIdSetter, _alphabetIdSetter, code);
    }

    @Override
    public boolean removeLanguage(LanguageId language) {
        return LangbookDatabase.removeLanguage(_db, _languageIdSetter, _alphabetIdSetter, language);
    }

    @Override
    public boolean addAlphabetCopyingFromOther(AlphabetId alphabet, AlphabetId sourceAlphabet) {
        return LangbookDatabase.addAlphabetCopyingFromOther(_db, _languageIdSetter, _alphabetIdSetter, alphabet, sourceAlphabet);
    }

    @Override
    public boolean addAlphabetAsConversionTarget(Conversion<AlphabetId> conversion) {
        return LangbookDatabase.addAlphabetAsConversionTarget(_db, _languageIdSetter, conversion);
    }

    @Override
    public boolean removeAlphabet(AlphabetId alphabet) {
        return LangbookDatabase.removeAlphabet(_db, _alphabetIdSetter, alphabet);
    }

    @Override
    public Integer addAcceptation(int concept, ImmutableCorrelationArray<AlphabetId> correlationArray) {
        return LangbookDatabase.addAcceptation(_db, _alphabetIdSetter, concept, correlationArray);
    }

    @Override
    public boolean updateAcceptationCorrelationArray(int acceptation, ImmutableCorrelationArray<AlphabetId> newCorrelationArray) {
        return LangbookDatabase.updateAcceptationCorrelationArray(_db, _alphabetIdSetter, acceptation, newCorrelationArray);
    }

    @Override
    public boolean removeAcceptation(int acceptation) {
        return LangbookDatabase.removeAcceptation(_db, acceptation);
    }

    @Override
    public boolean shareConcept(int linkedAcceptation, int oldConcept) {
        return LangbookDatabase.shareConcept(_db, linkedAcceptation, oldConcept);
    }

    @Override
    public void duplicateAcceptationWithThisConcept(int linkedAcceptation, int concept) {
        LangbookDatabase.duplicateAcceptationWithThisConcept(_db, _alphabetIdSetter, linkedAcceptation, concept);
    }

    @Override
    public boolean replaceConversion(Conversion<AlphabetId> conversion) {
        return LangbookDatabase.replaceConversion(_db, _languageIdSetter, conversion);
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
        return LangbookDatabase.removeSentence(_db, sentenceId);
    }

    @Override
    public Integer addSentence(int concept, String text, Set<SentenceSpan> spans) {
        return LangbookDatabase.addSentence(_db, concept, text, spans);
    }

    @Override
    public boolean updateSentenceTextAndSpans(int sentenceId, String newText, Set<SentenceSpan> newSpans) {
        return LangbookDatabase.updateSentenceTextAndSpans(_db, sentenceId, newText, newSpans);
    }
}
