package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.database.Database;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.LanguageCreationResult;
import sword.langbook3.android.models.QuestionFieldDetails;

public final class LangbookDatabaseManager extends LangbookDatabaseChecker implements QuizzesManager, DefinitionsManager {

    private final Database _db;

    public LangbookDatabaseManager(Database db) {
        _db = db;
    }

    @Override
    Database getDatabase() {
        return _db;
    }

    @Override
    public int obtainRuledConcept(int rule, int concept) {
        return LangbookDatabase.obtainRuledConcept(_db, rule, concept);
    }

    @Override
    public Integer addAgent(
            int targetBunch, ImmutableIntSet sourceBunches, ImmutableIntSet diffBunches,
            ImmutableIntKeyMap<String> startMatcher, ImmutableIntKeyMap<String> startAdder,
            ImmutableIntKeyMap<String> endMatcher, ImmutableIntKeyMap<String> endAdder, int rule) {
        return LangbookDatabase.addAgent(_db, targetBunch, sourceBunches, diffBunches, startMatcher, startAdder, endMatcher, endAdder, rule);
    }

    @Override
    public void removeAgent(int agentId) {
        LangbookDatabase.removeAgent(_db, agentId);
    }

    @Override
    public boolean addAcceptationInBunch(int bunch, int acceptation) {
        return LangbookDatabase.addAcceptationInBunch(_db, bunch, acceptation);
    }

    @Override
    public boolean removeAcceptationFromBunch(int bunch, int acceptation) {
        return LangbookDatabase.removeAcceptationFromBunch(_db, bunch, acceptation);
    }

    @Override
    public LanguageCreationResult addLanguage(String code) {
        return LangbookDatabase.addLanguage(_db, code);
    }

    @Override
    public boolean removeLanguage(int language) {
        return LangbookDatabase.removeLanguage(_db, language);
    }

    @Override
    public boolean addAlphabetCopyingFromOther(int alphabet, int sourceAlphabet) {
        return LangbookDatabase.addAlphabetCopyingFromOther(_db, alphabet, sourceAlphabet);
    }

    @Override
    public boolean addAlphabetAsConversionTarget(Conversion conversion) {
        return LangbookDatabase.addAlphabetAsConversionTarget(_db, conversion);
    }

    @Override
    public boolean removeAlphabet(int alphabet) {
        return LangbookDatabase.removeAlphabet(_db, alphabet);
    }

    @Override
    public Integer addAcceptation(int concept, ImmutableList<ImmutableIntKeyMap<String>> correlationArray) {
        return LangbookDatabase.addAcceptation(_db, concept, correlationArray);
    }

    @Override
    public boolean updateAcceptationCorrelationArray(int acceptation, ImmutableList<ImmutableIntKeyMap<String>> newCorrelationArray) {
        return LangbookDatabase.updateAcceptationCorrelationArray(_db, acceptation, newCorrelationArray);
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
        LangbookDatabase.duplicateAcceptationWithThisConcept(_db, linkedAcceptation, concept);
    }

    @Override
    public boolean replaceConversion(Conversion conversion) {
        return LangbookDatabase.replaceConversion(_db, conversion);
    }

    @Override
    public Integer obtainQuiz(int bunch, ImmutableList<QuestionFieldDetails> fields) {
        return LangbookDatabase.obtainQuiz(_db, bunch, fields);
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
}
