package sword.langbook3.android.db;

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
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

abstract class LangbookDatabaseChecker<LanguageId extends LanguageIdInterface, AlphabetId extends AlphabetIdInterface, SymbolArrayId extends SymbolArrayIdInterface, CorrelationId extends CorrelationIdInterface, CorrelationArrayId extends CorrelationArrayIdInterface, AcceptationId extends AcceptationIdInterface> implements LangbookChecker<LanguageId, AlphabetId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId> {

    abstract IntSetter<LanguageId> getLanguageIdSetter();
    abstract IntSetter<AlphabetId> getAlphabetIdSetter();
    abstract IntSetter<CorrelationId> getCorrelationIdSetter();
    abstract IntSetter<CorrelationArrayId> getCorrelationArrayIdSetter();
    abstract IntSetter<AcceptationId> getAcceptationIdSetter();
    abstract DbExporter.Database getDatabase();

    @Override
    public LanguageId findLanguageByCode(String code) {
        return LangbookReadableDatabase.findLanguageByCode(getDatabase(), getLanguageIdSetter(), code);
    }

    @Override
    public AlphabetId findMainAlphabetForLanguage(LanguageId language) {
        return LangbookReadableDatabase.findMainAlphabetForLanguage(getDatabase(), getAlphabetIdSetter(), language);
    }

    @Override
    public ImmutableSet<AlphabetId> findAlphabetsByLanguage(LanguageId language) {
        return LangbookReadableDatabase.findAlphabetsByLanguage(getDatabase(), getAlphabetIdSetter(), language);
    }

    @Override
    public ImmutableCorrelation<AlphabetId> getAcceptationTexts(AcceptationId acceptation) {
        return LangbookReadableDatabase.getAcceptationTexts(getDatabase(), getAlphabetIdSetter(), acceptation);
    }

    @Override
    public Conversion<AlphabetId> getConversion(ImmutablePair<AlphabetId, AlphabetId> pair) {
        return LangbookReadableDatabase.getConversion(getDatabase(), pair);
    }

    @Override
    public int getMaxConcept() {
        return LangbookReadableDatabase.getMaxConcept(getDatabase());
    }

    @Override
    public ImmutableMap<AlphabetId, AlphabetId> getConversionsMap() {
        return LangbookReadableDatabase.getConversionsMap(getDatabase(), getAlphabetIdSetter());
    }

    @Override
    public ImmutableList<CorrelationId> getAcceptationCorrelationArray(AcceptationId acceptation) {
        return LangbookReadableDatabase.getAcceptationCorrelations(getDatabase(), getAlphabetIdSetter(), getCorrelationIdSetter(), acceptation).left;
    }

    @Override
    public ImmutableSet<AcceptationId> findAcceptationsByConcept(int concept) {
        return LangbookReadableDatabase.findAcceptationsByConcept(getDatabase(), getAcceptationIdSetter(), concept);
    }

    @Override
    public ImmutableSet<AcceptationId> getAcceptationsInBunch(int bunch) {
        return LangbookReadableDatabase.getAcceptationsInBunch(getDatabase(), getAcceptationIdSetter(), bunch);
    }

    @Override
    public ImmutableIntSet findBunchesWhereAcceptationIsIncluded(AcceptationId acceptation) {
        return LangbookReadableDatabase.findBunchesWhereAcceptationIsIncluded(getDatabase(), acceptation);
    }

    @Override
    public ImmutableIntKeyMap<String> readAllMatchingBunches(ImmutableCorrelation<AlphabetId> texts, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readAllMatchingBunches(getDatabase(), getAlphabetIdSetter(), getCorrelationIdSetter(), texts, preferredAlphabet);
    }

    @Override
    public MutableCorrelation<AlphabetId> readCorrelationArrayTexts(CorrelationArrayId correlationArrayId) {
        return LangbookReadableDatabase.readCorrelationArrayTexts(getDatabase(), getAlphabetIdSetter(), getCorrelationIdSetter(), correlationArrayId);
    }

    @Override
    public DefinitionDetails getDefinition(int concept) {
        return LangbookReadableDatabase.getDefinition(getDatabase(), concept);
    }

    @Override
    public ImmutableSet<String> findConversionConflictWords(ConversionProposal<AlphabetId> newConversion) {
        return LangbookReadableDatabase.findConversionConflictWords(getDatabase(), newConversion);
    }

    @Override
    public ImmutableIntKeyMap<ImmutableSet<QuestionFieldDetails<AlphabetId>>> readQuizSelectorEntriesForBunch(int bunch) {
        return LangbookReadableDatabase.readQuizSelectorEntriesForBunch(getDatabase(), getAlphabetIdSetter(), bunch);
    }

    @Override
    public Progress readQuizProgress(int quizId) {
        return LangbookReadableDatabase.readQuizProgress(getDatabase(), quizId);
    }

    @Override
    public ImmutableIntKeyMap<String> readAllRules(AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readAllRules(getDatabase(), preferredAlphabet);
    }

    @Override
    public boolean isSymbolArrayMerelyASentence(SymbolArrayId symbolArrayId) {
        return LangbookReadableDatabase.isSymbolArrayMerelyASentence(getDatabase(), symbolArrayId);
    }

    @Override
    public int conceptFromAcceptation(AcceptationId acceptationId) {
        return LangbookReadableDatabase.conceptFromAcceptation(getDatabase(), acceptationId);
    }

    @Override
    public boolean isAlphabetPresent(AlphabetId alphabet) {
        return LangbookReadableDatabase.isAlphabetPresent(getDatabase(), alphabet);
    }

    @Override
    public LanguageId getLanguageFromAlphabet(AlphabetId alphabet) {
        return LangbookReadableDatabase.getLanguageFromAlphabet(getDatabase(), getLanguageIdSetter(), alphabet);
    }

    @Override
    public ImmutableMap<AlphabetId, String> readAllAlphabets(AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readAllAlphabets(getDatabase(), getAlphabetIdSetter(), preferredAlphabet);
    }

    @Override
    public AcceptationDetailsModel<LanguageId, AlphabetId, CorrelationId, AcceptationId> getAcceptationsDetails(AcceptationId staticAcceptation, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.getAcceptationsDetails(getDatabase(), getLanguageIdSetter(), getAlphabetIdSetter(), getCorrelationIdSetter(), getAcceptationIdSetter(), staticAcceptation, preferredAlphabet);
    }

    @Override
    public ImmutableList<SearchResult<AcceptationId>> getSearchHistory() {
        return LangbookReadableDatabase.getSearchHistory(getDatabase(), getAcceptationIdSetter());
    }

    @Override
    public QuizDetails<AlphabetId> getQuizDetails(int quizId) {
        return LangbookReadableDatabase.getQuizDetails(getDatabase(), getAlphabetIdSetter(), quizId);
    }

    @Override
    public String readQuestionFieldText(AcceptationId acceptation, QuestionFieldDetails<AlphabetId> field) {
        return LangbookReadableDatabase.readQuestionFieldText(getDatabase(), acceptation, field);
    }

    @Override
    public ImmutableIntValueMap<AcceptationId> getCurrentKnowledge(int quizId) {
        return LangbookReadableDatabase.getCurrentKnowledge(getDatabase(), getAcceptationIdSetter(), quizId);
    }

    @Override
    public ImmutableIntSet getAgentIds() {
        return LangbookReadableDatabase.getAgentIds(getDatabase());
    }

    @Override
    public ImmutableList<SearchResult<AcceptationId>> findAcceptationFromText(String queryText, int restrictionStringType, ImmutableIntRange range) {
        return LangbookReadableDatabase.findAcceptationFromText(getDatabase(), getAcceptationIdSetter(), queryText, restrictionStringType, range);
    }

    @Override
    public AgentRegister<CorrelationId> getAgentRegister(int agentId) {
        return LangbookReadableDatabase.getAgentRegister(getDatabase(), getCorrelationIdSetter(), agentId);
    }

    @Override
    public AgentDetails<AlphabetId> getAgentDetails(int agentId) {
        return LangbookReadableDatabase.getAgentDetails(getDatabase(), getAlphabetIdSetter(), getCorrelationIdSetter(), agentId);
    }

    @Override
    public ImmutableList<DisplayableItem<AcceptationId>> readBunchSetAcceptationsAndTexts(int bunchSet, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readBunchSetAcceptationsAndTexts(getDatabase(), getAcceptationIdSetter(), bunchSet, preferredAlphabet);
    }

    @Override
    public ImmutableList<SearchResult<AcceptationId>> findAcceptationAndRulesFromText(String queryText, int restrictionStringType, ImmutableIntRange range) {
        return LangbookReadableDatabase.findAcceptationAndRulesFromText(getDatabase(), getAcceptationIdSetter(), queryText, restrictionStringType, range);
    }

    @Override
    public ImmutableMap<TableCellReference, TableCellValue> readTableContent(int dynamicAcceptation, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readTableContent(getDatabase(), dynamicAcceptation, preferredAlphabet);
    }

    @Override
    public AcceptationId getStaticAcceptationFromDynamic(AcceptationId dynamicAcceptation) {
        return LangbookReadableDatabase.getStaticAcceptationFromDynamic(getDatabase(), getAcceptationIdSetter(), dynamicAcceptation);
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
    public AcceptationId findRuledAcceptationByAgentAndBaseAcceptation(int agentId, AcceptationId baseAcceptation) {
        return LangbookReadableDatabase.findRuledAcceptationByAgentAndBaseAcceptation(getDatabase(), getAcceptationIdSetter(), agentId, baseAcceptation);
    }

    @Override
    public String readAcceptationMainText(AcceptationId acceptation) {
        return LangbookReadableDatabase.readAcceptationMainText(getDatabase(), acceptation);
    }

    @Override
    public ImmutableIntSet findAllAgentsThatIncludedAcceptationInBunch(int bunch, AcceptationId acceptation) {
        return LangbookReadableDatabase.findAllAgentsThatIncludedAcceptationInBunch(getDatabase(), bunch, acceptation);
    }

    @Override
    public ImmutableMap<AcceptationId, AcceptationId> getAgentProcessedMap(int agentId) {
        return LangbookReadableDatabase.getAgentProcessedMap(getDatabase(), getAcceptationIdSetter(), agentId);
    }

    @Override
    public MorphologyReaderResult<AcceptationId> readMorphologiesFromAcceptation(AcceptationId acceptation, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readMorphologiesFromAcceptation(getDatabase(), getAcceptationIdSetter(), acceptation, preferredAlphabet);
    }

    @Override
    public ImmutableSet<AcceptationId> getAcceptationsInBunchByBunchAndAgent(int bunch, int agent) {
        return LangbookReadableDatabase.getAcceptationsInBunchByBunchAndAgent(getDatabase(), getAcceptationIdSetter(), bunch, agent);
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
    public ImmutableSet<SentenceSpan<AcceptationId>> getSentenceSpans(int symbolArray) {
        return LangbookReadableDatabase.getSentenceSpans(getDatabase(), getAcceptationIdSetter(), symbolArray);
    }

    @Override
    public ImmutableMap<LanguageId, String> readAllLanguages(AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readAllLanguages(getDatabase(), getLanguageIdSetter(), preferredAlphabet);
    }

    @Override
    public ImmutableCorrelation<AlphabetId> getCorrelationWithText(CorrelationId correlationId) {
        return LangbookReadableDatabase.getCorrelationWithText(getDatabase(), getAlphabetIdSetter(), correlationId);
    }

    @Override
    public DisplayableItem<AcceptationId> readConceptAcceptationAndText(int concept, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readConceptAcceptationAndText(getDatabase(), getAcceptationIdSetter(), concept, preferredAlphabet);
    }

    @Override
    public String readConceptText(int concept, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readConceptText(getDatabase(), concept, preferredAlphabet);
    }

    @Override
    public ImmutableMap<AlphabetId, String> readAlphabetsForLanguage(LanguageId language, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.readAlphabetsForLanguage(getDatabase(), getAlphabetIdSetter(), language, preferredAlphabet);
    }

    @Override
    public boolean checkAlphabetCanBeRemoved(AlphabetId alphabet) {
        return LangbookReadableDatabase.checkAlphabetCanBeRemoved(getDatabase(), getAlphabetIdSetter(), alphabet);
    }

    @Override
    public CorrelationDetailsModel<AlphabetId, CorrelationId, AcceptationId> getCorrelationDetails(CorrelationId correlationId, AlphabetId preferredAlphabet) {
        return LangbookReadableDatabase.getCorrelationDetails(getDatabase(), getAlphabetIdSetter(), getCorrelationIdSetter(), getAcceptationIdSetter(), correlationId, preferredAlphabet);
    }

    @Override
    public CorrelationId findCorrelation(Correlation<AlphabetId> correlation) {
        return LangbookReadableDatabase.findCorrelation(getDatabase(), getAlphabetIdSetter(), getCorrelationIdSetter(), correlation);
    }

    @Override
    public boolean isAnyLanguagePresent() {
        return LangbookReadableDatabase.isAnyLanguagePresent(getDatabase());
    }

    @Override
    public ImmutablePair<ImmutableCorrelation<AlphabetId>, LanguageId> readAcceptationTextsAndLanguage(AcceptationId acceptation) {
        return LangbookReadableDatabase.readAcceptationTextsAndLanguage(getDatabase(), getLanguageIdSetter(), getAlphabetIdSetter(), acceptation);
    }

    @Override
    public ImmutableMap<AlphabetId, AlphabetId> findConversions(Set<AlphabetId> alphabets) {
        return LangbookReadableDatabase.findConversions(getDatabase(), getAlphabetIdSetter(), alphabets);
    }

    @Override
    public ImmutableMap<String, AcceptationId> readTextAndDynamicAcceptationsMapFromAcceptation(AcceptationId staticAcceptation) {
        return LangbookReadableDatabase.readTextAndDynamicAcceptationsMapFromAcceptation(getDatabase(), getAcceptationIdSetter(), staticAcceptation);
    }

    public ImmutableIntKeyMap<String> getSampleSentences(AcceptationId staticAcceptation) {
        return LangbookReadableDatabase.getSampleSentences(getDatabase(), getAcceptationIdSetter(), staticAcceptation);
    }

    @Override
    public SentenceDetailsModel<AcceptationId> getSentenceDetails(int sentenceId) {
        return LangbookReadableDatabase.getSentenceDetails(getDatabase(), getAcceptationIdSetter(), sentenceId);
    }
}
