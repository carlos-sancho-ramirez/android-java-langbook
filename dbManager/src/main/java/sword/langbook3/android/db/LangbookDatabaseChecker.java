package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.Map;
import sword.collections.MutableMap;
import sword.collections.Set;
import sword.database.DbExporter;
import sword.langbook3.android.models.AcceptationDetailsModel;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.ConversionProposal;
import sword.langbook3.android.models.CorrelationDetailsModel;
import sword.langbook3.android.models.DefinitionDetails;
import sword.langbook3.android.models.DisplayableItem;
import sword.langbook3.android.models.MorphologyReaderResult;
import sword.langbook3.android.models.Progress;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.QuizDetails;
import sword.langbook3.android.models.SearchResult;
import sword.langbook3.android.models.SentenceDetailsModel;
import sword.langbook3.android.models.SentenceSpan;
import sword.langbook3.android.models.TableCellReference;
import sword.langbook3.android.models.TableCellValue;

abstract class LangbookDatabaseChecker implements LangbookChecker {

    abstract DbExporter.Database getDatabase();

    @Override
    public Integer findLanguageByCode(String code) {
        return LangbookReadableDatabase.findLanguageByCode(getDatabase(), code);
    }

    @Override
    public AlphabetId findMainAlphabetForLanguage(int language) {
        return new AlphabetId(LangbookReadableDatabase.findMainAlphabetForLanguage(getDatabase(), language));
    }

    @Override
    public ImmutableSet<AlphabetId> findAlphabetsByLanguage(int language) {
        return LangbookReadableDatabase.findAlphabetsByLanguage(getDatabase(), language);
    }

    @Override
    public ImmutableMap<AlphabetId, String> getAcceptationTexts(int acceptation) {
        return LangbookReadableDatabase.getAcceptationTexts(getDatabase(), acceptation);
    }

    @Override
    public Conversion getConversion(ImmutablePair<AlphabetId, AlphabetId> pair) {
        return LangbookReadableDatabase.getConversion(getDatabase(), pair);
    }

    @Override
    public int getMaxConcept() {
        return LangbookReadableDatabase.getMaxConcept(getDatabase());
    }

    @Override
    public ImmutableMap<AlphabetId, AlphabetId> getConversionsMap() {
        return LangbookReadableDatabase.getConversionsMap(getDatabase());
    }

    @Override
    public ImmutableIntList getAcceptationCorrelationArray(int acceptation) {
        return LangbookReadableDatabase.getAcceptationCorrelations(getDatabase(), acceptation).left;
    }

    @Override
    public ImmutableIntSet findAcceptationsByConcept(int concept) {
        return LangbookReadableDatabase.findAcceptationsByConcept(getDatabase(), concept);
    }

    @Override
    public ImmutableIntSet getAcceptationsInBunch(int bunch) {
        return LangbookReadableDatabase.getAcceptationsInBunch(getDatabase(), bunch);
    }

    @Override
    public ImmutableIntSet findBunchesWhereAcceptationIsIncluded(int acceptation) {
        return LangbookReadableDatabase.findBunchesWhereAcceptationIsIncluded(getDatabase(), acceptation);
    }

    @Override
    public ImmutableIntKeyMap<String> readAllMatchingBunches(ImmutableMap<AlphabetId, String> texts, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readAllMatchingBunches(getDatabase(), texts, preferredAlphabet);
    }

    @Override
    public MutableMap<AlphabetId, String> readCorrelationArrayTexts(int correlationArrayId) {
        return LangbookReadableDatabase.readCorrelationArrayTexts(getDatabase(), correlationArrayId);
    }

    @Override
    public DefinitionDetails getDefinition(int concept) {
        return LangbookReadableDatabase.getDefinition(getDatabase(), concept);
    }

    @Override
    public ImmutableSet<String> findConversionConflictWords(ConversionProposal newConversion) {
        return LangbookReadableDatabase.findConversionConflictWords(getDatabase(), newConversion);
    }

    @Override
    public ImmutableIntKeyMap<ImmutableSet<QuestionFieldDetails>> readQuizSelectorEntriesForBunch(int bunch) {
        return LangbookReadableDatabase.readQuizSelectorEntriesForBunch(getDatabase(), bunch);
    }

    @Override
    public Progress readQuizProgress(int quizId) {
        return LangbookReadableDatabase.readQuizProgress(getDatabase(), quizId);
    }

    @Override
    public ImmutableIntKeyMap<String> readAllRules(AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readAllRules(getDatabase(), preferredAlphabet.key);
    }

    @Override
    public boolean isSymbolArrayMerelyASentence(int symbolArrayId) {
        return LangbookReadableDatabase.isSymbolArrayMerelyASentence(getDatabase(), symbolArrayId);
    }

    @Override
    public int conceptFromAcceptation(int acceptationId) {
        return LangbookReadableDatabase.conceptFromAcceptation(getDatabase(), acceptationId);
    }

    @Override
    public boolean isAlphabetPresent(AlphabetId alphabet) {
        return LangbookReadableDatabase.isAlphabetPresent(getDatabase(), alphabet);
    }

    @Override
    public Integer getLanguageFromAlphabet(AlphabetId alphabet) {
        return LangbookReadableDatabase.getLanguageFromAlphabet(getDatabase(), alphabet);
    }

    @Override
    public ImmutableMap<AlphabetId, String> readAllAlphabets(AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readAllAlphabets(getDatabase(), preferredAlphabet);
    }

    @Override
    public AcceptationDetailsModel getAcceptationsDetails(int staticAcceptation, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.getAcceptationsDetails(getDatabase(), staticAcceptation, preferredAlphabet);
    }

    @Override
    public ImmutableList<SearchResult> getSearchHistory() {
        return LangbookReadableDatabase.getSearchHistory(getDatabase());
    }

    @Override
    public QuizDetails getQuizDetails(int quizId) {
        return LangbookReadableDatabase.getQuizDetails(getDatabase(), quizId);
    }

    @Override
    public String readQuestionFieldText(int acceptation, QuestionFieldDetails field) {
        return LangbookReadableDatabase.readQuestionFieldText(getDatabase(), acceptation, field);
    }

    @Override
    public ImmutableIntPairMap getCurrentKnowledge(int quizId) {
        return LangbookReadableDatabase.getCurrentKnowledge(getDatabase(), quizId);
    }

    @Override
    public ImmutableIntSet getAgentIds() {
        return LangbookReadableDatabase.getAgentIds(getDatabase());
    }

    @Override
    public ImmutableList<SearchResult> findAcceptationFromText(String queryText, int restrictionStringType, ImmutableIntRange range) {
        return LangbookReadableDatabase.findAcceptationFromText(getDatabase(), queryText, restrictionStringType, range);
    }

    @Override
    public AgentRegister getAgentRegister(int agentId) {
        return LangbookReadableDatabase.getAgentRegister(getDatabase(), agentId);
    }

    @Override
    public AgentDetails getAgentDetails(int agentId) {
        return LangbookReadableDatabase.getAgentDetails(getDatabase(), agentId);
    }

    @Override
    public ImmutableList<DisplayableItem> readBunchSetAcceptationsAndTexts(int bunchSet, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readBunchSetAcceptationsAndTexts(getDatabase(), bunchSet, preferredAlphabet.key);
    }

    @Override
    public ImmutableList<SearchResult> findAcceptationAndRulesFromText(String queryText, int restrictionStringType, ImmutableIntRange range) {
        return LangbookReadableDatabase.findAcceptationAndRulesFromText(getDatabase(), queryText, restrictionStringType, range);
    }

    @Override
    public ImmutableMap<TableCellReference, TableCellValue> readTableContent(int dynamicAcceptation, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readTableContent(getDatabase(), dynamicAcceptation, preferredAlphabet.key);
    }

    @Override
    public Integer getStaticAcceptationFromDynamic(int dynamicAcceptation) {
        return LangbookReadableDatabase.getStaticAcceptationFromDynamic(getDatabase(), dynamicAcceptation);
    }

    @Override
    public Integer findRuledConcept(int rule, int concept) {
        return LangbookReadableDatabase.findRuledConcept(getDatabase(), rule, concept);
    }

    @Override
    public ImmutableIntPairMap findRuledConceptsByRule(int rule) {
        return LangbookReadableDatabase.findRuledConceptsByRule(getDatabase(), rule);
    }

    @Override
    public Integer findRuledAcceptationByAgentAndBaseAcceptation(int agentId, int baseAcceptation) {
        return LangbookReadableDatabase.findRuledAcceptationByAgentAndBaseAcceptation(getDatabase(), agentId, baseAcceptation);
    }

    @Override
    public String readAcceptationMainText(int acceptation) {
        return LangbookReadableDatabase.readAcceptationMainText(getDatabase(), acceptation);
    }

    @Override
    public ImmutableIntSet findAllAgentsThatIncludedAcceptationInBunch(int bunch, int acceptation) {
        return LangbookReadableDatabase.findAllAgentsThatIncludedAcceptationInBunch(getDatabase(), bunch, acceptation);
    }

    @Override
    public ImmutableIntPairMap getAgentProcessedMap(int agentId) {
        return LangbookReadableDatabase.getAgentProcessedMap(getDatabase(), agentId);
    }

    @Override
    public MorphologyReaderResult readMorphologiesFromAcceptation(int acceptation, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readMorphologiesFromAcceptation(getDatabase(), acceptation, preferredAlphabet);
    }

    @Override
    public ImmutableIntSet getAcceptationsInBunchByBunchAndAgent(int bunch, int agent) {
        return LangbookReadableDatabase.getAcceptationsInBunchByBunchAndAgent(getDatabase(), bunch, agent);
    }

    @Override
    public ImmutableIntSet getBunchSet(int setId) {
        return LangbookReadableDatabase.getBunchSet(getDatabase(), setId);
    }

    @Override
    public String getSentenceText(int sentenceId) {
        return LangbookReadableDatabase.getSentenceText(getDatabase(), sentenceId);
    }

    @Override
    public ImmutableSet<SentenceSpan> getSentenceSpans(int symbolArray) {
        return LangbookReadableDatabase.getSentenceSpans(getDatabase(), symbolArray);
    }

    @Override
    public ImmutableIntKeyMap<String> readAllLanguages(AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readAllLanguages(getDatabase(), preferredAlphabet);
    }

    @Override
    public ImmutableMap<AlphabetId, String> getCorrelationWithText(int correlationId) {
        return LangbookReadableDatabase.getCorrelationWithText(getDatabase(), correlationId);
    }

    @Override
    public DisplayableItem readConceptAcceptationAndText(int concept, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readConceptAcceptationAndText(getDatabase(), concept, preferredAlphabet);
    }

    @Override
    public String readConceptText(int concept, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readConceptText(getDatabase(), concept, preferredAlphabet);
    }

    @Override
    public ImmutableMap<AlphabetId, String> readAlphabetsForLanguage(int language, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readAlphabetsForLanguage(getDatabase(), language, preferredAlphabet);
    }

    @Override
    public boolean checkAlphabetCanBeRemoved(AlphabetId alphabet) {
        return LangbookReadableDatabase.checkAlphabetCanBeRemoved(getDatabase(), alphabet);
    }

    @Override
    public CorrelationDetailsModel getCorrelationDetails(int correlationId, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.getCorrelationDetails(getDatabase(), correlationId, preferredAlphabet);
    }

    @Override
    public Integer findCorrelation(Map<AlphabetId, String> correlation) {
        return LangbookReadableDatabase.findCorrelation(getDatabase(), correlation);
    }

    @Override
    public boolean isAnyLanguagePresent() {
        return LangbookReadableDatabase.isAnyLanguagePresent(getDatabase());
    }

    @Override
    public ImmutablePair<ImmutableMap<AlphabetId, String>, Integer> readAcceptationTextsAndLanguage(int acceptation) {
        return LangbookReadableDatabase.readAcceptationTextsAndLanguage(getDatabase(), acceptation);
    }

    @Override
    public ImmutableMap<AlphabetId, AlphabetId> findConversions(Set<AlphabetId> alphabets) {
        return LangbookReadableDatabase.findConversions(getDatabase(), alphabets);
    }

    @Override
    public ImmutableIntValueMap<String> readTextAndDynamicAcceptationsMapFromAcceptation(int staticAcceptation) {
        return LangbookReadableDatabase.readTextAndDynamicAcceptationsMapFromAcceptation(getDatabase(), staticAcceptation);
    }

    public ImmutableIntKeyMap<String> getSampleSentences(int staticAcceptation) {
        return LangbookReadableDatabase.getSampleSentences(getDatabase(), staticAcceptation);
    }

    @Override
    public SentenceDetailsModel getSentenceDetails(int sentenceId) {
        return LangbookReadableDatabase.getSentenceDetails(getDatabase(), sentenceId);
    }
}
