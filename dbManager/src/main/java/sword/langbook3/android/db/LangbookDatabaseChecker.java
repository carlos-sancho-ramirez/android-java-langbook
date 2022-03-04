package sword.langbook3.android.db;

import sword.collections.Function;
import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableIntValueHashMap;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutablePair;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.List;
import sword.collections.Map;
import sword.collections.MutableHashMap;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntSet;
import sword.collections.MutableIntValueHashMap;
import sword.collections.MutableIntValueMap;
import sword.collections.MutableList;
import sword.collections.MutableMap;
import sword.collections.MutableSet;
import sword.collections.Set;
import sword.collections.SortUtils;
import sword.database.Database;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbStringValue;
import sword.database.DbTable;
import sword.database.DbValue;
import sword.langbook3.android.collections.StringUtils;
import sword.langbook3.android.collections.SyncCacheMap;
import sword.langbook3.android.db.LangbookDbSchema.Tables;
import sword.langbook3.android.models.AcceptationDetailsModel;
import sword.langbook3.android.models.AgentDetails;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.CharacterCompositionEditorModel;
import sword.langbook3.android.models.CharacterCompositionPart;
import sword.langbook3.android.models.CharacterCompositionRepresentation;
import sword.langbook3.android.models.CharacterDetailsModel;
import sword.langbook3.android.models.CharacterPickerItem;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.models.ConversionProposal;
import sword.langbook3.android.models.CorrelationDetailsModel;
import sword.langbook3.android.models.DefinitionDetails;
import sword.langbook3.android.models.DerivedAcceptationsReaderResult;
import sword.langbook3.android.models.DisplayableItem;
import sword.langbook3.android.models.DynamizableResult;
import sword.langbook3.android.models.IdTextPairResult;
import sword.langbook3.android.models.IdentifiableResult;
import sword.langbook3.android.models.MorphologyReaderResult;
import sword.langbook3.android.models.MorphologyResult;
import sword.langbook3.android.models.Progress;
import sword.langbook3.android.models.QuestionFieldDetails;
import sword.langbook3.android.models.QuizDetails;
import sword.langbook3.android.models.RuledAcceptationMutableRegister;
import sword.langbook3.android.models.SearchResult;
import sword.langbook3.android.models.SentenceDetailsModel;
import sword.langbook3.android.models.SentenceSpan;
import sword.langbook3.android.models.SynonymTranslationResult;

import static sword.collections.SortUtils.equal;
import static sword.langbook3.android.db.LangbookDbSchema.EMPTY_CORRELATION_ARRAY_ID;
import static sword.langbook3.android.db.LangbookDbSchema.EMPTY_CORRELATION_ID;
import static sword.langbook3.android.db.LangbookDbSchema.MAX_ALLOWED_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.MIN_ALLOWED_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.NO_SCORE;
import static sword.langbook3.android.db.LangbookDbSchema.Tables.alphabets;
import static sword.langbook3.android.models.CharacterCompositionRepresentation.INVALID_CHARACTER;
import static sword.langbook3.android.models.CharacterDetailsModel.UNKNOWN_COMPOSITION_TYPE;

abstract class LangbookDatabaseChecker<ConceptId extends ConceptIdInterface, LanguageId extends LanguageIdInterface<ConceptId>, AlphabetId extends AlphabetIdInterface<ConceptId>, CharacterId extends CharacterIdInterface, SymbolArrayId extends SymbolArrayIdInterface, CorrelationId extends CorrelationIdInterface, CorrelationArrayId extends CorrelationArrayIdInterface, AcceptationId extends AcceptationIdInterface, BunchId extends BunchIdInterface<ConceptId>, BunchSetId extends BunchSetIdInterface, RuleId extends RuleIdInterface<ConceptId>, AgentId extends AgentIdInterface, QuizId extends QuizIdInterface, SentenceId extends SentenceIdInterface> implements LangbookChecker<ConceptId, LanguageId, AlphabetId, CharacterId, SymbolArrayId, CorrelationId, CorrelationArrayId, AcceptationId, BunchId, BunchSetId, RuleId, AgentId, QuizId, SentenceId> {

    final Database _db;
    final ConceptSetter<ConceptId> _conceptIdSetter;
    final ConceptualizableSetter<ConceptId, LanguageId> _languageIdSetter;
    final ConceptualizableSetter<ConceptId, AlphabetId> _alphabetIdSetter;
    final IntSetter<CharacterId> _characterIdSetter;
    final IntSetter<SymbolArrayId> _symbolArrayIdSetter;
    final IntSetter<CorrelationId> _correlationIdSetter;
    final IntSetter<CorrelationArrayId> _correlationArrayIdSetter;
    final IntSetter<AcceptationId> _acceptationIdSetter;
    final ConceptualizableSetter<ConceptId, BunchId> _bunchIdSetter;
    final BunchSetIntSetter<BunchSetId> _bunchSetIdSetter;
    final ConceptualizableSetter<ConceptId, RuleId> _ruleIdSetter;
    final IntSetter<AgentId> _agentIdSetter;
    final IntSetter<QuizId> _quizIdSetter;
    final IntSetter<SentenceId> _sentenceIdSetter;

    LangbookDatabaseChecker(Database db, ConceptSetter<ConceptId> conceptIdManager, ConceptualizableSetter<ConceptId, LanguageId> languageIdManager, ConceptualizableSetter<ConceptId, AlphabetId> alphabetIdManager, IntSetter<CharacterId> characterIdManager, IntSetter<SymbolArrayId> symbolArrayIdManager, IntSetter<CorrelationId> correlationIdSetter, IntSetter<CorrelationArrayId> correlationArrayIdSetter, IntSetter<AcceptationId> acceptationIdSetter, ConceptualizableSetter<ConceptId, BunchId> bunchIdSetter, BunchSetIntSetter<BunchSetId> bunchSetIdSetter, ConceptualizableSetter<ConceptId, RuleId> ruleIdSetter, IntSetter<AgentId> agentIdSetter, IntSetter<QuizId> quizIdSetter, IntSetter<SentenceId> sentenceIdSetter) {
        if (db == null || conceptIdManager == null || languageIdManager == null || alphabetIdManager == null || characterIdManager == null || symbolArrayIdManager == null || correlationIdSetter == null || correlationArrayIdSetter == null || acceptationIdSetter == null || bunchIdSetter == null || bunchSetIdSetter == null || ruleIdSetter == null || agentIdSetter == null || quizIdSetter == null || sentenceIdSetter == null) {
            throw new IllegalArgumentException();
        }

        _db = db;
        _conceptIdSetter = conceptIdManager;
        _languageIdSetter = languageIdManager;
        _alphabetIdSetter = alphabetIdManager;
        _characterIdSetter = characterIdManager;
        _symbolArrayIdSetter = symbolArrayIdManager;
        _correlationIdSetter = correlationIdSetter;
        _correlationArrayIdSetter = correlationArrayIdSetter;
        _acceptationIdSetter = acceptationIdSetter;
        _bunchIdSetter = bunchIdSetter;
        _bunchSetIdSetter = bunchSetIdSetter;
        _ruleIdSetter = ruleIdSetter;
        _agentIdSetter = agentIdSetter;
        _quizIdSetter = quizIdSetter;
        _sentenceIdSetter = sentenceIdSetter;
    }

    private List<DbValue> selectSingleRow(DbQuery query) {
        try (DbResult result = _db.select(query)) {
            if (!result.hasNext()) {
                throw new AssertionError("Nothing found matching the given criteria");
            }

            final List<DbValue> row = result.next();
            if (result.hasNext()) {
                throw new AssertionError("Multiple rows found matching the given criteria");
            }

            return row;
        }
    }

    private List<DbValue> selectOptionalSingleRow(DbQuery query) {
        try (DbResult result = _db.select(query)) {
            return result.hasNext()? result.next() : null;
        }
    }

    private List<DbValue> selectFirstRow(DbQuery query) {
        try (DbResult result = _db.select(query)) {
            if (!result.hasNext()) {
                throw new AssertionError("Nothing found matching the given criteria");
            }

            return result.next();
        }
    }

    private String selectOptionalFirstTextColumn(DbQuery query) {
        String result = null;
        try (DbResult dbResult = _db.select(query)) {
            if (dbResult.hasNext()) {
                result = dbResult.next().get(0).toText();
            }

            if (dbResult.hasNext()) {
                throw new AssertionError("Only 0 or 1 row was expected");
            }
        }

        return result;
    }

    private DbValue selectFirstDbValue(DbQuery query) {
        DbValue result = null;
        try (DbResult dbResult = _db.select(query)) {
            if (dbResult.hasNext()) {
                result = dbResult.next().get(0);
            }

            if (dbResult.hasNext()) {
                throw new AssertionError("Expected to find just one value. But there were more");
            }
        }

        if (result == null) {
            throw new AssertionError("Expected to find one value. But was empty");
        }

        return result;
    }

    private DbValue selectOptionalFirstDbValue(DbQuery query) {
        DbValue result = null;
        try (DbResult dbResult = _db.select(query)) {
            if (dbResult.hasNext()) {
                result = dbResult.next().get(0);
            }

            if (dbResult.hasNext()) {
                throw new AssertionError("Only 0 or 1 row was expected");
            }
        }

        return result;
    }

    private boolean selectExistAtLeastOneRow(DbQuery query) {
        try (DbResult dbResult = _db.select(query)) {
            return dbResult.hasNext();
        }
    }

    private boolean selectExistingRow(DbQuery query) {
        boolean result = false;
        try (DbResult dbResult = _db.select(query)) {
            if (dbResult.hasNext()) {
                dbResult.next();
                result = true;
            }

            if (dbResult.hasNext()) {
                throw new AssertionError();
            }
        }

        return result;
    }

    @Override
    public LanguageId findLanguageByCode(String code) {
        final LangbookDbSchema.LanguagesTable table = Tables.languages;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getCodeColumnIndex(), code)
                .select(table.getIdColumnIndex());

        final DbValue value = selectOptionalFirstDbValue(query);
        return (value != null)? _languageIdSetter.getKeyFromDbValue(value) : null;
    }

    @Override
    public AlphabetId findMainAlphabetForLanguage(LanguageId language) {
        final LangbookDbSchema.LanguagesTable table = Tables.languages;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), language)
                .select(table.getMainAlphabetColumnIndex());

        final DbValue value = selectOptionalFirstDbValue(query);
        return (value != null)? _alphabetIdSetter.getKeyFromDbValue(value) : null;
    }

    @Override
    public ImmutableSet<AlphabetId> findAlphabetsByLanguage(LanguageId language) {
        final LangbookDbSchema.AlphabetsTable table = alphabets;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getLanguageColumnIndex(), language)
                .select(table.getIdColumnIndex());
        return _db.select(query).map(row -> _alphabetIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    static final class StringQueryAcceptationDetails<AlphabetId, AcceptationId> {
        final AcceptationId mainAcceptation;
        final String mainText;
        final ImmutableCorrelation<AlphabetId> texts;

        StringQueryAcceptationDetails(AcceptationId mainAcceptation, String mainText, ImmutableCorrelation<AlphabetId> texts) {
            this.mainAcceptation = mainAcceptation;
            this.mainText = mainText;
            this.texts = texts;
        }
    }

    StringQueryAcceptationDetails<AlphabetId, AcceptationId> getStringQueryAcceptationDetails(AcceptationId acceptation) {
        final LangbookDbSchema.StringQueriesTable table = Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(table.getMainAcceptationColumnIndex(), table.getStringAlphabetColumnIndex(), table.getStringColumnIndex(), table.getMainStringColumnIndex());

        AcceptationId mainAcceptation = null;
        String mainString = null;

        final ImmutableMap.Builder<AlphabetId, String> builder = new ImmutableHashMap.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                builder.put(_alphabetIdSetter.getKeyFromDbValue(row.get(1)), row.get(2).toText());

                if (mainAcceptation == null) {
                    mainAcceptation = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
                    mainString = row.get(3).toText();
                }
                else if (!mainAcceptation.sameValue(row.get(0)) || !mainString.equals(row.get(3).toText())) {
                    throw new AssertionError();
                }
            }
        }

        return new StringQueryAcceptationDetails<>(mainAcceptation, mainString, new ImmutableCorrelation<>(builder.build()));
    }

    @Override
    public ImmutableCorrelation<AlphabetId> getAcceptationTexts(AcceptationId acceptation) {
        final LangbookDbSchema.StringQueriesTable table = Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(table.getStringAlphabetColumnIndex(), table.getStringColumnIndex());

        final ImmutableMap.Builder<AlphabetId, String> builder = new ImmutableHashMap.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final AlphabetId alphabet = _alphabetIdSetter.getKeyFromDbValue(row.get(0));
                if (alphabet != null) {
                    builder.put(alphabet, row.get(1).toText());
                }
            }
        }

        return new ImmutableCorrelation<>(builder.build());
    }

    @Override
    public Conversion<AlphabetId> getConversion(ImmutablePair<AlphabetId, AlphabetId> pair) {
        final LangbookDbSchema.ConversionsTable conversions = Tables.conversions;
        final LangbookDbSchema.SymbolArraysTable symbols = Tables.symbolArrays;

        final int off1Symbols = conversions.columns().size();
        final int off2Symbols = off1Symbols + symbols.columns().size();

        final DbQuery query = new DbQueryBuilder(conversions)
                .join(symbols, conversions.getSourceColumnIndex(), symbols.getIdColumnIndex())
                .join(symbols, conversions.getTargetColumnIndex(), symbols.getIdColumnIndex())
                .where(conversions.getSourceAlphabetColumnIndex(), pair.left)
                .where(conversions.getTargetAlphabetColumnIndex(), pair.right)
                .select(
                        off1Symbols + symbols.getStrColumnIndex(),
                        off2Symbols + symbols.getStrColumnIndex());

        final MutableMap<String, String> resultMap = MutableHashMap.empty();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final String sourceText = row.get(0).toText();
                final String targetText = row.get(1).toText();
                resultMap.put(sourceText, targetText);
            }
        }

        return new Conversion<>(pair.left, pair.right, resultMap);
    }

    private int getColumnMax(DbTable table, int columnIndex) {
        final DbQuery query = new DbQuery.Builder(table)
                .select(DbQuery.max(columnIndex));

        try (DbResult result = _db.select(query)) {
            return result.hasNext()? result.next().get(0).toInt() : 0;
        }
    }

    private int getMaxConceptInAcceptations() {
        LangbookDbSchema.AcceptationsTable table = Tables.acceptations;
        return getColumnMax(table, table.getConceptColumnIndex());
    }

    private int getMaxConceptInRuledConcepts() {
        LangbookDbSchema.RuledConceptsTable table = Tables.ruledConcepts;
        return getColumnMax(table, table.getIdColumnIndex());
    }

    private int getMaxLanguage() {
        LangbookDbSchema.LanguagesTable table = Tables.languages;
        return getColumnMax(table, table.getIdColumnIndex());
    }

    private int getMaxAlphabet() {
        LangbookDbSchema.AlphabetsTable table = alphabets;
        return getColumnMax(table, table.getIdColumnIndex());
    }

    private int getMaxConceptInComplementedConcepts() {
        LangbookDbSchema.ComplementedConceptsTable table = Tables.complementedConcepts;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex(), table.getBaseColumnIndex(), table.getComplementColumnIndex());

        int max = 0;
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int id = row.get(0).toInt();
                final int base = row.get(1).toInt();
                final int complement = row.get(2).toInt();
                final int localMax = (id > base && id > complement)? id : (base > complement)? base : complement;
                if (localMax > max) {
                    max = localMax;
                }
            }
        }

        return max;
    }

    private int getMaxConceptInConceptCompositions() {
        LangbookDbSchema.ConceptCompositionsTable table = Tables.conceptCompositions;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getComposedColumnIndex(), table.getItemColumnIndex());

        int max = 0;
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int compositionId = row.get(0).toInt();
                final int item = row.get(1).toInt();
                final int localMax = (item > compositionId)? item : compositionId;
                if (localMax > max) {
                    max = localMax;
                }
            }
        }

        return max;
    }

    private int getMaxConceptInSentences() {
        LangbookDbSchema.SentencesTable table = Tables.sentences;
        return getColumnMax(table, table.getConceptColumnIndex());
    }

    private int getMaxCharacterIdInUnicodes() {
        LangbookDbSchema.UnicodeCharactersTable table = Tables.unicodeCharacters;
        return getColumnMax(table, table.getIdColumnIndex());
    }

    private int getMaxCharacterIdInTokens() {
        LangbookDbSchema.CharacterTokensTable table = Tables.characterTokens;
        return getColumnMax(table, table.getIdColumnIndex());
    }

    int getMaxCorrelationId() {
        final LangbookDbSchema.CorrelationsTable table = Tables.correlations;
        return getColumnMax(table, table.getCorrelationIdColumnIndex());
    }

    int getMaxCorrelationArrayId() {
        final LangbookDbSchema.CorrelationArraysTable table = Tables.correlationArrays;
        return getColumnMax(table, table.getArrayIdColumnIndex());
    }

    int getMaxConcept() {
        int max = getMaxConceptInAcceptations();
        int temp = getMaxConceptInRuledConcepts();
        if (temp > max) {
            max = temp;
        }

        temp = getMaxLanguage();
        if (temp > max) {
            max = temp;
        }

        temp = getMaxAlphabet();
        if (temp > max) {
            max = temp;
        }

        temp = getMaxConceptInComplementedConcepts();
        if (temp > max) {
            max = temp;
        }

        temp = getMaxConceptInConceptCompositions();
        if (temp > max) {
            max = temp;
        }

        temp = getMaxConceptInSentences();
        if (temp > max) {
            max = temp;
        }

        return max;
    }

    int getMaxCharacterId() {
        int maxInUnicodes = getMaxCharacterIdInUnicodes();
        int maxInTokens = getMaxCharacterIdInTokens();
        return Math.max(maxInUnicodes, maxInTokens);
    }

    @Override
    public ConceptId getNextAvailableConceptId() {
        return _conceptIdSetter.getKeyFromInt(getMaxConcept() + 1);
    }

    CharacterId getNextAvailableCharacterId() {
        return _characterIdSetter.getKeyFromInt(getMaxCharacterId() + 1);
    }

    @Override
    public ImmutableMap<AlphabetId, AlphabetId> getConversionsMap() {
        final LangbookDbSchema.ConversionsTable conversions = Tables.conversions;

        final DbQuery query = new DbQuery.Builder(conversions)
                .groupBy(conversions.getSourceAlphabetColumnIndex(), conversions.getTargetAlphabetColumnIndex())
                .select(
                        conversions.getSourceAlphabetColumnIndex(),
                        conversions.getTargetAlphabetColumnIndex());

        final ImmutableMap.Builder<AlphabetId, AlphabetId> builder = new ImmutableHashMap.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                builder.put(_alphabetIdSetter.getKeyFromDbValue(row.get(1)), _alphabetIdSetter.getKeyFromDbValue(row.get(0)));
            }
        }

        return builder.build();
    }

    private ImmutablePair<ImmutableList<CorrelationId>, ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>>> getAcceptationCorrelations(AcceptationId acceptation) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.CorrelationArraysTable correlationArrays = Tables.correlationArrays;
        final LangbookDbSchema.CorrelationsTable correlations = Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbols = Tables.symbolArrays;

        final int corrArraysOffset = acceptations.columns().size();
        final int corrOffset = corrArraysOffset + correlationArrays.columns().size();
        final int symbolsOffset = corrOffset + correlations.columns().size();

        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(correlationArrays, acceptations.getCorrelationArrayColumnIndex(), correlationArrays.getArrayIdColumnIndex())
                .join(correlations, corrArraysOffset + correlationArrays.getCorrelationColumnIndex(), correlations.getCorrelationIdColumnIndex())
                .join(symbols, corrOffset + correlations.getSymbolArrayColumnIndex(), symbols.getIdColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .orderBy(
                        corrArraysOffset + correlationArrays.getArrayPositionColumnIndex(),
                        corrOffset + correlations.getAlphabetColumnIndex())
                .select(
                        corrArraysOffset + correlationArrays.getArrayPositionColumnIndex(),
                        corrOffset + correlations.getCorrelationIdColumnIndex(),
                        corrOffset + correlations.getAlphabetColumnIndex(),
                        symbolsOffset + symbols.getStrColumnIndex()
                );

        final MutableList<CorrelationId> correlationIds = MutableList.empty();
        final MutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> correlationMap = MutableHashMap.empty();
        try (DbResult dbResult = _db.select(query)) {
            if (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
                int pos = row.get(0).toInt();
                CorrelationId correlationId = _correlationIdSetter.getKeyFromDbValue(row.get(1));
                if (pos != correlationIds.size()) {
                    throw new AssertionError("Expected position " + correlationIds.size() + ", but it was " + pos);
                }

                builder.put(_alphabetIdSetter.getKeyFromDbValue(row.get(2)), row.get(3).toText());

                while (dbResult.hasNext()) {
                    row = dbResult.next();
                    int newPos = row.get(0).toInt();
                    if (newPos != pos) {
                        correlationMap.put(correlationId, builder.build());
                        correlationIds.append(correlationId);
                        correlationId = _correlationIdSetter.getKeyFromDbValue(row.get(1));
                        builder = new ImmutableCorrelation.Builder<>();
                        pos = newPos;
                    }

                    if (newPos != correlationIds.size()) {
                        throw new AssertionError("Expected position " + correlationIds.size() + ", but it was " + pos);
                    }
                    builder.put(_alphabetIdSetter.getKeyFromDbValue(row.get(2)), row.get(3).toText());
                }
                correlationMap.put(correlationId, builder.build());
                correlationIds.append(correlationId);
            }
        }

        return new ImmutablePair<>(correlationIds.toImmutable(), correlationMap.toImmutable());
    }

    private ImmutablePair<ImmutableList<CorrelationId>, ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>>> getCorrelations(CorrelationArrayId correlationArrayId) {
        final LangbookDbSchema.CorrelationArraysTable correlationArrays = Tables.correlationArrays;
        final LangbookDbSchema.CorrelationsTable correlations = Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbols = Tables.symbolArrays;

        final int corrOffset = correlationArrays.columns().size();
        final int symbolsOffset = corrOffset + correlations.columns().size();

        final DbQuery query = new DbQueryBuilder(correlationArrays)
                .join(correlations, correlationArrays.getCorrelationColumnIndex(), correlations.getCorrelationIdColumnIndex())
                .join(symbols, corrOffset + correlations.getSymbolArrayColumnIndex(), symbols.getIdColumnIndex())
                .where(correlationArrays.getArrayIdColumnIndex(), correlationArrayId)
                .orderBy(
                        correlationArrays.getArrayPositionColumnIndex(),
                        corrOffset + correlations.getAlphabetColumnIndex())
                .select(
                        correlationArrays.getArrayPositionColumnIndex(),
                        corrOffset + correlations.getCorrelationIdColumnIndex(),
                        corrOffset + correlations.getAlphabetColumnIndex(),
                        symbolsOffset + symbols.getStrColumnIndex()
                );

        final MutableList<CorrelationId> correlationIds = MutableList.empty();
        final MutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> correlationMap = MutableHashMap.empty();
        try (DbResult dbResult = _db.select(query)) {
            if (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
                int pos = row.get(0).toInt();
                CorrelationId correlationId = _correlationIdSetter.getKeyFromDbValue(row.get(1));
                if (pos != correlationIds.size()) {
                    throw new AssertionError("Expected position " + correlationIds.size() + ", but it was " + pos);
                }

                builder.put(_alphabetIdSetter.getKeyFromDbValue(row.get(2)), row.get(3).toText());

                while (dbResult.hasNext()) {
                    row = dbResult.next();
                    int newPos = row.get(0).toInt();
                    if (newPos != pos) {
                        correlationMap.put(correlationId, builder.build());
                        correlationIds.append(correlationId);
                        correlationId = _correlationIdSetter.getKeyFromDbValue(row.get(1));
                        builder = new ImmutableCorrelation.Builder<>();
                        pos = newPos;
                    }

                    if (newPos != correlationIds.size()) {
                        throw new AssertionError("Expected position " + correlationIds.size() + ", but it was " + pos);
                    }
                    builder.put(_alphabetIdSetter.getKeyFromDbValue(row.get(2)), row.get(3).toText());
                }
                correlationMap.put(correlationId, builder.build());
                correlationIds.append(correlationId);
            }
        }

        return new ImmutablePair<>(correlationIds.toImmutable(), correlationMap.toImmutable());
    }

    @Override
    public ImmutableList<CorrelationId> getAcceptationCorrelationArray(AcceptationId acceptation) {
        return getAcceptationCorrelations(acceptation).left;
    }

    CorrelationArrayId getAcceptationCorrelationArrayId(AcceptationId acceptationId) {
        final LangbookDbSchema.AcceptationsTable table = Tables.acceptations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), acceptationId)
                .select(table.getCorrelationArrayColumnIndex());
        return _correlationArrayIdSetter.getKeyFromDbValue(selectFirstDbValue(query));
    }

    ImmutablePair<ImmutableCorrelationArray<AlphabetId>, ImmutableList<CorrelationId>> getAcceptationCorrelationArrayWithText(AcceptationId acceptation) {
        final ImmutablePair<ImmutableList<CorrelationId>, ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>>> pair = getAcceptationCorrelations(acceptation);
        return new ImmutablePair<>(new ImmutableCorrelationArray<>(pair.left.map(pair.right::get)), pair.left);
    }

    @Override
    public ImmutablePair<ImmutableCorrelationArray<AlphabetId>, ImmutableList<CorrelationId>> getCorrelationArrayWithText(CorrelationArrayId correlationArrayId) {
        final ImmutablePair<ImmutableList<CorrelationId>, ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>>> pair = getCorrelations(correlationArrayId);
        return new ImmutablePair<>(new ImmutableCorrelationArray<>(pair.left.map(pair.right::get)), pair.left);
    }

    @Override
    public ImmutableSet<AcceptationId> findAcceptationsByConcept(ConceptId concept) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final DbQuery query = new DbQueryBuilder(acceptations)
                .where(acceptations.getConceptColumnIndex(), concept)
                .select(acceptations.getIdColumnIndex());

        return _db.select(query).map(row -> _acceptationIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    @Override
    public ImmutableSet<AcceptationId> getAcceptationsInBunch(BunchId bunch) {
        final LangbookDbSchema.BunchAcceptationsTable table = Tables.bunchAcceptations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .select(table.getAcceptationColumnIndex());

        return _db.select(query).map(row -> _acceptationIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    @Override
    public ImmutableSet<BunchId> findBunchesWhereAcceptationIsIncluded(AcceptationId acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable table = Tables.bunchAcceptations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .select(table.getBunchColumnIndex());
        return _db.select(query).map(row -> _bunchIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    private boolean checkMatching(ImmutableCorrelation<AlphabetId> startMatcher, ImmutableCorrelation<AlphabetId> endMatcher, ImmutableCorrelation<AlphabetId> texts) {
        for (Map.Entry<AlphabetId, String> entry : startMatcher.entries()) {
            final String text = texts.get(entry.key(), null);
            if (text == null || !text.startsWith(entry.value())) {
                return false;
            }
        }

        for (Map.Entry<AlphabetId, String> entry : endMatcher.entries()) {
            final String text = texts.get(entry.key(), null);
            if (text == null || !text.endsWith(entry.value())) {
                return false;
            }
        }

        return !startMatcher.isEmpty() || !endMatcher.isEmpty();
    }

    private ImmutableSet<BunchId> readBunchesFromSetOfBunchSets(ImmutableSet<BunchSetId> bunchSets) {
        final ImmutableSet.Builder<BunchId> builder = new ImmutableHashSet.Builder<>();
        for (BunchSetId bunchSet : bunchSets) {
            for (BunchId bunch : getBunchSet(bunchSet)) {
                builder.add(bunch);
            }
        }

        return builder.build();
    }

    @Override
    public ImmutableMap<BunchId, String> readAllMatchingBunches(ImmutableCorrelation<AlphabetId> texts, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final DbQuery query = new DbQuery.Builder(agents)
                .where(agents.getDiffBunchSetColumnIndex(), 0)
                .select(agents.getSourceBunchSetColumnIndex(),
                        agents.getStartMatcherColumnIndex(),
                        agents.getEndMatcherColumnIndex());

        final SyncCacheMap<CorrelationId, ImmutableCorrelation<AlphabetId>> cachedCorrelations =
                new SyncCacheMap<>(this::getCorrelationWithText);
        final ImmutableHashSet.Builder<BunchSetId> validBunchSetsBuilder = new ImmutableHashSet.Builder<>();

        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                BunchSetId bunchSet = _bunchSetIdSetter.getKeyFromDbValue(row.get(0));
                CorrelationId startMatcherId = _correlationIdSetter.getKeyFromDbValue(row.get(1));
                CorrelationId endMatcherId = _correlationIdSetter.getKeyFromDbValue(row.get(2));

                final ImmutableCorrelation<AlphabetId> startMatcher = cachedCorrelations.get(startMatcherId);
                final ImmutableCorrelation<AlphabetId> endMatcher = cachedCorrelations.get(endMatcherId);
                if (checkMatching(startMatcher, endMatcher, texts)) {
                    validBunchSetsBuilder.add(bunchSet);
                }
            }
        }

        final ImmutableSet<BunchId> bunches = readBunchesFromSetOfBunchSets(validBunchSetsBuilder.build());
        final ImmutableMap.Builder<BunchId, String> builder = new ImmutableHashMap.Builder<>();
        for (BunchId bunch : bunches) {
            builder.put(bunch, readConceptText(bunch.getConceptId(), preferredAlphabet));
        }

        return builder.build();
    }

    @Override
    public DefinitionDetails<ConceptId> getDefinition(ConceptId concept) {
        final LangbookDbSchema.ComplementedConceptsTable complementedConcepts = Tables.complementedConcepts;
        final LangbookDbSchema.ConceptCompositionsTable compositions = Tables.conceptCompositions;

        DbQuery query = new DbQueryBuilder(complementedConcepts)
                .where(complementedConcepts.getIdColumnIndex(), concept)
                .select(complementedConcepts.getBaseColumnIndex(), complementedConcepts.getComplementColumnIndex());

        final ConceptId baseConcept;
        final ConceptId compositionId;
        try (DbResult dbResult = _db.select(query)) {
            if (!dbResult.hasNext()) {
                return null;
            }

            final List<DbValue> row = dbResult.next();
            baseConcept = _conceptIdSetter.getKeyFromDbValue(row.get(0));
            compositionId = _conceptIdSetter.getKeyFromDbValue(row.get(1));
        }

        query = new DbQueryBuilder(compositions)
                .where(compositions.getComposedColumnIndex(), compositionId)
                .select(compositions.getItemColumnIndex());
        ImmutableSet<ConceptId> complements = _db.select(query).map(row -> _conceptIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
        if (complements.isEmpty() && compositionId != null) {
            complements = new ImmutableHashSet.Builder<ConceptId>().add(compositionId).build();
        }

        return new DefinitionDetails<>(baseConcept, complements);
    }

    @Override
    public ImmutableSet<String> findConversionConflictWords(ConversionProposal<AlphabetId> newConversion) {
        // TODO: Logic in the word should be somehow centralised with #checkConversionConflicts method
        final LangbookDbSchema.StringQueriesTable table = Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(table)
                .whereColumnValueMatch(table.getDynamicAcceptationColumnIndex(), table.getMainAcceptationColumnIndex())
                .where(table.getStringAlphabetColumnIndex(), newConversion.getSourceAlphabet())
                .select(table.getStringColumnIndex());

        return _db.select(query)
                .map(row -> row.get(0).toText())
                .filter(str -> newConversion.convert(str) == null)
                .toSet()
                .toImmutable();
    }

    @Override
    public ImmutableMap<QuizId, ImmutableSet<QuestionFieldDetails<AlphabetId, RuleId>>> readQuizSelectorEntriesForBunch(BunchId bunch) {
        final LangbookDbSchema.QuizDefinitionsTable quizzes = Tables.quizDefinitions;
        final LangbookDbSchema.QuestionFieldSets fieldSets = Tables.questionFieldSets;

        final int offset = quizzes.columns().size();
        final DbQuery query = new DbQueryBuilder(quizzes)
                .join(fieldSets, quizzes.getQuestionFieldsColumnIndex(), fieldSets.getSetIdColumnIndex())
                .where(quizzes.getBunchColumnIndex(), bunch)
                .select(
                        quizzes.getIdColumnIndex(),
                        offset + fieldSets.getAlphabetColumnIndex(),
                        offset + fieldSets.getRuleColumnIndex(),
                        offset + fieldSets.getFlagsColumnIndex());

        final MutableMap<QuizId, ImmutableSet<QuestionFieldDetails<AlphabetId, RuleId>>> resultMap = MutableHashMap.empty();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final QuizId quizId = _quizIdSetter.getKeyFromDbValue(row.get(0));
                final RuleId rule = _ruleIdSetter.getKeyFromDbValue(row.get(2));
                final QuestionFieldDetails<AlphabetId, RuleId> field = new QuestionFieldDetails<>(_alphabetIdSetter.getKeyFromDbValue(row.get(1)), rule, row.get(3).toInt());
                final ImmutableSet<QuestionFieldDetails<AlphabetId, RuleId>> set = resultMap.get(quizId, ImmutableHashSet.empty());
                resultMap.put(quizId, set.add(field));
            }
        }

        return resultMap.toImmutable();
    }

    @Override
    public Progress readQuizProgress(QuizId quizId) {
        final LangbookDbSchema.KnowledgeTable knowledge = Tables.knowledge;

        final DbQuery query = new DbQueryBuilder(knowledge)
                .where(knowledge.getQuizDefinitionColumnIndex(), quizId)
                .select(knowledge.getScoreColumnIndex());

        final int[] progress = new int[MAX_ALLOWED_SCORE - MIN_ALLOWED_SCORE + 1];
        int numberOfQuestions = 0;

        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                numberOfQuestions++;
                final int score = dbResult.next().get(0).toInt();
                if (score != NO_SCORE) {
                    progress[score - MIN_ALLOWED_SCORE]++;
                }
            }
        }

        return new Progress(ImmutableIntList.from(progress), numberOfQuestions);
    }

    @Override
    public ImmutableMap<RuleId, String> readAllRules(AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int accOffset = agents.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(agents)
                .join(acceptations, agents.getRuleColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .select(agents.getRuleColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final MutableMap<RuleId, String> result = MutableHashMap.empty();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                final RuleId rule = _ruleIdSetter.getKeyFromDbValue(row.get(0));

                if (result.get(rule, null) == null || preferredAlphabet.sameValue(row.get(1))) {
                    result.put(rule, row.get(2).toText());
                }
            }
        }

        return result.toImmutable();
    }

    private boolean isSymbolArrayUsedInAnyCorrelation(SymbolArrayId symbolArrayId) {
        final LangbookDbSchema.CorrelationsTable table = Tables.correlations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSymbolArrayColumnIndex(), symbolArrayId)
                .select(table.getIdColumnIndex());
        return selectExistAtLeastOneRow(query);
    }

    private boolean isSymbolArrayUsedInAnyConversion(SymbolArrayId symbolArrayId) {
        final LangbookDbSchema.ConversionsTable table = Tables.conversions;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getSourceColumnIndex(), table.getTargetColumnIndex());
        return _db.select(query).anyMatch(row -> symbolArrayId.sameValue(row.get(0)) || symbolArrayId.sameValue(row.get(1)));
    }

    @Override
    public boolean isSymbolArrayMerelyASentence(SymbolArrayId symbolArrayId) {
        return !isSymbolArrayUsedInAnyCorrelation(symbolArrayId) && !isSymbolArrayUsedInAnyConversion(symbolArrayId);
    }

    @Override
    public ConceptId conceptFromAcceptation(AcceptationId acceptationId) {
        final LangbookDbSchema.AcceptationsTable table = Tables.acceptations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), acceptationId)
                .select(table.getConceptColumnIndex());
        try (DbResult result = _db.select(query)) {
            final ConceptId concept = result.hasNext()? _conceptIdSetter.getKeyFromDbValue(result.next().get(0)) : null;

            if (result.hasNext()) {
                throw new AssertionError("Multiple rows found matching the given criteria");
            }

            return concept;
        }
    }

    @Override
    public boolean isAlphabetPresent(AlphabetId alphabet) {
        final LangbookDbSchema.AlphabetsTable table = alphabets;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), alphabet)
                .select(table.getIdColumnIndex());

        return selectExistingRow(query);
    }

    @Override
    public LanguageId getLanguageFromAlphabet(AlphabetId alphabet) {
        final LangbookDbSchema.AlphabetsTable table = alphabets;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), alphabet)
                .select(table.getLanguageColumnIndex());

        final DbValue value = selectOptionalFirstDbValue(query);
        return (value != null)? _languageIdSetter.getKeyFromDbValue(value) : null;
    }

    @Override
    public ImmutableMap<AlphabetId, String> readAllAlphabets(AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AlphabetsTable alphabets = Tables.alphabets;
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int accOffset = alphabets.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(alphabets)
                .join(acceptations, alphabets.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .orderBy(alphabets.getIdColumnIndex()) // I do not understand why, but it seems to be required to ensure order
                .select(alphabets.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final ImmutableMap.Builder<AlphabetId, String> builder = new ImmutableHashMap.Builder<>();
        try (DbResult result = _db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                AlphabetId alphabet = _alphabetIdSetter.getKeyFromDbValue(row.get(0));
                String text = row.get(2).toText();

                while (result.hasNext()) {
                    row = result.next();
                    if (alphabet.sameValue(row.get(0))) {
                        if (preferredAlphabet.sameValue(row.get(1))) {
                            text = row.get(2).toText();
                        }
                    }
                    else {
                        builder.put(alphabet, text);

                        alphabet = _alphabetIdSetter.getKeyFromDbValue(row.get(0));
                        text = row.get(2).toText();
                    }
                }

                builder.put(alphabet, text);
            }
        }

        return builder.build();
    }

    private static final class AcceptationOrigin<AcceptationId, AgentId> {
        final AcceptationId originalAcceptationId;
        final AgentId appliedAgent;
        final String originalAcceptationText;

        AcceptationOrigin(AcceptationId originalAcceptationId, AgentId appliedAgent, String originalAcceptationText) {
            this.originalAcceptationId = originalAcceptationId;
            this.appliedAgent = appliedAgent;
            this.originalAcceptationText = originalAcceptationText;
        }
    }

    private static final class AppliedRule<AcceptationId, RuleId> {
        final AcceptationId acceptationId;
        final RuleId rule;
        final String text;

        AppliedRule(AcceptationId acceptationId, RuleId rule, String text) {
            this.acceptationId = acceptationId;
            this.rule = rule;
            this.text = text;
        }
    }

    private AcceptationOrigin<AcceptationId, AgentId> readOriginalAcceptation(AcceptationId acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = Tables.ruledAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final int offset = ruledAcceptations.columns().size();

        final DbQuery query = new DbQueryBuilder(ruledAcceptations)
                .join(strings, ruledAcceptations.getAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(ruledAcceptations.getIdColumnIndex(), acceptation)
                .select(ruledAcceptations.getAcceptationColumnIndex(),
                        ruledAcceptations.getAgentColumnIndex(),
                        offset + strings.getStringAlphabetColumnIndex(),
                        offset + strings.getStringColumnIndex());

        AcceptationId id = null;
        AgentId agentId = null;
        String text = null;
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                if (id == null || preferredAlphabet.sameValue(row.get(2))) {
                    id = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
                    agentId = _agentIdSetter.getKeyFromDbValue(row.get(1));
                    text = row.get(3).toText();
                }
            }
        }

        return (id == null)? null : new AcceptationOrigin<>(id, agentId, text);
    }

    private AppliedRule<AcceptationId, RuleId> readRulePreferredTextByAgent(AgentId agentId, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final int accOffset = agents.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQueryBuilder(agents)
                .join(acceptations, agents.getRuleColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(agents.getIdColumnIndex(), agentId)
                .select(agents.getRuleColumnIndex(),
                        accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        AcceptationId acceptationId = null;
        RuleId ruleId = null;
        String text = null;
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                if (ruleId == null || preferredAlphabet.sameValue(row.get(2))) {
                    ruleId = _ruleIdSetter.getKeyFromDbValue(row.get(0));
                    acceptationId = _acceptationIdSetter.getKeyFromDbValue(row.get(1));
                    text = row.get(3).toText();
                }
            }
        }

        return (ruleId == null)? null : new AppliedRule<>(acceptationId, ruleId, text);
    }

    private IdTextPairResult<LanguageId> readLanguageFromAlphabet(AlphabetId alphabet, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.AlphabetsTable alphabets = Tables.alphabets;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int accOffset = alphabets.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQueryBuilder(alphabets)
                .join(acceptations, alphabets.getLanguageColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(alphabets.getIdColumnIndex(), alphabet)
                .select(
                        accOffset + acceptations.getConceptColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        try (DbResult dbResult = _db.select(query)) {
            if (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                LanguageId lang = _languageIdSetter.getKeyFromDbValue(row.get(0));
                String text = row.get(2).toText();
                boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(1));
                while (!preferredAlphabetFound && dbResult.hasNext()) {
                    row = dbResult.next();
                    if (preferredAlphabet.sameValue(row.get(1))) {
                        lang = _languageIdSetter.getKeyFromDbValue(row.get(0));
                        text = row.get(2).toText();
                        preferredAlphabetFound = true;
                    }
                }

                return new IdTextPairResult<>(lang, text);
            }
        }

        throw new IllegalArgumentException("alphabet " + alphabet + " not found");
    }

    private ImmutableMap<AcceptationId, ImmutableSet<AlphabetId>> readAcceptationsSharingTexts(AcceptationId acceptation) {
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int offset = strings.columns().size();
        final DbQuery query = new DbQueryBuilder(strings)
                .join(strings, strings.getStringColumnIndex(), strings.getStringColumnIndex())
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .whereColumnValueMatch(strings.getStringAlphabetColumnIndex(), offset + strings.getStringAlphabetColumnIndex())
                .select(strings.getStringAlphabetColumnIndex(), offset + strings.getDynamicAcceptationColumnIndex());

        final MutableSet<AlphabetId> foundAlphabets = MutableHashSet.empty();
        final MutableList<AlphabetId> sortedAlphabets = MutableList.empty();
        final MutableIntKeyMap<ImmutableSet<AlphabetId>> alphabetSets = MutableIntKeyMap.empty();
        alphabetSets.put(0, ImmutableHashSet.empty());
        final MutableIntValueMap<AcceptationId> result = MutableIntValueHashMap.empty();

        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final AlphabetId alphabet = _alphabetIdSetter.getKeyFromDbValue(row.get(0));
                final AcceptationId acc = _acceptationIdSetter.getKeyFromDbValue(row.get(1));

                if (!acceptation.equals(acc)) {
                    if (!foundAlphabets.contains(alphabet)) {
                        final int bitMask = 1 << foundAlphabets.size();
                        foundAlphabets.add(alphabet);
                        sortedAlphabets.append(alphabet);

                        for (int key : alphabetSets.keySet().toImmutable()) {
                            alphabetSets.put(bitMask | key, alphabetSets.get(key).add(alphabet));
                        }
                    }

                    final int alphabetBitPosition = 1 << sortedAlphabets.indexOf(alphabet);
                    final int value = result.get(acc, 0);
                    result.put(acc, value | alphabetBitPosition);
                }
            }
        }

        return result.map(alphabetSets::get).toImmutable();
    }

    private String getAcceptationDisplayableText(AcceptationIdInterface acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .select(strings.getStringAlphabetColumnIndex(), strings.getStringColumnIndex());

        String text = null;
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                if (text == null || preferredAlphabet.sameValue(row.get(0))) {
                    text = row.get(1).toText();
                }
            }
        }

        return text;
    }

    private ImmutableMap<AcceptationId, String> readDefinitionComponentsText(ConceptId compositionId, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.ConceptCompositionsTable compositions = Tables.conceptCompositions;
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int accOffset = compositions.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQueryBuilder(compositions)
                .join(acceptations, compositions.getItemColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(compositions.getComposedColumnIndex(), compositionId)
                .select(
                        compositions.getItemColumnIndex(),
                        accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final MutableIntKeyMap<AcceptationId> conceptAccMap = MutableIntKeyMap.empty();
        final MutableIntKeyMap<String> conceptTextMap = MutableIntKeyMap.empty();

        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int concept = row.get(0).toInt();
                final AcceptationId accId = _acceptationIdSetter.getKeyFromDbValue(row.get(1));
                final DbValue alphabet = row.get(2);
                final String text = row.get(3).toText();

                final AcceptationId currentAccId = conceptAccMap.get(concept, null);
                if (currentAccId == null || preferredAlphabet.sameValue(alphabet)) {
                    conceptAccMap.put(concept, accId);
                    conceptTextMap.put(concept, text);
                }
            }
        }

        final ImmutableMap.Builder<AcceptationId, String> builder = new ImmutableHashMap.Builder<>();
        for (IntKeyMap.Entry<AcceptationId> pair : conceptAccMap.entries()) {
            builder.put(pair.value(), conceptTextMap.get(pair.key()));
        }

        return builder.build();
    }

    private ImmutablePair<IdentifiableResult<AcceptationId>, ImmutableMap<AcceptationId, String>> readDefinitionFromAcceptation(AcceptationId acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.ComplementedConceptsTable complementedConcepts = Tables.complementedConcepts;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int bunchOffset = acceptations.columns().size();
        final int accOffset = bunchOffset + complementedConcepts.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(complementedConcepts, acceptations.getConceptColumnIndex(), complementedConcepts.getIdColumnIndex())
                .join(acceptations, bunchOffset + complementedConcepts.getBaseColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .select(accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex(),
                        bunchOffset + complementedConcepts.getComplementColumnIndex());

        IdentifiableResult<AcceptationId> result = null;
        ConceptId compositionId = null;
        try (DbResult dbResult = _db.select(query)) {
            if (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                AcceptationId acc = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
                boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(1));
                String text = row.get(2).toText();
                compositionId = _conceptIdSetter.getKeyFromDbValue(row.get(3));
                while (!preferredAlphabetFound && dbResult.hasNext()) {
                    row = dbResult.next();
                    if (preferredAlphabet.sameValue(row.get(1))) {
                        acc = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
                        text = row.get(2).toText();
                        break;
                    }
                }

                result = new IdentifiableResult<>(acc, text);
            }
        }

        ImmutableMap<AcceptationId, String> componentTexts = ImmutableHashMap.empty();
        if (compositionId != null) {
            final ImmutableMap<AcceptationId, String> texts = readDefinitionComponentsText(compositionId, preferredAlphabet);
            if (texts.isEmpty()) {
                final DisplayableItem<AcceptationId> item = readConceptAcceptationAndText(compositionId, preferredAlphabet);
                componentTexts = componentTexts.put(item.id, item.text);
            }
            else {
                componentTexts = texts;
            }
        }

        return new ImmutablePair<>(result, componentTexts);
    }

    private ImmutableMap<AcceptationId, String> readSubtypesFromAcceptation(AcceptationId acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.ComplementedConceptsTable complementedConcepts = Tables.complementedConcepts;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int bunchOffset = acceptations.columns().size();
        final int accOffset = bunchOffset + complementedConcepts.columns().size();
        final int strOffset = accOffset + bunchOffset;

        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(complementedConcepts, acceptations.getConceptColumnIndex(), complementedConcepts.getBaseColumnIndex())
                .join(acceptations, bunchOffset + complementedConcepts.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .select(bunchOffset + complementedConcepts.getIdColumnIndex(),
                        accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final MutableIntKeyMap<String> conceptText = MutableIntKeyMap.empty();
        final MutableIntKeyMap<AcceptationId> conceptAccs = MutableIntKeyMap.empty();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int concept = row.get(0).toInt();
                final DbValue alphabet = row.get(2);
                if (preferredAlphabet.sameValue(alphabet) || !conceptAccs.keySet().contains(concept)) {
                    conceptAccs.put(concept, _acceptationIdSetter.getKeyFromDbValue(row.get(1)));
                    conceptText.put(concept, row.get(3).toText());
                }
            }
        }

        final ImmutableMap.Builder<AcceptationId, String> builder = new ImmutableHashMap.Builder<>();
        final int length = conceptAccs.size();
        for (int i = 0; i < length; i++) {
            builder.put(conceptAccs.valueAt(i), conceptText.valueAt(i));
        }

        return builder.build();
    }

    private MutableMap<AcceptationId, SynonymTranslationResult<LanguageId>> readAcceptationSynonymsAndTranslations(AcceptationId acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.AlphabetsTable alphabets = Tables.alphabets;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int accOffset = acceptations.columns().size();
        final int stringsOffset = accOffset * 2;
        final int alphabetsOffset = stringsOffset + strings.columns().size();

        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(acceptations, acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(alphabets, stringsOffset + strings.getStringAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .select(stringsOffset + strings.getMainAcceptationColumnIndex(),
                        stringsOffset + strings.getDynamicAcceptationColumnIndex(),
                        alphabetsOffset + alphabets.getLanguageColumnIndex(),
                        stringsOffset + strings.getStringAlphabetColumnIndex(),
                        stringsOffset + strings.getStringColumnIndex());

        final MutableMap<AcceptationId, SynonymTranslationResult<LanguageId>> builder = MutableHashMap.empty();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final AcceptationId accId = _acceptationIdSetter.getKeyFromDbValue(row.get(1));
                if (!acceptation.equals(accId) && (builder.get(accId, null) == null || preferredAlphabet.sameValue(row.get(3)))) {
                    final LanguageId language = _languageIdSetter.getKeyFromDbValue(row.get(2));
                    builder.put(accId, new SynonymTranslationResult<>(language, row.get(4).toText(), !accId.sameValue(row.get(0))));
                }
            }
        }

        return builder;
    }

    private ImmutableList<DynamizableResult<AcceptationId>> readBunchesWhereAcceptationIsIncluded(AcceptationId acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int accOffset = bunchAcceptations.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQueryBuilder(bunchAcceptations)
                .join(acceptations, bunchAcceptations.getBunchColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(bunchAcceptations.getBunchColumnIndex(),
                        accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex(),
                        bunchAcceptations.getAgentColumnIndex());

        final MutableIntSet bunchesWhereIncludedStatically = MutableIntArraySet.empty();
        final MutableIntKeyMap<AcceptationId> acceptationsMap = MutableIntKeyMap.empty();
        final MutableIntKeyMap<String> textsMap = MutableIntKeyMap.empty();

        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int bunch = row.get(0).toInt();
                boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(2));

                if (preferredAlphabetFound || acceptationsMap.get(bunch, null) == null) {
                    final AcceptationId acc = _acceptationIdSetter.getKeyFromDbValue(row.get(1));
                    final String text = row.get(3).toText();
                    acceptationsMap.put(bunch, acc);
                    textsMap.put(bunch, text);
                }

                final int agent = row.get(4).toInt();
                if (agent == 0) {
                    bunchesWhereIncludedStatically.add(bunch);
                }
            }
        }

        return acceptationsMap.keySet().map(bunch -> new DynamizableResult<>(acceptationsMap.get(bunch), !bunchesWhereIncludedStatically.contains(bunch), textsMap.get(bunch))).toImmutable();
    }

    private DerivedAcceptationsReaderResult<AcceptationId, RuleId, AgentId> readDerivedAcceptations(AcceptationId acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = Tables.ruledAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final LangbookDbSchema.AgentsTable agents = Tables.agents;

        final int strOffset = ruledAcceptations.columns().size();
        final int agentsOffset = strOffset + strings.columns().size();

        final DbQuery query = new DbQueryBuilder(ruledAcceptations)
                .join(strings, ruledAcceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(agents, ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .where(ruledAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(ruledAcceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex(),
                        ruledAcceptations.getAgentColumnIndex(),
                        agentsOffset + agents.getRuleColumnIndex());

        final MutableMap<AcceptationId, String> texts = MutableHashMap.empty();
        final MutableMap<AcceptationId, AgentId> accAgents = MutableHashMap.empty();
        final MutableMap<AgentId, RuleId> agentRules = MutableHashMap.empty();

        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final AcceptationId dynAcc = _acceptationIdSetter.getKeyFromDbValue(row.get(0));

                final boolean dynAccNotFound = texts.get(dynAcc, null) == null;
                if (dynAccNotFound || preferredAlphabet.sameValue(row.get(1))) {
                    texts.put(dynAcc, row.get(2).toText());
                }

                if (dynAccNotFound) {
                    final AgentId agent = _agentIdSetter.getKeyFromDbValue(row.get(3));
                    final RuleId rule = _ruleIdSetter.getKeyFromDbValue(row.get(4));
                    accAgents.put(dynAcc, agent);
                    agentRules.put(agent, rule);
                }
            }
        }

        final ImmutableMap<AcceptationId, IdentifiableResult<AgentId>> acceptations = accAgents.keySet().assign(acc -> new IdentifiableResult<>(accAgents.get(acc), texts.get(acc))).toImmutable();
        final ImmutableMap<RuleId, String> ruleTexts = agentRules.toSet().assign(rule -> readConceptText(rule.getConceptId(), preferredAlphabet)).toImmutable();
        return new DerivedAcceptationsReaderResult<>(acceptations, ruleTexts, agentRules.toImmutable());
    }

    private ImmutableList<DynamizableResult<AcceptationId>> readAcceptationBunchChildren(AcceptationId acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int bunchAccOffset = acceptations.columns().size();
        final int strOffset = bunchAccOffset + bunchAcceptations.columns().size();
        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(bunchAcceptations, acceptations.getConceptColumnIndex(), bunchAcceptations.getBunchColumnIndex())
                .join(strings, bunchAccOffset + bunchAcceptations.getAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .select(strOffset + strings.getMainAcceptationColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex(),
                        bunchAccOffset + bunchAcceptations.getAgentColumnIndex());

        final MutableSet<AcceptationId> includedStatically = MutableHashSet.empty();
        final MutableMap<AcceptationId, String> accTexts = MutableHashMap.empty();

        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final AcceptationId acc = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
                boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(1));

                if (preferredAlphabetFound || accTexts.get(acc, null) == null) {
                    final String text = row.get(2).toText();
                    final int agent = row.get(3).toInt();
                    accTexts.put(acc, text);
                    if (agent == 0) {
                        includedStatically.add(acc);
                    }
                }
            }
        }

        return accTexts.keySet().map(acc -> new DynamizableResult<>(acc, !includedStatically.contains(acc), accTexts.get(acc))).toImmutable();
    }

    private ImmutableSet<AgentId> readAgentsWhereAcceptationIsTarget(AcceptationId staticAcceptation) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.BunchSetsTable bunchSets = Tables.bunchSets;
        final LangbookDbSchema.AgentsTable agents = Tables.agents;

        final int bunchSetsOffset = acceptations.columns().size();
        final int agentsOffset = bunchSetsOffset + bunchSets.columns().size();
        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(bunchSets, acceptations.getConceptColumnIndex(), bunchSets.getBunchColumnIndex())
                .join(agents, bunchSetsOffset + bunchSets.getSetIdColumnIndex(), agents.getTargetBunchSetColumnIndex())
                .where(acceptations.getIdColumnIndex(), staticAcceptation)
                .select(agentsOffset + agents.getIdColumnIndex());

        return _db.select(query).map(row -> _agentIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    private ImmutableSet<AgentId> readAgentsWhereAcceptationIsSource(AcceptationId staticAcceptation) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final LangbookDbSchema.BunchSetsTable bunchSets = Tables.bunchSets;

        final int bunchSetsOffset = acceptations.columns().size();
        final int agentsOffset = bunchSetsOffset + bunchSets.columns().size();

        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(bunchSets, acceptations.getConceptColumnIndex(), bunchSets.getBunchColumnIndex())
                .join(agents, bunchSetsOffset + bunchSets.getSetIdColumnIndex(), agents.getSourceBunchSetColumnIndex())
                .where(acceptations.getIdColumnIndex(), staticAcceptation)
                .select(agentsOffset + agents.getIdColumnIndex());

        return _db.select(query).map(row -> _agentIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    private ImmutableSet<AgentId> readAgentsWhereAcceptationIsRule(AcceptationId staticAcceptation) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.AgentsTable agents = Tables.agents;

        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(agents, acceptations.getConceptColumnIndex(), agents.getRuleColumnIndex())
                .where(acceptations.getIdColumnIndex(), staticAcceptation)
                .select(acceptations.columns().size() + agents.getIdColumnIndex());
        return _db.select(query).map(row -> _agentIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    private ImmutableSet<AgentId> readAgentsWhereAcceptationIsProcessed(AcceptationId acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;

        final DbQuery query = new DbQueryBuilder(bunchAcceptations)
                .where(bunchAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(bunchAcceptations.getAgentColumnIndex());

        return _db.select(query).map(row -> _agentIdSetter.getKeyFromDbValue(row.get(0))).filter(SortUtils::nonNull).toSet().toImmutable();
    }

    /**
     * Return information about all agents involved with the given acceptation.
     * @param staticAcceptation Identifier for the acceptation to analyze.
     * @return a map whose keys are agent identifier and values are flags. Flags should match the values at {@link sword.langbook3.android.models.AcceptationDetailsModel.InvolvedAgentResultFlags}
     */
    private ImmutableIntValueMap<AgentId> readAcceptationInvolvedAgents(AcceptationId staticAcceptation) {
        final MutableIntValueMap<AgentId> flags = MutableIntValueHashMap.empty();

        for (AgentId agentId : readAgentsWhereAcceptationIsTarget(staticAcceptation)) {
            flags.put(agentId, flags.get(agentId, 0) | AcceptationDetailsModel.InvolvedAgentResultFlags.target);
        }

        for (AgentId agentId : readAgentsWhereAcceptationIsSource(staticAcceptation)) {
            flags.put(agentId, flags.get(agentId, 0) | AcceptationDetailsModel.InvolvedAgentResultFlags.source);
        }

        // TODO: Diff not implemented as right now it is impossible

        for (AgentId agentId : readAgentsWhereAcceptationIsRule(staticAcceptation)) {
            flags.put(agentId, flags.get(agentId, 0) | AcceptationDetailsModel.InvolvedAgentResultFlags.rule);
        }

        for (AgentId agentId : readAgentsWhereAcceptationIsProcessed(staticAcceptation)) {
            flags.put(agentId, flags.get(agentId, 0) | AcceptationDetailsModel.InvolvedAgentResultFlags.processed);
        }

        return flags.toImmutable();
    }

    @Override
    public AcceptationDetailsModel<ConceptId, LanguageId, AlphabetId, CorrelationId, AcceptationId, RuleId, AgentId, SentenceId> getAcceptationsDetails(AcceptationId staticAcceptation, AlphabetId preferredAlphabet) {
        final ConceptId concept = conceptFromAcceptation(staticAcceptation);
        if (concept == null) {
            return null;
        }

        final AcceptationOrigin<AcceptationId, AgentId> origin = readOriginalAcceptation(staticAcceptation, preferredAlphabet);
        final AcceptationId originalAcceptationId = (origin != null)? origin.originalAcceptationId : null;
        final AgentId appliedAgentId = (origin != null)? origin.appliedAgent : null;
        final String originalAcceptationText = (origin != null)? origin.originalAcceptationText : null;

        final AcceptationId appliedRuleAcceptationId;
        final RuleId appliedRuleId;
        final String appliedRuleAcceptationText;
        if (appliedAgentId != null) {
            final AppliedRule<AcceptationId, RuleId> appliedRule = readRulePreferredTextByAgent(appliedAgentId, preferredAlphabet);
            appliedRuleAcceptationId = appliedRule.acceptationId;
            appliedRuleId = appliedRule.rule;
            appliedRuleAcceptationText = appliedRule.text;
        }
        else {
            appliedRuleAcceptationId = null;
            appliedRuleId = null;
            appliedRuleAcceptationText = null;
        }

        ImmutablePair<ImmutableList<CorrelationId>, ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>>> correlationResultPair = getAcceptationCorrelations(staticAcceptation);
        final AlphabetId givenAlphabet = correlationResultPair.right.get(correlationResultPair.left.get(0)).keyAt(0);
        final IdTextPairResult<LanguageId> languageResult = readLanguageFromAlphabet(givenAlphabet, preferredAlphabet);
        final MutableMap<LanguageId, String> languageStrs = MutableHashMap.empty();
        languageStrs.put(languageResult.id, languageResult.text);

        final ImmutableCorrelation<AlphabetId> texts = getAcceptationTexts(staticAcceptation);
        final ImmutableMap<AcceptationId, ImmutableSet<AlphabetId>> acceptationsSharingTexts = readAcceptationsSharingTexts(staticAcceptation);

        final ImmutableSet<AlphabetId> allAphabets = texts.keySet();
        final ImmutableSet<AcceptationId> accsSharingSome = acceptationsSharingTexts.filterNot(allAphabets::equalSet).keySet();
        final int accSharingSomeSize = accsSharingSome.size();
        final MutableMap<AcceptationId, String> accSharingSomeText = MutableHashMap.empty((currentSize, desiredSize) -> accSharingSomeSize);
        for (AcceptationId acc : accsSharingSome) {
            accSharingSomeText.put(acc, getAcceptationDisplayableText(acc, preferredAlphabet));
        }

        final ImmutablePair<IdentifiableResult<AcceptationId>, ImmutableMap<AcceptationId, String>> definition = readDefinitionFromAcceptation(staticAcceptation, preferredAlphabet);
        final ImmutableMap<AcceptationId, String> subtypes = readSubtypesFromAcceptation(staticAcceptation, preferredAlphabet);
        final ImmutableMap<AcceptationId, SynonymTranslationResult<LanguageId>> synonymTranslationResults =
                readAcceptationSynonymsAndTranslations(staticAcceptation, preferredAlphabet).toImmutable();
        for (Map.Entry<AcceptationId, SynonymTranslationResult<LanguageId>> entry : synonymTranslationResults.entries()) {
            final LanguageId language = entry.value().language;
            if (languageStrs.get(language, null) == null) {
                languageStrs.put(language, readConceptText(language.getConceptId(), preferredAlphabet));
            }
        }

        final ImmutableList<DynamizableResult<AcceptationId>> bunchesWhereAcceptationIsIncluded = readBunchesWhereAcceptationIsIncluded(staticAcceptation, preferredAlphabet);
        final DerivedAcceptationsReaderResult<AcceptationId, RuleId, AgentId> morphologyResults = readDerivedAcceptations(staticAcceptation, preferredAlphabet);
        final ImmutableList<DynamizableResult<AcceptationId>> bunchChildren = readAcceptationBunchChildren(staticAcceptation, preferredAlphabet);
        final ImmutableIntValueMap<AgentId> involvedAgents = readAcceptationInvolvedAgents(staticAcceptation);

        final ImmutableMap<RuleId, String> ruleTexts = (appliedRuleAcceptationId == null)? morphologyResults.ruleTexts :
                morphologyResults.ruleTexts.put(appliedRuleId, appliedRuleAcceptationText);

        final ImmutableMap<SentenceId, String> sampleSentences = getSampleSentences(staticAcceptation).putAll(
                getSampleSentencesApplyingRule(_ruleIdSetter.getKeyFromConceptId(concept)));

        final AcceptationId baseConceptAcceptationId = (definition.left != null)? definition.left.id : null;
        final String baseConceptText = (definition.left != null)? definition.left.text : null;
        return new AcceptationDetailsModel<>(concept, languageResult, originalAcceptationId, originalAcceptationText,
                appliedAgentId, appliedRuleId, appliedRuleAcceptationId, correlationResultPair.left,
                correlationResultPair.right, texts, acceptationsSharingTexts, accSharingSomeText.toImmutable(), baseConceptAcceptationId,
                baseConceptText, definition.right, subtypes, synonymTranslationResults, bunchChildren,
                bunchesWhereAcceptationIsIncluded, morphologyResults.acceptations,
                ruleTexts, involvedAgents, morphologyResults.agentRules, languageStrs.toImmutable(), sampleSentences);
    }

    @Override
    public ImmutableList<SearchResult<AcceptationId, RuleId>> getSearchHistory() {
        final LangbookDbSchema.SearchHistoryTable history = Tables.searchHistory;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final int offset = history.columns().size();
        final DbQuery query = new DbQuery.Builder(history)
                .join(strings, history.getAcceptation(), strings.getDynamicAcceptationColumnIndex())
                .orderBy(new DbQuery.Ordered(history.getIdColumnIndex(), true))
                .select(history.getAcceptation(),
                        offset + strings.getMainAcceptationColumnIndex(),
                        offset + strings.getStringColumnIndex(),
                        offset + strings.getMainStringColumnIndex());

        final MutableSet<AcceptationId> acceptations = MutableHashSet.empty();
        final ImmutableList.Builder<SearchResult<AcceptationId, RuleId>> builder = new ImmutableList.Builder<>();

        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final AcceptationId acceptation = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
                if (!acceptations.contains(acceptation)) {
                    acceptations.add(acceptation);
                    builder.add(new SearchResult<>(row.get(2).toText(), row.get(3).toText(), acceptation, !acceptation.sameValue(row.get(1))));
                }
            }
        }

        return builder.build();
    }

    @Override
    public QuizDetails<AlphabetId, BunchId, RuleId> getQuizDetails(QuizId quizId) {
        final LangbookDbSchema.QuizDefinitionsTable quizzes = Tables.quizDefinitions;
        final LangbookDbSchema.QuestionFieldSets questions = Tables.questionFieldSets;
        final int offset = quizzes.columns().size();
        final DbQuery query = new DbQueryBuilder(quizzes)
                .join(questions, quizzes.getQuestionFieldsColumnIndex(), questions.getSetIdColumnIndex())
                .where(quizzes.getIdColumnIndex(), quizId)
                .select(quizzes.getBunchColumnIndex(),
                        offset + questions.getAlphabetColumnIndex(),
                        offset + questions.getRuleColumnIndex(),
                        offset + questions.getFlagsColumnIndex());

        final ImmutableList.Builder<QuestionFieldDetails<AlphabetId, RuleId>> builder = new ImmutableList.Builder<>();
        BunchId bunch = null;

        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                bunch = _bunchIdSetter.getKeyFromDbValue(row.get(0));
                final RuleId rule = _ruleIdSetter.getKeyFromDbValue(row.get(2));
                builder.add(new QuestionFieldDetails<>(_alphabetIdSetter.getKeyFromDbValue(row.get(1)), rule, row.get(3).toInt()));
            }
        }

        final ImmutableList<QuestionFieldDetails<AlphabetId, RuleId>> fields = builder.build();
        return fields.isEmpty()? null : new QuizDetails<>(bunch, fields);
    }

    private String getAcceptationText(AcceptationId acceptation, AlphabetId alphabet) {
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strings.getStringColumnIndex());
        return selectSingleRow(query).get(0).toText();
    }

    private String readSameConceptTexts(AcceptationId acceptation, AlphabetId alphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int accOffset = acceptations.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(acceptations, acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getIdColumnIndex(), acceptation)
                .whereColumnValueDiffer(acceptations.getIdColumnIndex(), accOffset + acceptations.getIdColumnIndex())
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(strOffset + strings.getStringColumnIndex());

        return _db.select(query)
                .map(row -> row.get(0).toText())
                .reduce((a, b) -> a + ", " + b);
    }

    private String readApplyRuleText(AcceptationId acceptation, QuestionFieldDetails<AlphabetId, RuleId> field) {
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = Tables.ruledAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int strOffset = ruledAcceptations.columns().size();
        final int agentsOffset = strOffset + strings.columns().size();

        final DbQuery query = new DbQueryBuilder(ruledAcceptations)
                .join(strings, ruledAcceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(agents, ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .where(ruledAcceptations.getAcceptationColumnIndex(), acceptation)
                .where(agentsOffset + agents.getRuleColumnIndex(), field.rule)
                .where(strOffset + strings.getStringAlphabetColumnIndex(), field.alphabet)
                .select(strOffset + strings.getStringColumnIndex());
        return selectSingleRow(query).get(0).toText();
    }

    @Override
    public String readQuestionFieldText(AcceptationId acceptation, QuestionFieldDetails<AlphabetId, RuleId> field) {
        switch (field.getType()) {
            case LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC:
                return getAcceptationText(acceptation, field.alphabet);

            case LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT:
                return readSameConceptTexts(acceptation, field.alphabet);

            case LangbookDbSchema.QuestionFieldFlags.TYPE_APPLY_RULE:
                return readApplyRuleText(acceptation, field);
        }

        throw new UnsupportedOperationException("Unsupported question field type");
    }

    @Override
    public ImmutableIntValueMap<AcceptationId> getCurrentKnowledge(QuizId quizId) {
        final LangbookDbSchema.KnowledgeTable table = Tables.knowledge;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getQuizDefinitionColumnIndex(), quizId)
                .select(table.getAcceptationColumnIndex(), table.getScoreColumnIndex());

        final ImmutableIntValueMap.Builder<AcceptationId> builder = new ImmutableIntValueHashMap.Builder<>();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final AcceptationId acceptationId = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
                builder.put(acceptationId, row.get(1).toInt());
            }
        }

        return builder.build();
    }

    @Override
    public ImmutableSet<AgentId> getAgentIds() {
        final LangbookDbSchema.AgentsTable table = Tables.agents;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex());

        return _db.select(query).map(row -> _agentIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    @Override
    public ImmutableList<SearchResult<AcceptationId, RuleId>> findAcceptationFromText(String queryText, int restrictionStringType, ImmutableIntRange range) {
        final LangbookDbSchema.StringQueriesTable table = Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStringColumnIndex(), new DbQuery.Restriction(new DbStringValue(queryText),
                        restrictionStringType))
                .range(range)
                .select(
                        table.getStringColumnIndex(),
                        table.getMainStringColumnIndex(),
                        table.getMainAcceptationColumnIndex(),
                        table.getDynamicAcceptationColumnIndex());

        final MutableMap<AcceptationId, SearchResult<AcceptationId, RuleId>> map = MutableHashMap.empty();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final String str = row.get(0).toText();
                final String mainStr = row.get(1).toText();
                final AcceptationId acc = _acceptationIdSetter.getKeyFromDbValue(row.get(2));
                final AcceptationId dynAcc = _acceptationIdSetter.getKeyFromDbValue(row.get(3));

                map.put(dynAcc, new SearchResult<>(str, mainStr, dynAcc, !dynAcc.equals(acc)));
            }
        }

        return map.toList().toImmutable().sort((a, b) -> !a.isDynamic() && b.isDynamic() || a.isDynamic() == b.isDynamic() && SortUtils.compareCharSequenceByUnicode(a.getStr(), b.getStr()));
    }

    RuleId getAgentRule(AgentId agentId) {
        final LangbookDbSchema.AgentsTable table = Tables.agents;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), agentId)
                .select(table.getRuleColumnIndex());

        try (DbResult result = _db.select(query)) {
            return result.hasNext()? _ruleIdSetter.getKeyFromDbValue(result.next().get(0)) : null;
        }
    }

    @Override
    public AgentRegister<CorrelationId, CorrelationArrayId, BunchSetId, RuleId> getAgentRegister(AgentId agentId) {
        final LangbookDbSchema.AgentsTable table = Tables.agents;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), agentId)
                .select(table.getTargetBunchSetColumnIndex(),
                        table.getSourceBunchSetColumnIndex(),
                        table.getDiffBunchSetColumnIndex(),
                        table.getStartMatcherColumnIndex(),
                        table.getStartAdderArrayColumnIndex(),
                        table.getEndMatcherColumnIndex(),
                        table.getEndAdderArrayColumnIndex(),
                        table.getRuleColumnIndex());

        final List<DbValue> agentRow = selectOptionalSingleRow(query);
        if (agentRow != null) {
            final BunchSetId targetBunchSetId = _bunchSetIdSetter.getKeyFromDbValue(agentRow.get(0));
            final BunchSetId sourceBunchSetId = _bunchSetIdSetter.getKeyFromDbValue(agentRow.get(1));
            final BunchSetId diffBunchSetId = _bunchSetIdSetter.getKeyFromDbValue(agentRow.get(2));
            final CorrelationId startMatcherId = _correlationIdSetter.getKeyFromDbValue(agentRow.get(3));
            final CorrelationArrayId startAdderId = _correlationArrayIdSetter.getKeyFromDbValue(agentRow.get(4));
            final CorrelationId endMatcherId = _correlationIdSetter.getKeyFromDbValue(agentRow.get(5));
            final CorrelationArrayId endAdderId = _correlationArrayIdSetter.getKeyFromDbValue(agentRow.get(6));
            final RuleId ruleId = _ruleIdSetter.getKeyFromDbValue(agentRow.get(7));
            return new AgentRegister<>(targetBunchSetId, sourceBunchSetId, diffBunchSetId,
                    startMatcherId, startAdderId, endMatcherId, endAdderId, ruleId);
        }

        return null;
    }

    @Override
    public AgentDetails<AlphabetId, CorrelationId, BunchId, RuleId> getAgentDetails(AgentId agentId) {
        final AgentRegister<CorrelationId, CorrelationArrayId, BunchSetId, RuleId> register = getAgentRegister(agentId);
        final ImmutableSet<BunchId> targetBunches = getBunchSet(register.targetBunchSetId);
        final ImmutableSet<BunchId> sourceBunches = getBunchSet(register.sourceBunchSetId);
        final ImmutableSet<BunchId> diffBunches = (register.sourceBunchSetId != register.diffBunchSetId)?
                getBunchSet(register.diffBunchSetId) : sourceBunches;

        final ImmutableCorrelation<AlphabetId> startMatcher = getCorrelationWithText(register.startMatcherId);
        final ImmutablePair<ImmutableCorrelationArray<AlphabetId>, ImmutableList<CorrelationId>> startAdderPair = getCorrelationArrayWithText(register.startAdderId);
        final ImmutableCorrelation<AlphabetId> endMatcher = getCorrelationWithText(register.endMatcherId);
        final ImmutablePair<ImmutableCorrelationArray<AlphabetId>, ImmutableList<CorrelationId>> endAdderPair = getCorrelationArrayWithText(register.endAdderId);

        return new AgentDetails<>(targetBunches, sourceBunches, diffBunches,
                startMatcher, startAdderPair.left, startAdderPair.right,
                endMatcher, endAdderPair.left, endAdderPair.right, register.rule);
    }

    @Override
    public ImmutableList<DisplayableItem<AcceptationId>> readBunchSetAcceptationsAndTexts(BunchSetId bunchSet, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.BunchSetsTable bunchSets = Tables.bunchSets;
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int accOffset = bunchSets.columns().size();
        final int stringsOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQueryBuilder(bunchSets)
                .join(acceptations, bunchSets.getBunchColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchSets.getSetIdColumnIndex(), bunchSet)
                .select(
                        bunchSets.getBunchColumnIndex(),
                        accOffset + acceptations.getIdColumnIndex(),
                        stringsOffset + strings.getStringAlphabetColumnIndex(),
                        stringsOffset + strings.getStringColumnIndex()
                );

        ImmutableList.Builder<DisplayableItem<AcceptationId>> builder = new ImmutableList.Builder<>();
        try (DbResult cursor = _db.select(query)) {
            if (cursor.hasNext()) {
                List<DbValue> row = cursor.next();
                int bunch = row.get(0).toInt();
                AcceptationId acc = _acceptationIdSetter.getKeyFromDbValue(row.get(1));
                boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(2));
                String text = row.get(3).toText();

                while (cursor.hasNext()) {
                    row = cursor.next();
                    if (bunch == row.get(0).toInt()) {
                        if (!preferredAlphabetFound && preferredAlphabet.sameValue(row.get(2))) {
                            acc = _acceptationIdSetter.getKeyFromDbValue(row.get(1));
                            text = row.get(3).toText();
                            preferredAlphabetFound = true;
                        }
                    }
                    else {
                        builder.add(new DisplayableItem<>(acc, text));

                        bunch = row.get(0).toInt();
                        acc = _acceptationIdSetter.getKeyFromDbValue(row.get(1));
                        preferredAlphabetFound = preferredAlphabet.sameValue(row.get(2));
                        text = row.get(3).toText();
                    }
                }

                builder.add(new DisplayableItem<>(acc, text));
            }
        }

        return builder.build();
    }

    private static final class AcceptationFromTextResult<AcceptationId> {
        final String str;
        final String mainStr;
        final AcceptationId acceptation;
        final AcceptationId baseAcceptation;

        AcceptationFromTextResult(String str, String mainStr, AcceptationId acceptation, AcceptationId baseAcceptation) {
            if (str == null || mainStr == null || acceptation == null || baseAcceptation == null) {
                throw new IllegalArgumentException();
            }

            this.str = str;
            this.mainStr = mainStr;
            this.acceptation = acceptation;
            this.baseAcceptation = baseAcceptation;
        }

        boolean isDynamic() {
            return !acceptation.equals(baseAcceptation);
        }
    }

    private ImmutableList<AcceptationFromTextResult<AcceptationId>> findAcceptationFromText2(String queryText, int restrictionStringType, ImmutableIntRange range) {
        final LangbookDbSchema.StringQueriesTable table = Tables.stringQueries;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStringColumnIndex(), new DbQuery.Restriction(new DbStringValue(queryText),
                        restrictionStringType))
                .range(range)
                .select(
                        table.getStringColumnIndex(),
                        table.getMainStringColumnIndex(),
                        table.getMainAcceptationColumnIndex(),
                        table.getDynamicAcceptationColumnIndex());

        final MutableMap<AcceptationId, AcceptationFromTextResult<AcceptationId>> map = MutableHashMap.empty();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final String str = row.get(0).toText();
                final String mainStr = row.get(1).toText();
                final AcceptationId acc = _acceptationIdSetter.getKeyFromDbValue(row.get(2));
                final AcceptationId dynAcc = _acceptationIdSetter.getKeyFromDbValue(row.get(3));

                map.put(dynAcc, new AcceptationFromTextResult<>(str, mainStr, dynAcc, acc));
            }
        }

        return map.toList().toImmutable().sort((a, b) -> !a.isDynamic() && b.isDynamic() || a.isDynamic() == b.isDynamic() && SortUtils.compareCharSequenceByUnicode(a.str, b.str));
    }

    @Override
    public ImmutableList<SearchResult<AcceptationId, RuleId>> findAcceptationAndRulesFromText(String queryText, int restrictionStringType, ImmutableIntRange range) {
        final ImmutableList<AcceptationFromTextResult<AcceptationId>> rawResult = findAcceptationFromText2(queryText, restrictionStringType, range);
        final SyncCacheMap<AcceptationId, String> mainTexts = new SyncCacheMap<>(this::readAcceptationMainText);

        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final int offset = ruledAcceptations.columns().size();

        return rawResult.map(rawEntry -> {
            if (rawEntry.isDynamic()) {
                ImmutableList<RuleId> rules = ImmutableList.empty();
                AcceptationId acc = rawEntry.acceptation;
                while (!acc.equals(rawEntry.baseAcceptation)) {
                    final DbQuery query = new DbQueryBuilder(ruledAcceptations)
                            .join(agents, ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                            .where(ruledAcceptations.getIdColumnIndex(), acc)
                            .select(ruledAcceptations.getAcceptationColumnIndex(), offset + agents.getRuleColumnIndex());

                    final List<DbValue> row = selectSingleRow(query);
                    acc = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
                    rules = rules.append(_ruleIdSetter.getKeyFromDbValue(row.get(1)));
                }

                return new SearchResult<>(rawEntry.str, rawEntry.mainStr, rawEntry.acceptation, true, mainTexts.get(acc), rules);
            }
            else {
                return new SearchResult<>(rawEntry.str, rawEntry.mainStr, rawEntry.acceptation, false);
            }
        });
    }

    @Override
    public AcceptationId getStaticAcceptationFromDynamic(AcceptationId dynamicAcceptation) {
        final LangbookDbSchema.StringQueriesTable table = Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getDynamicAcceptationColumnIndex(), dynamicAcceptation)
                .select(table.getMainAcceptationColumnIndex());

        AcceptationId value = null;
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final AcceptationId newValue = _acceptationIdSetter.getKeyFromDbValue(dbResult.next().get(0));
                if (value == null) {
                    value = newValue;
                }
                else if (!value.equals(newValue)) {
                    throw new AssertionError();
                }
            }
        }

        return value;
    }

    @Override
    public ConceptId findRuledConcept(RuleId rule, ConceptId concept) {
        final LangbookDbSchema.RuledConceptsTable table = Tables.ruledConcepts;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getRuleColumnIndex(), rule)
                .where(table.getConceptColumnIndex(), concept)
                .select(table.getIdColumnIndex());

        try (DbResult result = _db.select(query)) {
            final ConceptId id = result.hasNext()? _conceptIdSetter.getKeyFromDbValue(result.next().get(0)) : null;
            if (result.hasNext()) {
                throw new AssertionError("There should not be repeated ruled concepts");
            }
            return id;
        }
    }

    @Override
    public ImmutableMap<ConceptId, ConceptId> findRuledConceptsByRule(RuleId rule) {
        final LangbookDbSchema.RuledConceptsTable table = Tables.ruledConcepts;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getRuleColumnIndex(), rule)
                .select(table.getIdColumnIndex(), table.getConceptColumnIndex());

        final ImmutableHashMap.Builder<ConceptId, ConceptId> builder = new ImmutableHashMap.Builder<>();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> list = result.next();
                final ConceptId ruledConcept = _conceptIdSetter.getKeyFromDbValue(list.get(0));
                final ConceptId baseConcept = _conceptIdSetter.getKeyFromDbValue(list.get(1));
                builder.put(ruledConcept, baseConcept);
            }
        }

        return builder.build();
    }

    @Override
    public AcceptationId findRuledAcceptationByAgentAndBaseAcceptation(AgentId agentId, AcceptationId baseAcceptation) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAccs = Tables.ruledAcceptations;
        final DbQuery query = new DbQueryBuilder(ruledAccs)
                .where(ruledAccs.getAcceptationColumnIndex(), baseAcceptation)
                .where(ruledAccs.getAgentColumnIndex(), agentId)
                .select(ruledAccs.getIdColumnIndex());
        final DbResult dbResult = _db.select(query);
        final AcceptationId result = dbResult.hasNext()? _acceptationIdSetter.getKeyFromDbValue(dbResult.next().get(0)) : null;
        if (dbResult.hasNext()) {
            throw new AssertionError();
        }

        return result;
    }

    @Override
    public String readAcceptationMainText(AcceptationId acceptation) {
        final LangbookDbSchema.StringQueriesTable table = Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(table.getMainStringColumnIndex());

        return selectFirstRow(query).get(0).toText();
    }

    @Override
    public ImmutableSet<AgentId> findAllAgentsThatIncludedAcceptationInBunch(BunchId bunch, AcceptationId acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;

        final DbQuery query = new DbQueryBuilder(bunchAcceptations)
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .where(bunchAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(bunchAcceptations.getAgentColumnIndex());

        return _db.select(query).map(row -> _agentIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    @Override
    public ImmutableMap<AcceptationId, AcceptationId> getAgentProcessedMap(AgentId agentId) {
        final LangbookDbSchema.RuledAcceptationsTable table = Tables.ruledAcceptations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .select(table.getAcceptationColumnIndex(), table.getIdColumnIndex());

        final ImmutableMap.Builder<AcceptationId, AcceptationId> builder = new ImmutableHashMap.Builder<>();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final AcceptationId baseAcceptation = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
                final AcceptationId ruledAcceptation = _acceptationIdSetter.getKeyFromDbValue(row.get(1));
                builder.put(baseAcceptation, ruledAcceptation);
            }
        }

        return builder.build();
    }

    @Override
    public MorphologyReaderResult<AcceptationId, RuleId, AgentId> readMorphologiesFromAcceptation(AcceptationId acceptation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = Tables.agents;

        final int ruledAccOffset = strings.columns().size();
        final int agentsOffset = ruledAccOffset + ruledAcceptations.columns().size();

        final DbQuery mainQuery = new DbQueryBuilder(strings)
                .join(ruledAcceptations, strings.getDynamicAcceptationColumnIndex(), ruledAcceptations.getIdColumnIndex())
                .join(agents, ruledAccOffset + ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .where(strings.getMainAcceptationColumnIndex(), acceptation)
                .select(strings.getDynamicAcceptationColumnIndex(),
                        strings.getStringAlphabetColumnIndex(),
                        strings.getStringColumnIndex(),
                        ruledAccOffset + ruledAcceptations.getAcceptationColumnIndex(),
                        ruledAccOffset + ruledAcceptations.getAgentColumnIndex(),
                        agentsOffset + agents.getRuleColumnIndex());

        final MutableMap<AcceptationId, String> texts = MutableHashMap.empty();
        final MutableMap<AcceptationId, AcceptationId> sourceAccs = MutableHashMap.empty();
        final MutableMap<AcceptationId, RuleId> accRules = MutableHashMap.empty();
        final MutableMap<AgentId, RuleId> agentRules = MutableHashMap.empty();

        try (DbResult dbResult = _db.select(mainQuery)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final AcceptationId dynAcc = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
                final DbValue alphabet = row.get(1);
                final AgentId agent = _agentIdSetter.getKeyFromDbValue(row.get(4));
                final RuleId rule = _ruleIdSetter.getKeyFromDbValue(row.get(5));

                final boolean dynAccNotFound = texts.get(dynAcc, null) == null;
                if (dynAccNotFound || preferredAlphabet.sameValue(alphabet)) {
                    texts.put(dynAcc, row.get(2).toText());
                }

                if (dynAccNotFound) {
                    sourceAccs.put(dynAcc, _acceptationIdSetter.getKeyFromDbValue(row.get(3)));
                    accRules.put(dynAcc, rule);
                }

                agentRules.put(agent, rule);
            }
        }

        final ImmutableSet<RuleId> rules = accRules.toSet().toImmutable();
        final ImmutableMap<RuleId, String> ruleTexts = rules.assign(rule -> readConceptText(rule.getConceptId(), preferredAlphabet));

        final ImmutableList<MorphologyResult<AcceptationId, RuleId>> morphologies = sourceAccs.keySet().map(dynAcc -> {
            AcceptationId acc = dynAcc;
            ImmutableList<RuleId> ruleChain = ImmutableList.empty();
            while (!acceptation.equals(acc)) {
                ruleChain = ruleChain.append(accRules.get(acc));
                acc = sourceAccs.get(acc);
            }
            return new MorphologyResult<>(dynAcc, ruleChain, texts.get(dynAcc));
        }).toImmutable();

        return new MorphologyReaderResult<>(morphologies, ruleTexts, agentRules.toImmutable());
    }

    @Override
    public ImmutableSet<AcceptationId> getAcceptationsInBunchByBunchAndAgent(BunchId bunch, AgentId agent) {
        final LangbookDbSchema.BunchAcceptationsTable table = Tables.bunchAcceptations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .where(table.getAgentColumnIndex(), agent)
                .select(table.getAcceptationColumnIndex());

        return _db.select(query).map(row -> _acceptationIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    @Override
    public ImmutableSet<BunchId> getBunchSet(BunchSetId setId) {
        if (setId.isDeclaredEmpty()) {
            return ImmutableHashSet.empty();
        }

        final LangbookDbSchema.BunchSetsTable table = Tables.bunchSets;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSetIdColumnIndex(), setId)
                .select(table.getBunchColumnIndex());

        return _db.select(query).map(row -> _bunchIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    @Override
    public String getSentenceText(SentenceId sentenceId) {
        final LangbookDbSchema.SentencesTable table = Tables.sentences;
        final LangbookDbSchema.SymbolArraysTable texts = Tables.symbolArrays;
        final DbQuery query = new DbQueryBuilder(table)
                .join(texts, table.getSymbolArrayColumnIndex(), texts.getIdColumnIndex())
                .where(table.getIdColumnIndex(), sentenceId)
                .select(table.columns().size() + texts.getStrColumnIndex());
        return selectOptionalFirstTextColumn(query);
    }

    @Override
    public ImmutableSet<SentenceSpan<AcceptationId>> getSentenceSpans(SentenceId sentenceId) {
        final LangbookDbSchema.SpanTable table = Tables.spans;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceIdColumnIndex(), sentenceId)
                .select(table.getStartColumnIndex(), table.getLengthColumnIndex(), table.getDynamicAcceptationColumnIndex());
        final ImmutableHashSet.Builder<SentenceSpan<AcceptationId>> builder = new ImmutableHashSet.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int start = row.get(0).toInt();
                final int length = row.get(1).toInt();
                final AcceptationId acc = _acceptationIdSetter.getKeyFromDbValue(row.get(2));
                final ImmutableIntRange range = new ImmutableIntRange(start, start + length - 1);
                builder.add(new SentenceSpan<>(range, acc));
            }
        }

        return builder.build();
    }

    ImmutableSet<SentenceId> getSentencesByDynamicAcceptation(AcceptationId acceptation) {
        final LangbookDbSchema.SpanTable table = Tables.spans;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(table.getSentenceIdColumnIndex());

        return _db.select(query)
                .map(row -> _sentenceIdSetter.getKeyFromDbValue(row.get(0)))
                .toSet()
                .toImmutable();
    }

    @Override
    public ImmutableMap<LanguageId, String> readAllLanguages(AlphabetId preferredAlphabet) {
        final LangbookDbSchema.LanguagesTable languages = Tables.languages;
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable stringQueries = Tables.stringQueries;

        final int accOffset = languages.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQuery.Builder(languages)
                .join(acceptations, languages.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(stringQueries, accOffset + acceptations.getIdColumnIndex(), stringQueries.getDynamicAcceptationColumnIndex())
                .select(
                        languages.getIdColumnIndex(),
                        strOffset + stringQueries.getStringAlphabetColumnIndex(),
                        strOffset + stringQueries.getStringColumnIndex()
                );

        MutableSet<LanguageId> foundLanguages = MutableHashSet.empty();
        MutableMap<LanguageId, String> result = MutableHashMap.empty();

        try (DbResult r = _db.select(query)) {
            while (r.hasNext()) {
                List<DbValue> row = r.next();
                final LanguageId lang = _languageIdSetter.getKeyFromDbValue(row.get(0));
                final boolean isPreferredAlphabet = preferredAlphabet.sameValue(row.get(1));

                if (isPreferredAlphabet || !foundLanguages.contains(lang)) {
                    foundLanguages.add(lang);
                    result.put(lang, row.get(2).toText());
                }
            }
        }

        return result.toImmutable();
    }

    @Override
    public ImmutableCorrelation<AlphabetId> getCorrelationWithText(CorrelationId correlationId) {
        final LangbookDbSchema.CorrelationsTable correlations = Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbolArrays = Tables.symbolArrays;

        final DbQuery query = new DbQueryBuilder(correlations)
                .join(symbolArrays, correlations.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .where(correlations.getCorrelationIdColumnIndex(), correlationId)
                .select(correlations.getAlphabetColumnIndex(), correlations.columns().size() + symbolArrays.getStrColumnIndex());
        final ImmutableMap.Builder<AlphabetId, String> builder = new ImmutableHashMap.Builder<>();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                builder.put(_alphabetIdSetter.getKeyFromDbValue(row.get(0)), row.get(1).toText());
            }
        }
        return new ImmutableCorrelation<>(builder.build());
    }

    @Override
    public DisplayableItem<AcceptationId> readConceptAcceptationAndText(ConceptId concept, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int j1Offset = acceptations.columns().size();
        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(strings, acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getConceptColumnIndex(), concept)
                .select(j1Offset + strings.getStringAlphabetColumnIndex(),
                        acceptations.getIdColumnIndex(),
                        j1Offset + strings.getStringColumnIndex());

        AcceptationId acceptation;
        String text;
        try (DbResult result = _db.select(query)) {
            List<DbValue> row = result.next();
            boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(0));
            acceptation = _acceptationIdSetter.getKeyFromDbValue(row.get(1));
            text = row.get(2).toText();
            while (!preferredAlphabetFound && result.hasNext()) {
                row = result.next();
                if (preferredAlphabet.sameValue(row.get(0))) {
                    preferredAlphabetFound = true;
                    acceptation = _acceptationIdSetter.getKeyFromDbValue(row.get(1));
                    text = row.get(2).toText();
                }
            }
        }

        return new DisplayableItem<>(acceptation, text);
    }

    @Override
    public String readConceptText(ConceptId concept, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int j1Offset = acceptations.columns().size();
        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(strings, acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(acceptations.getConceptColumnIndex(), concept)
                .select(j1Offset + strings.getStringAlphabetColumnIndex(), j1Offset + strings.getStringColumnIndex());

        String text;
        try (DbResult result = _db.select(query)) {
            List<DbValue> row = result.next();
            boolean preferredAlphabetFound = preferredAlphabet.sameValue(row.get(0));
            text = row.get(1).toText();
            while (!preferredAlphabetFound && result.hasNext()) {
                row = result.next();
                if (preferredAlphabet.sameValue(row.get(0))) {
                    preferredAlphabetFound = true;
                    text = row.get(1).toText();
                }
            }
        }

        return text;
    }

    @Override
    public ImmutableMap<AlphabetId, String> readAlphabetsForLanguage(LanguageId language, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AlphabetsTable alphabets = Tables.alphabets;
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable stringQueries = Tables.stringQueries;

        final int accOffset = alphabets.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();

        final DbQuery query = new DbQueryBuilder(alphabets)
                .join(acceptations, alphabets.getIdColumnIndex(), acceptations.getConceptColumnIndex())
                .join(stringQueries, accOffset + acceptations.getIdColumnIndex(), stringQueries.getDynamicAcceptationColumnIndex())
                .where(alphabets.getLanguageColumnIndex(), language)
                .select(
                        alphabets.getIdColumnIndex(),
                        strOffset + stringQueries.getStringAlphabetColumnIndex(),
                        strOffset + stringQueries.getStringColumnIndex());

        final MutableSet<AlphabetId> foundAlphabets = MutableHashSet.empty();
        final MutableMap<AlphabetId, String> result = MutableHashMap.empty();
        try (DbResult r = _db.select(query)) {
            while (r.hasNext()) {
                final List<DbValue> row = r.next();
                final AlphabetId id = _alphabetIdSetter.getKeyFromDbValue(row.get(0));
                final boolean isPreferredAlphabet = preferredAlphabet.sameValue(row.get(1));

                if (isPreferredAlphabet || !foundAlphabets.contains(id)) {
                    foundAlphabets.add(id);
                    result.put(id, row.get(2).toText());
                }
            }
        }

        return result.toImmutable();
    }

    ImmutableSet<AlphabetId> alphabetsWithinLanguage(AlphabetId alphabet) {
        final LangbookDbSchema.AlphabetsTable table = alphabets;
        final int offset = table.columns().size();
        final DbQuery query = new DbQueryBuilder(table)
                .join(table, table.getLanguageColumnIndex(), table.getLanguageColumnIndex())
                .where(table.getIdColumnIndex(), alphabet)
                .select(offset + table.getIdColumnIndex());

        final ImmutableSet.Builder<AlphabetId> builder = new ImmutableHashSet.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                builder.add(_alphabetIdSetter.getKeyFromDbValue(dbResult.next().get(0)));
            }
        }

        return builder.build();
    }

    boolean isAlphabetUsedInQuestions(AlphabetId alphabet) {
        final LangbookDbSchema.QuestionFieldSets table = Tables.questionFieldSets;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getAlphabetColumnIndex(), alphabet)
                .select(table.getIdColumnIndex());

        try (DbResult dbResult = _db.select(query)) {
            return dbResult.hasNext();
        }
    }

    @Override
    public boolean checkAlphabetCanBeRemoved(AlphabetId alphabet) {
        // There must be at least another alphabet in the same language to avoid leaving the language without alphabets
        if (alphabetsWithinLanguage(alphabet).size() < 2) {
            return false;
        }

        // For now, let's assume that a source alphabet cannot be removed while the conversion already exists
        if (getConversionsMap().contains(alphabet)) {
            return false;
        }

        // First, quizzes using this alphabet should be removed
        return !isAlphabetUsedInQuestions(alphabet);
    }

    private ImmutableMap<AcceptationId, String> readStaticAcceptationsIncludingCorrelation(CorrelationId correlation, AlphabetId preferredAlphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.CorrelationArraysTable correlationArrays = Tables.correlationArrays;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int accOffset = correlationArrays.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final DbQuery query = new DbQueryBuilder(correlationArrays)
                .join(acceptations, correlationArrays.getArrayIdColumnIndex(), acceptations.getCorrelationArrayColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(correlationArrays.getCorrelationColumnIndex(), correlation)
                .whereColumnValueMatch(strOffset + strings.getDynamicAcceptationColumnIndex(), strOffset + strings.getMainAcceptationColumnIndex())
                .select(accOffset + acceptations.getIdColumnIndex(),
                        strOffset + strings.getStringAlphabetColumnIndex(),
                        strOffset + strings.getStringColumnIndex());

        final MutableMap<AcceptationId, String> result = MutableHashMap.empty();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final AcceptationId accId = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
                if (preferredAlphabet.sameValue(row.get(1)) || result.get(accId, null) == null) {
                    result.put(accId, row.get(2).toText());
                }
            }
        }

        return result.toImmutable();
    }

    private ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> readCorrelationsWithSameSymbolArray(CorrelationId correlation, AlphabetId alphabet) {
        final LangbookDbSchema.CorrelationsTable correlations = Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbolArrays = Tables.symbolArrays;

        final int corrOffset2 = correlations.columns().size();
        final int corrOffset3 = corrOffset2 + corrOffset2;
        final int strOffset = corrOffset3 + corrOffset2;

        final DbQuery query = new DbQueryBuilder(correlations)
                .join(correlations, correlations.getSymbolArrayColumnIndex(), correlations.getSymbolArrayColumnIndex())
                .join(correlations, corrOffset2 + correlations.getCorrelationIdColumnIndex(), correlations.getCorrelationIdColumnIndex())
                .join(symbolArrays, corrOffset3 + correlations.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .where(correlations.getCorrelationIdColumnIndex(), correlation)
                .whereColumnValueMatch(correlations.getAlphabetColumnIndex(), corrOffset2 + correlations.getAlphabetColumnIndex())
                .whereColumnValueDiffer(correlations.getCorrelationIdColumnIndex(), corrOffset2 + correlations.getCorrelationIdColumnIndex())
                .where(correlations.getAlphabetColumnIndex(), alphabet)
                .select(
                        corrOffset2 + correlations.getCorrelationIdColumnIndex(),
                        corrOffset3 + correlations.getAlphabetColumnIndex(),
                        strOffset + symbolArrays.getStrColumnIndex());

        final MutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> result = MutableHashMap.empty();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final CorrelationId corrId = _correlationIdSetter.getKeyFromDbValue(row.get(0));
                final AlphabetId textAlphabet = _alphabetIdSetter.getKeyFromDbValue(row.get(1));
                final String text = row.get(2).toText();
                final ImmutableCorrelation<AlphabetId> currentCorr = result.get(corrId, ImmutableCorrelation.empty());
                result.put(corrId, currentCorr.put(textAlphabet, text));
            }
        }

        return result.toImmutable();
    }

    @Override
    public CorrelationDetailsModel<AlphabetId, CharacterId, CorrelationId, AcceptationId> getCorrelationDetails(CorrelationId correlationId, AlphabetId preferredAlphabet) {
        final ImmutableCorrelation<AlphabetId> correlation = getCorrelationWithText(correlationId);
        if (correlation.isEmpty()) {
            return null;
        }

        final ImmutableMap<AlphabetId, String> alphabets = readAllAlphabets(preferredAlphabet);
        final ImmutableMap<AcceptationId, String> acceptations = readStaticAcceptationsIncludingCorrelation(correlationId, preferredAlphabet);

        final int entryCount = correlation.size();
        final MutableMap<AlphabetId, ImmutableSet<CorrelationId>> relatedCorrelationsByAlphabet = MutableHashMap.empty();
        final MutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> relatedCorrelations = MutableHashMap.empty();

        for (int i = 0; i < entryCount; i++) {
            final AlphabetId matchingAlphabet = correlation.keyAt(i);
            final ImmutableMap<CorrelationId, ImmutableCorrelation<AlphabetId>> correlations = readCorrelationsWithSameSymbolArray(correlationId, matchingAlphabet);

            final int amount = correlations.size();
            final ImmutableSet.Builder<CorrelationId> setBuilder = new ImmutableHashSet.Builder<>();
            for (int j = 0; j < amount; j++) {
                final CorrelationId corrId = correlations.keyAt(j);
                if (relatedCorrelations.get(corrId, null) == null) {
                    relatedCorrelations.put(corrId, correlations.valueAt(j));
                }

                setBuilder.add(corrId);
            }
            relatedCorrelationsByAlphabet.put(matchingAlphabet, setBuilder.build());
        }

        final MutableSet<Character> foundCharacters = MutableHashSet.empty();
        for (String text : correlation) {
            final int textLength = text.length();
            for (int i = 0; i < textLength; i++) {
                foundCharacters.add(text.charAt(i));
            }
        }

        final MutableMap<Character, CharacterId> characters = MutableHashMap.empty();
        for (char ch : foundCharacters) {
            final CharacterId characterId = findCharacter(ch);
            if (characterId != null) {
                characters.put(ch, characterId);
            }
        }

        return new CorrelationDetailsModel<>(alphabets, correlation, acceptations,
                relatedCorrelationsByAlphabet.toImmutable(), relatedCorrelations.toImmutable(), characters.toImmutable());
    }

    @Override
    public CharacterId findCharacter(char ch) {
        final LangbookDbSchema.UnicodeCharactersTable table = Tables.unicodeCharacters;
        final DbQuery dbQuery = new DbQueryBuilder(table)
                .where(table.getUnicodeColumnIndex(), ch)
                .select(table.getIdColumnIndex());

        final DbValue dbValue = selectOptionalFirstDbValue(dbQuery);
        return (dbValue != null)? _characterIdSetter.getKeyFromDbValue(dbValue) : null;
    }

    @Override
    public CorrelationId findCorrelation(Correlation<AlphabetId> correlation) {
        if (correlation.isEmpty()) {
            return null;
        }
        final ImmutableCorrelation<AlphabetId> immutableCorrelation = correlation.toImmutable();

        final LangbookDbSchema.CorrelationsTable table = Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbolArrays = Tables.symbolArrays;

        final int offset = table.columns().size();
        final int offset2 = offset + symbolArrays.columns().size();
        final int offset3 = offset2 + table.columns().size();

        final DbQuery query = new DbQueryBuilder(table)
                .join(symbolArrays, table.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .join(table, table.getCorrelationIdColumnIndex(), table.getCorrelationIdColumnIndex())
                .join(symbolArrays, offset2 + table.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .where(offset + symbolArrays.getStrColumnIndex(), immutableCorrelation.valueAt(0))
                .where(table.getAlphabetColumnIndex(), immutableCorrelation.keyAt(0))
                .select(
                        table.getCorrelationIdColumnIndex(),
                        offset2 + table.getAlphabetColumnIndex(),
                        offset3 + symbolArrays.getStrColumnIndex());

        try (DbResult result = _db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                CorrelationId correlationId = _correlationIdSetter.getKeyFromDbValue(row.get(0));
                ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
                builder.put(_alphabetIdSetter.getKeyFromDbValue(row.get(1)), row.get(2).toText());

                while (result.hasNext()) {
                    row = result.next();
                    CorrelationId newCorrelationId = _correlationIdSetter.getKeyFromDbValue(row.get(0));
                    if (!newCorrelationId.equals(correlationId)) {
                        if (builder.build().equals(immutableCorrelation)) {
                            return correlationId;
                        }

                        correlationId = newCorrelationId;
                        builder = new ImmutableCorrelation.Builder<>();
                    }

                    builder.put(_alphabetIdSetter.getKeyFromDbValue(row.get(1)), row.get(2).toText());
                }

                if (builder.build().equals(immutableCorrelation)) {
                    return correlationId;
                }
            }
        }

        return null;
    }

    @Override
    public boolean isAnyLanguagePresent() {
        final LangbookDbSchema.LanguagesTable table = Tables.languages;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex());

        return selectExistAtLeastOneRow(query);
    }

    @Override
    public ImmutablePair<ImmutableCorrelation<AlphabetId>, LanguageId> readAcceptationTextsAndLanguage(AcceptationId acceptation) {
        final LangbookDbSchema.StringQueriesTable table = Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabetsTable = alphabets;
        final DbQuery query = new DbQueryBuilder(table)
                .join(alphabetsTable, table.getStringAlphabetColumnIndex(), alphabetsTable.getIdColumnIndex())
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(
                        table.getStringAlphabetColumnIndex(),
                        table.getStringColumnIndex(),
                        table.columns().size() + alphabetsTable.getLanguageColumnIndex());
        final ImmutableMap.Builder<AlphabetId, String> builder = new ImmutableHashMap.Builder<>();
        boolean languageSet = false;
        LanguageId language = null;
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final AlphabetId alphabet = _alphabetIdSetter.getKeyFromDbValue(row.get(0));
                final String text = row.get(1).toText();

                if (!languageSet) {
                    language = _languageIdSetter.getKeyFromDbValue(row.get(2));
                    languageSet = true;
                }
                else if (!language.sameValue(row.get(2))) {
                    throw new AssertionError();
                }

                builder.put(alphabet, text);
            }
        }

        return new ImmutablePair<>(new ImmutableCorrelation<>(builder.build()), language);
    }

    @Override
    public ImmutableMap<AlphabetId, AlphabetId> findConversions(Set<AlphabetId> alphabets) {
        final LangbookDbSchema.ConversionsTable conversions = Tables.conversions;

        final DbQuery query = new DbQuery.Builder(conversions)
                .groupBy(conversions.getSourceAlphabetColumnIndex(), conversions.getTargetAlphabetColumnIndex())
                .select(
                        conversions.getSourceAlphabetColumnIndex(),
                        conversions.getTargetAlphabetColumnIndex());

        final MutableSet<AlphabetId> foundAlphabets = MutableHashSet.empty();
        final ImmutableMap.Builder<AlphabetId, AlphabetId> builder = new ImmutableHashMap.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final AlphabetId source = _alphabetIdSetter.getKeyFromDbValue(row.get(0));
                final AlphabetId target = _alphabetIdSetter.getKeyFromDbValue(row.get(1));

                if (foundAlphabets.contains(target)) {
                    throw new AssertionError();
                }
                foundAlphabets.add(target);

                if (alphabets.contains(target)) {
                    if (!alphabets.contains(source)) {
                        throw new AssertionError();
                    }

                    builder.put(target, source);
                }
            }
        }

        return builder.build();
    }

    private AcceptationId getMainAcceptation(AcceptationId acceptation) {
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(strings)
                .where(strings.getDynamicAcceptationColumnIndex(), acceptation)
                .select(strings.getMainAcceptationColumnIndex());

        try (DbResult result = _db.select(query)) {
            return result.hasNext()? _acceptationIdSetter.getKeyFromDbValue(result.next().get(0)) : acceptation;
        }
    }

    @Override
    public ImmutableMap<String, AcceptationId> readTextAndDynamicAcceptationsMapFromAcceptation(AcceptationId acceptation) {
        final AcceptationId staticAcceptation = getMainAcceptation(acceptation);
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final DbQuery query = new DbQueryBuilder(strings)
                .where(strings.getMainAcceptationColumnIndex(), staticAcceptation)
                .select(strings.getDynamicAcceptationColumnIndex(),
                        strings.getStringColumnIndex());

        final MutableMap<String, AcceptationId> result = MutableHashMap.empty();
        final MutableSet<String> discardTexts = MutableHashSet.empty();

        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final AcceptationId dynAcc = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
                final String text = row.get(1).toText();

                if (!discardTexts.contains(text)) {
                    final AcceptationId foundDynAcc = result.get(text, null);
                    if (foundDynAcc == null) {
                        result.put(text, dynAcc);
                    }
                    else if (!foundDynAcc.equals(dynAcc)) {
                        discardTexts.add(text);
                        result.removeAt(result.indexOfKey(text));
                    }
                }
            }
        }

        return result.toImmutable();
    }

    @Override
    public ImmutableMap<SentenceId, String> getSampleSentences(AcceptationId staticAcceptation) {
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final LangbookDbSchema.SpanTable spans = Tables.spans;
        final int offset = strings.columns().size();

        final DbQuery query = new DbQueryBuilder(strings)
                .join(spans, strings.getDynamicAcceptationColumnIndex(), spans.getDynamicAcceptationColumnIndex())
                .where(strings.getMainAcceptationColumnIndex(), getMainAcceptation(staticAcceptation))
                .select(offset + spans.getSentenceIdColumnIndex());

        return _db.select(query).map(row -> _sentenceIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable().assign(this::getSentenceText);
    }

    @Override
    public ImmutableMap<SentenceId, String> getSampleSentencesApplyingRule(RuleId appliedRule) {
        final LangbookDbSchema.RuleSentenceMatchesTable ruleSentenceMatches = Tables.ruleSentenceMatches;
        final LangbookDbSchema.SentencesTable sentences = Tables.sentences;
        final LangbookDbSchema.SymbolArraysTable texts = Tables.symbolArrays;

        final int sentencesOffset = ruleSentenceMatches.columns().size();
        final int textsOffset = sentencesOffset + sentences.columns().size();

        final DbQuery query = new DbQueryBuilder(ruleSentenceMatches)
                .join(sentences, ruleSentenceMatches.getSentenceColumnIndex(), sentences.getIdColumnIndex())
                .join(texts, sentencesOffset + sentences.getSymbolArrayColumnIndex(), texts.getIdColumnIndex())
                .where(ruleSentenceMatches.getRuleColumnIndex(), appliedRule)
                .select(ruleSentenceMatches.getSentenceColumnIndex(), textsOffset + texts.getStrColumnIndex());

        final MutableHashMap<SentenceId, String> result = MutableHashMap.empty();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                result.put(_sentenceIdSetter.getKeyFromDbValue(row.get(0)), row.get(1).toText());
            }
        }

        return result.toImmutable();
    }

    ImmutableSet<SentenceId> getSentencesApplyingRule(RuleId appliedRule) {
        final LangbookDbSchema.RuleSentenceMatchesTable ruleSentenceMatches = Tables.ruleSentenceMatches;

        final DbQuery query = new DbQueryBuilder(ruleSentenceMatches)
                .where(ruleSentenceMatches.getRuleColumnIndex(), appliedRule)
                .select(ruleSentenceMatches.getSentenceColumnIndex());

        return _db.select(query).map(row -> _sentenceIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    Set<RuleId> getAppliedRulesBySentenceId(SentenceId sentence) {
        final LangbookDbSchema.RuleSentenceMatchesTable ruleSentenceMatches = Tables.ruleSentenceMatches;
        final DbQuery query = new DbQueryBuilder(ruleSentenceMatches)
                .where(ruleSentenceMatches.getSentenceColumnIndex(), sentence)
                .select(ruleSentenceMatches.getRuleColumnIndex());
        return _db.select(query).map(row -> _ruleIdSetter.getKeyFromDbValue(row.get(0))).toSet();
    }

    private static final class SentenceConceptAndText<ConceptId> {
        final ConceptId concept;
        final String text;

        SentenceConceptAndText(ConceptId concept, String text) {
            this.concept = concept;
            this.text = text;
        }
    }

    private SentenceConceptAndText<ConceptId> getSentenceConceptAndText(SentenceId sentenceId) {
        final LangbookDbSchema.SentencesTable sentences = Tables.sentences;
        final LangbookDbSchema.SymbolArraysTable texts = Tables.symbolArrays;
        final DbQuery query = new DbQueryBuilder(sentences)
                .join(texts, sentences.getSymbolArrayColumnIndex(), texts.getIdColumnIndex())
                .where(sentences.getIdColumnIndex(), sentenceId)
                .select(sentences.getConceptColumnIndex(), sentences.columns().size() + texts.getStrColumnIndex());

        try (DbResult dbResult = _db.select(query)) {
            if (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final ConceptId concept = _conceptIdSetter.getKeyFromDbValue(row.get(0));
                return new SentenceConceptAndText<>(concept, row.get(1).toText());
            }
        }

        return null;
    }

    private ImmutableSet<SentenceId> findSentenceIdsMatchingMeaning(SentenceId sentenceId) {
        final LangbookDbSchema.SentencesTable table = Tables.sentences;
        final int offset = table.columns().size();
        final DbQuery query = new DbQueryBuilder(table)
                .join(table, table.getConceptColumnIndex(), table.getConceptColumnIndex())
                .where(table.getIdColumnIndex(), sentenceId)
                .whereColumnValueDiffer(table.getIdColumnIndex(), offset + table.getIdColumnIndex())
                .select(offset + table.getIdColumnIndex());
        return _db.select(query).map(row -> _sentenceIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    @Override
    public SentenceDetailsModel<ConceptId, AcceptationId, SentenceId> getSentenceDetails(SentenceId sentenceId) {
        final SentenceConceptAndText<ConceptId> conceptAndText = getSentenceConceptAndText(sentenceId);
        if (conceptAndText == null) {
            return null;
        }

        final ImmutableSet<SentenceSpan<AcceptationId>> spans = getSentenceSpans(sentenceId);
        final ImmutableMap<SentenceId, String> sameMeaningSentences = findSentenceIdsMatchingMeaning(sentenceId).assign(this::getSentenceText);
        return new SentenceDetailsModel<>(conceptAndText.concept, conceptAndText.text, spans, sameMeaningSentences);
    }

    ImmutableSet<CorrelationId> findCorrelationsByLanguage(LanguageId language) {
        final LangbookDbSchema.CorrelationsTable correlations = Tables.correlations;
        final LangbookDbSchema.AlphabetsTable alphabets = Tables.alphabets;
        final int offset = correlations.columns().size();
        final DbQuery query = new DbQueryBuilder(correlations)
                .join(alphabets, correlations.getAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .where(offset + alphabets.getLanguageColumnIndex(), language)
                .select(correlations.getCorrelationIdColumnIndex());

        return _db.select(query).map(row -> _correlationIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    ImmutableMap<CorrelationId, SymbolArrayId> findCorrelationsAndSymbolArrayForAlphabet(AlphabetId sourceAlphabet) {
        final LangbookDbSchema.CorrelationsTable correlations = Tables.correlations;
        final DbQuery query = new DbQueryBuilder(correlations)
                .where(correlations.getAlphabetColumnIndex(), sourceAlphabet)
                .select(correlations.getCorrelationIdColumnIndex(), correlations.getSymbolArrayColumnIndex());

        final ImmutableMap.Builder<CorrelationId, SymbolArrayId> builder = new ImmutableHashMap.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final CorrelationId correlationId = _correlationIdSetter.getKeyFromDbValue(row.get(0));
                final SymbolArrayId symbolArrayId = _symbolArrayIdSetter.getKeyFromDbValue(row.get(1));
                builder.put(correlationId, symbolArrayId);
            }
        }

        return builder.build();
    }

    private ImmutableSet<CorrelationId> findCorrelationsUsedInAgentMatchers() {
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final DbQuery query = new DbQuery.Builder(agents)
                .select(agents.getStartMatcherColumnIndex(), agents.getEndMatcherColumnIndex());

        final ImmutableSet.Builder<CorrelationId> builder = new ImmutableHashSet.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                for (int i = 0; i < 2; i++) {
                    builder.add(_correlationIdSetter.getKeyFromDbValue(row.get(i)));
                }
            }
        }

        return builder.build();
    }

    private ImmutableSet<CorrelationId> findCorrelationsUsedInAgentAdders(int adderColumn) {
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final LangbookDbSchema.CorrelationArraysTable correlationArrays = Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(agents)
                .join(correlationArrays, adderColumn, correlationArrays.getArrayIdColumnIndex())
                .select(correlationArrays.getCorrelationColumnIndex());

        final ImmutableSet.Builder<CorrelationId> builder = new ImmutableHashSet.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                builder.add(_correlationIdSetter.getKeyFromDbValue(dbResult.next().get(0)));
            }
        }

        return builder.build();
    }

    ImmutableSet<CorrelationId> findCorrelationsUsedInAgents() {
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        return findCorrelationsUsedInAgentMatchers()
                .addAll(findCorrelationsUsedInAgentAdders(agents.getStartAdderArrayColumnIndex()))
                .addAll(findCorrelationsUsedInAgentAdders(agents.getEndAdderArrayColumnIndex()));
    }

    ImmutableSet<AcceptationId> findAcceptationsByLanguage(LanguageId language) {
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabets = Tables.alphabets;
        final int offset = strings.columns().size();
        final DbQuery query = new DbQueryBuilder(strings)
                .join(alphabets, strings.getStringAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .where(offset + alphabets.getLanguageColumnIndex(), language)
                .select(strings.getDynamicAcceptationColumnIndex());

        final ImmutableSet.Builder<AcceptationId> builder = new ImmutableHashSet.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                builder.add(_acceptationIdSetter.getKeyFromDbValue(dbResult.next().get(0)));
            }
        }

        return builder.build();
    }

    ImmutableIntSet findBunchConceptsLinkedToJustThisLanguage(LanguageId language) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabets = Tables.alphabets;

        final int accOffset = bunchAcceptations.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final int alphabetsOffset = strOffset + strings.columns().size();

        final DbQuery query = new DbQuery.Builder(bunchAcceptations)
                .join(acceptations, bunchAcceptations.getBunchColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(alphabets, strOffset + strings.getStringAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .groupBy(bunchAcceptations.getBunchColumnIndex())
                .select(bunchAcceptations.getBunchColumnIndex(), alphabetsOffset + alphabets.getLanguageColumnIndex());

        MutableIntKeyMap<ImmutableSet<LanguageId>> map = MutableIntKeyMap.empty();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int bunch = row.get(0).toInt();
                final LanguageId lang = _languageIdSetter.getKeyFromDbValue(row.get(1));
                final ImmutableSet<LanguageId> set = map.get(bunch, ImmutableHashSet.empty()).add(lang);
                map.put(bunch, set);
            }
        }

        return map.filter(set -> set.contains(language) && set.size() == 1).keySet().toImmutable();
    }

    ImmutableSet<LanguageId> findIncludedAcceptationLanguages(int bunch) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabets = Tables.alphabets;

        final int strOffset = bunchAcceptations.columns().size();
        final int alpOffset = strOffset + strings.columns().size();

        final DbQuery query = new DbQuery.Builder(bunchAcceptations)
                .join(strings, bunchAcceptations.getAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(alphabets, strOffset + strings.getStringAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .groupBy(alpOffset + alphabets.getLanguageColumnIndex())
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .select(alpOffset + alphabets.getLanguageColumnIndex());

        return _db.select(query).map(row -> _languageIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    ImmutableIntSet findSuperTypesLinkedToJustThisLanguage(LanguageId language) {
        final LangbookDbSchema.ComplementedConceptsTable complementedConcepts = Tables.complementedConcepts;
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final LangbookDbSchema.AlphabetsTable alphabets = Tables.alphabets;

        final int accOffset = complementedConcepts.columns().size();
        final int strOffset = accOffset + acceptations.columns().size();
        final int alphabetsOffset = strOffset + strings.columns().size();

        final DbQuery query = new DbQuery.Builder(complementedConcepts)
                .join(acceptations, complementedConcepts.getBaseColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .join(alphabets, strOffset + strings.getStringAlphabetColumnIndex(), alphabets.getIdColumnIndex())
                .groupBy(complementedConcepts.getBaseColumnIndex())
                .select(complementedConcepts.getBaseColumnIndex(), alphabetsOffset + alphabets.getLanguageColumnIndex());

        MutableIntKeyMap<ImmutableSet<LanguageId>> map = MutableIntKeyMap.empty();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int bunch = row.get(0).toInt();
                final LanguageId lang = _languageIdSetter.getKeyFromDbValue(row.get(1));
                final ImmutableSet<LanguageId> set = map.get(bunch, ImmutableHashSet.empty()).add(lang);
                map.put(bunch, set);
            }
        }

        return map.filter(set -> set.contains(language) && set.size() == 1).keySet().toImmutable();
    }

    CharacterId findCharacterToken(String token) {
        final LangbookDbSchema.CharacterTokensTable table = Tables.characterTokens;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getTokenColumnIndex(), token)
                .select(table.getIdColumnIndex());

        final DbValue dbValue = selectOptionalFirstDbValue(query);
        return (dbValue != null)? _characterIdSetter.getKeyFromDbValue(dbValue) : null;
    }

    SymbolArrayId findSymbolArray(String str) {
        final LangbookDbSchema.SymbolArraysTable table = Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .where(table.getStrColumnIndex(), str)
                .select(table.getIdColumnIndex());

        try (DbResult result = _db.select(query)) {
            final SymbolArrayId value = result.hasNext()? _symbolArrayIdSetter.getKeyFromDbValue(result.next().get(0)) : null;
            if (result.hasNext()) {
                throw new AssertionError();
            }

            return value;
        }
    }

    CorrelationId findCorrelation(Map<AlphabetId, SymbolArrayId> correlation) {
        if (correlation.size() == 0) {
            return _correlationIdSetter.getKeyFromInt(EMPTY_CORRELATION_ID);
        }
        final ImmutableMap<AlphabetId, SymbolArrayId> corr = correlation.toImmutable();

        final LangbookDbSchema.CorrelationsTable table = Tables.correlations;
        final int offset = table.columns().size();
        final DbQuery query = new DbQueryBuilder(table)
                .join(table, table.getCorrelationIdColumnIndex(), table.getCorrelationIdColumnIndex())
                .where(table.getSymbolArrayColumnIndex(), corr.valueAt(0))
                .where(table.getAlphabetColumnIndex(), corr.keyAt(0))
                .select(
                        table.getCorrelationIdColumnIndex(),
                        offset + table.getAlphabetColumnIndex(),
                        offset + table.getSymbolArrayColumnIndex());

        try (DbResult result = _db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                CorrelationId correlationId = _correlationIdSetter.getKeyFromDbValue(row.get(0));
                final SymbolArrayId symbolArrayId = _symbolArrayIdSetter.getKeyFromDbValue(row.get(2));
                ImmutableMap.Builder<AlphabetId, SymbolArrayId> builder = new ImmutableHashMap.Builder<>();
                builder.put(_alphabetIdSetter.getKeyFromDbValue(row.get(1)), symbolArrayId);

                while (result.hasNext()) {
                    row = result.next();
                    final CorrelationId newCorrelationId = _correlationIdSetter.getKeyFromDbValue(row.get(0));
                    if (!equal(newCorrelationId, correlationId)) {
                        if (builder.build().equals(corr)) {
                            return correlationId;
                        }

                        correlationId = newCorrelationId;
                        builder = new ImmutableHashMap.Builder<>();
                    }

                    final AlphabetId alphabet = _alphabetIdSetter.getKeyFromDbValue(row.get(1));
                    final SymbolArrayId symbolArray = _symbolArrayIdSetter.getKeyFromDbValue(row.get(2));
                    builder.put(alphabet, symbolArray);
                }

                if (builder.build().equalMap(corr)) {
                    return correlationId;
                }
            }
        }

        return null;
    }

    CorrelationArrayId findCorrelationArray(List<CorrelationId> array) {
        if (array.isEmpty()) {
            return _correlationArrayIdSetter.getKeyFromInt(EMPTY_CORRELATION_ARRAY_ID);
        }

        final LangbookDbSchema.CorrelationArraysTable table = Tables.correlationArrays;
        final int offset = table.columns().size();
        final DbQuery query = new DbQueryBuilder(table)
                .join(table, table.getArrayIdColumnIndex(), table.getArrayIdColumnIndex())
                .where(table.getArrayPositionColumnIndex(), 0)
                .where(table.getCorrelationColumnIndex(), array.get(0))
                .orderBy(table.getArrayIdColumnIndex(), offset + table.getArrayPositionColumnIndex())
                .select(table.getArrayIdColumnIndex(), offset + table.getCorrelationColumnIndex());

        try (DbResult result = _db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                CorrelationArrayId arrayId = _correlationArrayIdSetter.getKeyFromDbValue(row.get(0));
                ImmutableList.Builder<CorrelationId> builder = new ImmutableList.Builder<>();
                builder.add(_correlationIdSetter.getKeyFromDbValue(row.get(1)));

                while (result.hasNext()) {
                    row = result.next();
                    CorrelationArrayId newArrayId = _correlationArrayIdSetter.getKeyFromDbValue(row.get(0));
                    if (!arrayId.equals(newArrayId)) {
                        if (builder.build().equalTraversable(array)) {
                            return arrayId;
                        }

                        arrayId = newArrayId;
                        builder = new ImmutableList.Builder<>();
                    }
                    builder.add(_correlationIdSetter.getKeyFromDbValue(row.get(1)));
                }

                if (builder.build().equalTraversable(array)) {
                    return arrayId;
                }
            }
        }

        return null;
    }

    ImmutableSet<AcceptationId> findRuledAcceptationByBaseAcceptation(AcceptationId baseAcceptation) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAccs = Tables.ruledAcceptations;
        final DbQuery query = new DbQueryBuilder(ruledAccs)
                .where(ruledAccs.getAcceptationColumnIndex(), baseAcceptation)
                .select(ruledAccs.getIdColumnIndex());
        return _db.select(query).map(row -> _acceptationIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    @Override
    public AcceptationId findRuledAcceptationByRuleAndBaseAcceptation(RuleId rule, AcceptationId baseAcceptation) {
        final LangbookDbSchema.RuledAcceptationsTable ruledAccs = Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final DbQuery query = new DbQueryBuilder(ruledAccs)
                .join(agents, ruledAccs.getAgentColumnIndex(), agents.getIdColumnIndex())
                .where(ruledAccs.getAcceptationColumnIndex(), baseAcceptation)
                .where(ruledAccs.columns().size() + agents.getRuleColumnIndex(), rule)
                .select(ruledAccs.getIdColumnIndex());
        final DbResult dbResult = _db.select(query);
        final AcceptationId result = dbResult.hasNext()? _acceptationIdSetter.getKeyFromDbValue(dbResult.next().get(0)) : null;
        if (dbResult.hasNext()) {
            throw new AssertionError();
        }

        return result;
    }

    boolean isAcceptationStaticallyInBunch(BunchId bunch, AcceptationIdInterface acceptation) {
        final LangbookDbSchema.BunchAcceptationsTable table = Tables.bunchAcceptations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .where(table.getAcceptationColumnIndex(), acceptation)
                .where(table.getAgentColumnIndex(), 0)
                .select(table.getIdColumnIndex());
        try (DbResult result = _db.select(query)) {
            return result.hasNext();
        }
    }

    boolean checkConversionConflictsOnStaticAcceptationsOnly(ConversionProposal<AlphabetId> conversion) {
        final LangbookDbSchema.StringQueriesTable table = Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getStringAlphabetColumnIndex(), conversion.getSourceAlphabet())
                .whereColumnValueMatch(table.getMainAcceptationColumnIndex(), table.getDynamicAcceptationColumnIndex())
                .select(table.getStringColumnIndex());

        return !_db.select(query).anyMatch(row -> conversion.convert(row.get(0).toText()) == null);
    }

    BunchSetId findBunchSet(Set<BunchId> bunches) {
        final LangbookDbSchema.BunchSetsTable table = Tables.bunchSets;
        if (bunches.isEmpty()) {
            return _bunchSetIdSetter.getDeclaredEmpty();
        }

        final DbQuery query = new DbQueryBuilder(table)
                .join(table, table.getSetIdColumnIndex(), table.getSetIdColumnIndex())
                .where(table.getBunchColumnIndex(), bunches.valueAt(0))
                .orderBy(table.getSetIdColumnIndex())
                .select(table.getSetIdColumnIndex(), table.columns().size() + table.getBunchColumnIndex());

        try (DbResult result = _db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                final MutableSet<BunchId> set = MutableHashSet.empty();
                BunchSetId setId = _bunchSetIdSetter.getKeyFromDbValue(row.get(0));
                set.add(_bunchIdSetter.getKeyFromDbValue(row.get(1)));

                while (result.hasNext()) {
                    row = result.next();
                    if (!setId.sameValue(row.get(0))) {
                        if (set.equals(bunches)) {
                            return setId;
                        }

                        setId = _bunchSetIdSetter.getKeyFromDbValue(row.get(0));
                        set.clear();
                    }

                    set.add(_bunchIdSetter.getKeyFromDbValue(row.get(1)));
                }

                if (set.equals(bunches)) {
                    return setId;
                }
            }
        }

        return null;
    }

    MutableMap<RuleId, ConceptId> findRuledConceptsByConceptInvertedMap(ConceptId concept) {
        final LangbookDbSchema.RuledConceptsTable table = Tables.ruledConcepts;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getConceptColumnIndex(), concept)
                .select(table.getIdColumnIndex(), table.getRuleColumnIndex());

        final MutableMap<RuleId, ConceptId> ruledConcepts = MutableHashMap.empty();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final ConceptId ruledConcept = _conceptIdSetter.getKeyFromDbValue(row.get(0));
                final RuleId rule = _ruleIdSetter.getKeyFromDbValue(row.get(1));
                ruledConcepts.put(rule, ruledConcept);
            }
        }

        return ruledConcepts;
    }

    ImmutableMap<ConceptId, ConceptId> findRuledConceptsByRuleInvertedMap(RuleId rule) {
        final LangbookDbSchema.RuledConceptsTable table = Tables.ruledConcepts;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getRuleColumnIndex(), rule)
                .select(table.getIdColumnIndex(), table.getConceptColumnIndex());

        final ImmutableHashMap.Builder<ConceptId, ConceptId> builder = new ImmutableHashMap.Builder<>();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> list = result.next();
                final ConceptId base = _conceptIdSetter.getKeyFromDbValue(list.get(1));
                final ConceptId ruled = _conceptIdSetter.getKeyFromDbValue(list.get(0));
                builder.put(base, ruled);
            }
        }

        return builder.build();
    }

    RuleId getRuleByRuledConcept(ConceptId ruledConcept) {
        final LangbookDbSchema.RuledConceptsTable table = Tables.ruledConcepts;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), ruledConcept)
                .select(table.getRuleColumnIndex());

        final DbValue rawValue = selectOptionalFirstDbValue(query);
        return (rawValue != null)? _ruleIdSetter.getKeyFromDbValue(rawValue) : null;
    }

    private void getAppliedRulesIteration(AcceptationId acceptation, MutableList<RuleId> rules) {
        final LangbookDbSchema.RuledAcceptationsTable table = Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final DbQuery query = new DbQueryBuilder(table)
                .join(agents, table.getAgentColumnIndex(), agents.getIdColumnIndex())
                .where(table.getIdColumnIndex(), acceptation)
                .select(table.getAcceptationColumnIndex(), table.columns().size() + agents.getRuleColumnIndex());

        final List<DbValue> row = selectOptionalSingleRow(query);
        if (row != null) {
            final AcceptationId baseAcceptation = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
            final RuleId rule = _ruleIdSetter.getKeyFromDbValue(row.get(1));
            getAppliedRulesIteration(baseAcceptation, rules);
            rules.append(rule);
        }
    }

    List<RuleId> getAppliedRules(AcceptationId acceptation) {
        final MutableList<RuleId> rules = MutableList.empty();
        getAppliedRulesIteration(acceptation, rules);
        return rules;
    }

    boolean isRuleSentenceMatchPresent(RuleId rule, SentenceId sentence) {
        final LangbookDbSchema.RuleSentenceMatchesTable table = Tables.ruleSentenceMatches;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getRuleColumnIndex(), rule)
                .where(table.getSentenceColumnIndex(), sentence)
                .select(table.getIdColumnIndex());

        try (DbResult dbResult = _db.select(query)) {
            return dbResult.hasNext();
        }
    }

    boolean fillRuledAcceptation(AcceptationId acceptation, RuledAcceptationMutableRegister<AcceptationId, AgentId> register) {
        final LangbookDbSchema.RuledAcceptationsTable table = Tables.ruledAcceptations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), acceptation)
                .select(table.getAgentColumnIndex(), table.getAcceptationColumnIndex());

        try (DbResult dbResult = _db.select(query)) {
            if (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                register.agent = _agentIdSetter.getKeyFromDbValue(row.get(0));
                register.acceptation = _acceptationIdSetter.getKeyFromDbValue(row.get(1));
                return true;
            }
            else {
                return false;
            }
        }
    }

    ImmutableIntSet findAgentsByRule(RuleId rule) {
        final LangbookDbSchema.AgentsTable table = Tables.agents;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getRuleColumnIndex(), rule)
                .select(table.getIdColumnIndex());
        return _db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    QuizId findQuizDefinition(BunchId bunch, int setId) {
        final LangbookDbSchema.QuizDefinitionsTable table = Tables.quizDefinitions;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .where(table.getQuestionFieldsColumnIndex(), setId)
                .select(table.getIdColumnIndex());

        try (DbResult result = _db.select(query)) {
            final QuizId value = result.hasNext()? _quizIdSetter.getKeyFromDbValue(result.next().get(0)) : null;
            if (result.hasNext()) {
                throw new AssertionError("Only one quiz definition expected");
            }
            return value;
        }
    }

    ImmutableSet<AgentId> findAgentsWithoutSourceBunches() {
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final DbQuery query = new DbQuery.Builder(agents)
                .where(agents.getSourceBunchSetColumnIndex(), 0)
                .select(agents.getIdColumnIndex());

        return _db.select(query).map(row -> _agentIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    ImmutableSet<AgentId> findAffectedAgentsByAcceptationCorrelationModification(AcceptationId acceptation) {
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final LangbookDbSchema.BunchSetsTable bunchSets = Tables.bunchSets;
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;

        final int bunchSetOffset = agents.columns().size();
        final int bunchAccOffset = bunchSetOffset + bunchSets.columns().size();
        final DbQuery query = new DbQueryBuilder(agents)
                .join(bunchSets, agents.getSourceBunchSetColumnIndex(), bunchSets.getSetIdColumnIndex())
                .join(bunchAcceptations, bunchSetOffset + bunchSets.getBunchColumnIndex(), bunchAcceptations.getBunchColumnIndex())
                .where(bunchAccOffset + bunchAcceptations.getAgentColumnIndex(), 0)
                .where(bunchAccOffset + bunchAcceptations.getAcceptationColumnIndex(), acceptation)
                .select(agents.getIdColumnIndex());

        return _db.select(query).map(row -> _agentIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    ImmutableSet<QuizId> findQuizzesByBunch(BunchId bunch) {
        final LangbookDbSchema.QuizDefinitionsTable quizzes = Tables.quizDefinitions;
        final DbQuery query = new DbQueryBuilder(quizzes)
                .where(quizzes.getBunchColumnIndex(), bunch)
                .select(quizzes.getIdColumnIndex());

        return _db.select(query).map(row -> _quizIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    Integer findQuestionFieldSet(Iterable<QuestionFieldDetails<AlphabetId, RuleId>> collection) {
        final MutableHashSet<QuestionFieldDetails<AlphabetId, RuleId>> set = MutableHashSet.empty();
        if (collection == null) {
            return null;
        }

        for (QuestionFieldDetails<AlphabetId, RuleId> field : collection) {
            set.add(field);
        }

        if (set.isEmpty()) {
            return null;
        }

        final QuestionFieldDetails<AlphabetId, RuleId> firstField = set.iterator().next();
        final LangbookDbSchema.QuestionFieldSets fieldSets = Tables.questionFieldSets;
        final int columnCount = fieldSets.columns().size();
        final DbQuery query = new DbQueryBuilder(fieldSets)
                .join(fieldSets, fieldSets.getSetIdColumnIndex(), fieldSets.getSetIdColumnIndex())
                .where(fieldSets.getRuleColumnIndex(), firstField.rule)
                .where(fieldSets.getFlagsColumnIndex(), firstField.flags)
                .where(fieldSets.getAlphabetColumnIndex(), firstField.alphabet)
                .select(
                        fieldSets.getSetIdColumnIndex(),
                        columnCount + fieldSets.getAlphabetColumnIndex(),
                        columnCount + fieldSets.getRuleColumnIndex(),
                        columnCount + fieldSets.getFlagsColumnIndex());

        try (DbResult result = _db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                int setId = row.get(0).toInt();
                final MutableSet<QuestionFieldDetails<AlphabetId, RuleId>> foundSet = MutableHashSet.empty();
                RuleId ruleId = _ruleIdSetter.getKeyFromDbValue(row.get(2));
                foundSet.add(new QuestionFieldDetails<>(_alphabetIdSetter.getKeyFromDbValue(row.get(1)), ruleId, row.get(3).toInt()));

                while (result.hasNext()) {
                    row = result.next();
                    if (setId != row.get(0).toInt()) {
                        if (foundSet.equals(set)) {
                            return setId;
                        }
                        else {
                            foundSet.clear();
                            setId = row.get(0).toInt();
                        }
                    }

                    ruleId = _ruleIdSetter.getKeyFromDbValue(row.get(2));
                    foundSet.add(new QuestionFieldDetails<>(_alphabetIdSetter.getKeyFromDbValue(row.get(1)), ruleId, row.get(3).toInt()));
                }

                if (foundSet.equals(set)) {
                    return setId;
                }
            }
        }

        return null;
    }

    BunchSetId getNextAvailableBunchSetId() {
        final LangbookDbSchema.BunchSetsTable table = Tables.bunchSets;
        final int max = getColumnMax(table, table.getSetIdColumnIndex());
        return _bunchSetIdSetter.getKeyFromInt(max + 1);
    }

    int getMaxQuestionFieldSetId() {
        LangbookDbSchema.QuestionFieldSets table = Tables.questionFieldSets;
        return getColumnMax(table, table.getSetIdColumnIndex());
    }

    ImmutableSet<SymbolArrayId> getCorrelationSymbolArrayIds(CorrelationId correlationId) {
        final LangbookDbSchema.CorrelationsTable correlations = Tables.correlations;

        final DbQuery query = new DbQueryBuilder(correlations)
                .where(correlations.getCorrelationIdColumnIndex(), correlationId)
                .select(correlations.getSymbolArrayColumnIndex());
        return _db.select(query).map(row -> _symbolArrayIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    boolean isBunchAcceptationPresentByAgent(AgentId agentId) {
        final LangbookDbSchema.BunchAcceptationsTable table = Tables.bunchAcceptations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .select(table.getIdColumnIndex());

        return selectExistAtLeastOneRow(query);
    }

    MutableMap<BunchSetId, MutableSet<BunchId>> readBunchSetsWhereBunchIsIncluded(BunchId bunch) {
        final LangbookDbSchema.BunchSetsTable table = Tables.bunchSets;
        final DbQuery query = new DbQueryBuilder(table)
                .join(table, table.getSetIdColumnIndex(), table.getSetIdColumnIndex())
                .where(table.getBunchColumnIndex(), bunch)
                .select(table.getSetIdColumnIndex(), table.columns().size() + table.getBunchColumnIndex());
        final MutableMap<BunchSetId, MutableSet<BunchId>> map = MutableHashMap.empty();
        final SyncCacheMap<BunchSetId, MutableSet<BunchId>> cache = new SyncCacheMap<>(map, id -> MutableHashSet.empty());
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final BunchSetId bunchSetId = _bunchSetIdSetter.getKeyFromDbValue(row.get(0));
                final BunchId thisBunch = _bunchIdSetter.getKeyFromDbValue(row.get(1));
                cache.get(bunchSetId).add(thisBunch);
            }
        }

        return map;
    }

    ImmutableMap<AgentId, ImmutableSet<BunchId>> findAffectedAgentsByItsSourceWithTarget(BunchId bunch) {
        final LangbookDbSchema.BunchSetsTable bunchSets = Tables.bunchSets;
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final int offset = bunchSets.columns().size();
        final DbQuery query = new DbQueryBuilder(bunchSets)
                .join(agents, bunchSets.getSetIdColumnIndex(), agents.getSourceBunchSetColumnIndex())
                .where(bunchSets.getBunchColumnIndex(), bunch)
                .select(offset + agents.getIdColumnIndex(), offset + agents.getTargetBunchSetColumnIndex());

        final ImmutableMap.Builder<AgentId, ImmutableSet<BunchId>> builder = new ImmutableHashMap.Builder<>();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                List<DbValue> row = result.next();
                final BunchSetId bunchSetId = _bunchSetIdSetter.getKeyFromDbValue(row.get(1));
                builder.put(_agentIdSetter.getKeyFromDbValue(row.get(0)), getBunchSet(bunchSetId));
            }
        }

        return builder.build();
    }

    ImmutableMap<AgentId, ImmutableSet<BunchId>> findAffectedAgentsByItsDiffWithTarget(BunchId bunch) {
        final LangbookDbSchema.BunchSetsTable bunchSets = Tables.bunchSets;
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final int offset = bunchSets.columns().size();
        final DbQuery query = new DbQueryBuilder(bunchSets)
                .join(agents, bunchSets.getSetIdColumnIndex(), agents.getDiffBunchSetColumnIndex())
                .where(bunchSets.getBunchColumnIndex(), bunch)
                .select(offset + agents.getIdColumnIndex(), offset + agents.getTargetBunchSetColumnIndex());

        final ImmutableMap.Builder<AgentId, ImmutableSet<BunchId>> builder = new ImmutableHashMap.Builder<>();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final BunchSetId bunchSetId = _bunchSetIdSetter.getKeyFromDbValue(row.get(1));
                final AgentId agentId = _agentIdSetter.getKeyFromDbValue(row.get(0));
                builder.put(agentId, getBunchSet(bunchSetId));
            }
        }

        return builder.build();
    }

    private boolean isSymbolArrayUsedInAnySentence(SymbolArrayId symbolArrayId) {
        final LangbookDbSchema.SentencesTable table = Tables.sentences;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSymbolArrayColumnIndex(), symbolArrayId)
                .select(table.getIdColumnIndex());
        return selectExistAtLeastOneRow(query);
    }

    boolean isSymbolArrayInUse(SymbolArrayId symbolArrayId) {
        return isSymbolArrayUsedInAnyCorrelation(symbolArrayId) ||
                isSymbolArrayUsedInAnyConversion(symbolArrayId) ||
                isSymbolArrayUsedInAnySentence(symbolArrayId);
    }

    private boolean isCorrelationUsedInAnyCorrelationArray(CorrelationId correlationId) {
        final LangbookDbSchema.CorrelationArraysTable table = Tables.correlationArrays;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getCorrelationColumnIndex(), correlationId)
                .select(table.getIdColumnIndex());

        return selectExistAtLeastOneRow(query);
    }

    private boolean isCorrelationUsedInAgentMatchers(CorrelationId correlationId) {
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final DbQuery query = new DbQuery.Builder(agents)
                .select(agents.getStartMatcherColumnIndex(), agents.getEndMatcherColumnIndex());

        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                List<DbValue> row = dbResult.next();
                for (int i = 0; i < 2; i++) {
                    if (correlationId.sameValue(row.get(i))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isCorrelationUsedAsAgentAdder(int adderColumn, CorrelationId correlationId) {
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        final LangbookDbSchema.CorrelationArraysTable correlationArrays = Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(agents)
                .join(correlationArrays, adderColumn, correlationArrays.getArrayIdColumnIndex())
                .select(correlationArrays.getCorrelationColumnIndex());

        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                if (correlationId.sameValue(dbResult.next().get(0))) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isCorrelationUsedInAnyAgent(CorrelationId correlationId) {
        if (isCorrelationUsedInAgentMatchers(correlationId)) {
            return true;
        }

        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        return isCorrelationUsedAsAgentAdder(agents.getStartAdderArrayColumnIndex(), correlationId) ||
                isCorrelationUsedAsAgentAdder(agents.getEndAdderArrayColumnIndex(), correlationId);
    }

    boolean isCorrelationInUse(CorrelationId correlationId) {
        return isCorrelationUsedInAnyCorrelationArray(correlationId) || isCorrelationUsedInAnyAgent(correlationId);
    }

    CorrelationArrayId correlationArrayFromAcceptation(AcceptationId accId) {
        final LangbookDbSchema.AcceptationsTable table = Tables.acceptations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), accId)
                .select(table.getCorrelationArrayColumnIndex());
        return _correlationArrayIdSetter.getKeyFromDbValue(selectSingleRow(query).get(0));
    }

    ImmutableSet<ConceptId> getAlphabetAndLanguageConcepts() {
        final LangbookDbSchema.AlphabetsTable table = Tables.alphabets;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex(), table.getLanguageColumnIndex());

        final ImmutableHashSet.Builder<ConceptId> builder = new ImmutableHashSet.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                builder.add(_conceptIdSetter.getKeyFromDbValue(row.get(0)));
                builder.add(_conceptIdSetter.getKeyFromDbValue(row.get(1)));
            }
        }

        return builder.build();
    }

    MutableList<CorrelationId> getCorrelationArray(CorrelationArrayId id) {
        if (id.isEmptyReference()) {
            return MutableList.empty();
        }

        LangbookDbSchema.CorrelationArraysTable table = Tables.correlationArrays;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getArrayIdColumnIndex(), id)
                .select(table.getArrayPositionColumnIndex(), table.getCorrelationColumnIndex());
        final DbResult dbResult = _db.select(query);
        final int arrayLength = dbResult.getRemainingRows();
        final MutableList<CorrelationId> result = MutableList.empty((currentSize, newSize) -> arrayLength);
        for (int i = 0; i < arrayLength; i++) {
            result.append(null);
        }

        try {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int pos = row.get(0).toInt();
                final CorrelationId corr = _correlationIdSetter.getKeyFromDbValue(row.get(1));
                if (result.get(pos) != null) {
                    throw new AssertionError("Malformed correlation array with id " + id);
                }

                result.put(pos, corr);
            }
        }
        finally {
            dbResult.close();
        }

        return result;
    }

    private MutableCorrelation<AlphabetId> readCorrelationArrayTexts(CorrelationArrayId correlationArrayId) {
        MutableMap<AlphabetId, String> texts = MutableHashMap.empty();
        for (CorrelationId correlationId : getCorrelationArray(correlationArrayId)) {
            for (Map.Entry<AlphabetId, String> entry : getCorrelationWithText(correlationId).entries()) {
                final String currentValue = texts.get(entry.key(), "");
                texts.put(entry.key(), currentValue + entry.value());
            }
        }

        return new MutableCorrelation<>(texts);
    }

    boolean includeConvertedTexts(MutableCorrelation<AlphabetId> texts) {
        if (texts.isEmpty()) {
            return false;
        }

        final ImmutableMap<AlphabetId, AlphabetId> conversionMap = getConversionsMap();
        final int conversionCount = conversionMap.size();
        for (Map.Entry<AlphabetId, String> entry : texts.entries().toImmutable()) {
            for (int conversionIndex = 0; conversionIndex < conversionCount; conversionIndex++) {
                if (equal(conversionMap.valueAt(conversionIndex), entry.key())) {
                    final ImmutablePair<AlphabetId, AlphabetId> pair = new ImmutablePair<>(conversionMap.valueAt(conversionIndex), conversionMap.keyAt(conversionIndex));
                    final String convertedText = getConversion(pair).convert(entry.value());
                    if (convertedText == null) {
                        return false;
                    }

                    texts.put(pair.right, convertedText);
                }
            }
        }

        return true;
    }

    Correlation<AlphabetId> readCorrelationArrayTextAndItsAppliedConversions(CorrelationArrayId correlationArrayId) {
        final MutableCorrelation<AlphabetId> texts = readCorrelationArrayTexts(correlationArrayId);
        return includeConvertedTexts(texts)? texts : null;
    }

    boolean isCorrelationArrayInUse(CorrelationArrayId correlationArrayId) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;

        final DbQuery query = new DbQueryBuilder(acceptations)
                .where(acceptations.getCorrelationArrayColumnIndex(), correlationArrayId)
                .select(acceptations.getIdColumnIndex());

        return selectExistAtLeastOneRow(query);
    }

    boolean isBunchSetInUse(BunchSetId setId) {
        final LangbookDbSchema.AgentsTable agents = Tables.agents;

        final DbQuery query = new DbQuery.Builder(agents).select(
                agents.getTargetBunchSetColumnIndex(),
                agents.getSourceBunchSetColumnIndex(),
                agents.getDiffBunchSetColumnIndex());

        return _db.select(query).anyMatch(row -> setId.sameValue(row.get(0)) ||
                setId.sameValue(row.get(1)) || setId.sameValue(row.get(2)));
    }

    ConceptId findConceptComposition(ImmutableSet<ConceptId> concepts) {
        final int conceptCount = concepts.size();
        if (conceptCount == 0) {
            return null;
        }
        else if (conceptCount == 1) {
            return concepts.valueAt(0);
        }

        final LangbookDbSchema.ConceptCompositionsTable table = Tables.conceptCompositions;
        final DbQuery query = new DbQueryBuilder(table)
                .join(table, table.getComposedColumnIndex(), table.getComposedColumnIndex())
                .where(table.getItemColumnIndex(), concepts.valueAt(0))
                .select(table.getComposedColumnIndex(), table.columns().size() + table.getItemColumnIndex());

        final MutableMap<ConceptId, ImmutableSet<ConceptId>> possibleSets = MutableHashMap.empty();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final ConceptId compositionId = _conceptIdSetter.getKeyFromDbValue(row.get(0));
                final ConceptId item = _conceptIdSetter.getKeyFromDbValue(row.get(1));

                final ImmutableSet<ConceptId> set = possibleSets.get(compositionId, ImmutableHashSet.empty());
                possibleSets.put(compositionId, set.add(item));
            }
        }

        final int mapSize = possibleSets.size();
        for (int i = 0; i < mapSize; i++) {
            if (possibleSets.valueAt(i).equalSet(concepts)) {
                return possibleSets.keyAt(i);
            }
        }

        return null;
    }

    /**
     * Return true if the given concept is required for any agent as source, diff, target or rule
     * @param concept Concept to look up
     * @return Whether there is at least one agent that uses the concept as source, target, diff or rule.
     */
    boolean hasAgentsRequiringAcceptation(ConceptId concept) {
        final LangbookDbSchema.AgentsTable agents = Tables.agents;
        DbQuery query = new DbQuery.Builder(agents)
                .select(agents.getRuleColumnIndex(), agents.getTargetBunchSetColumnIndex(), agents.getSourceBunchSetColumnIndex(), agents.getDiffBunchSetColumnIndex());

        final ImmutableIntSetCreator builder = new ImmutableIntSetCreator();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final RuleId rule = _ruleIdSetter.getKeyFromDbValue(row.get(0));

                if (rule != null && equal(rule.getConceptId(), concept)) {
                    return true;
                }

                final int targetBunchSet = row.get(1).toInt();
                final int sourceBunchSet = row.get(2).toInt();
                final int diffBunchSet = row.get(3).toInt();
                builder.add(sourceBunchSet).add(diffBunchSet).add(targetBunchSet);
            }
        }
        final ImmutableIntSet requiredBunchSets = builder.build();

        final LangbookDbSchema.BunchSetsTable bunchSets = Tables.bunchSets;
        query = new DbQueryBuilder(bunchSets)
                .where(bunchSets.getBunchColumnIndex(), concept)
                .select(bunchSets.getSetIdColumnIndex());
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final int bunch = dbResult.next().get(0).toInt();
                if (requiredBunchSets.contains(bunch)) {
                    return true;
                }
            }
        }

        return false;
    }

    boolean isSymbolArrayPresent(SymbolArrayId symbolArray) {
        final LangbookDbSchema.SymbolArraysTable table = Tables.symbolArrays;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), symbolArray)
                .select(table.getIdColumnIndex());

        return selectExistingRow(query);
    }

    ImmutablePair<ImmutableCorrelation<AlphabetId>, AcceptationId> readAcceptationTextsAndMain(AcceptationId acceptation) {
        final LangbookDbSchema.StringQueriesTable table = Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getDynamicAcceptationColumnIndex(), acceptation)
                .select(
                        table.getStringAlphabetColumnIndex(),
                        table.getStringColumnIndex(),
                        table.getMainAcceptationColumnIndex());
        final ImmutableCorrelation.Builder<AlphabetId> builder = new ImmutableCorrelation.Builder<>();
        boolean mainAccSet = false;
        AcceptationId mainAcc = null;
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final AlphabetId alphabet = _alphabetIdSetter.getKeyFromDbValue(row.get(0));
                final String text = row.get(1).toText();

                if (!mainAccSet) {
                    mainAcc = _acceptationIdSetter.getKeyFromDbValue(row.get(2));
                    mainAccSet = true;
                }
                else if (!mainAcc.sameValue(row.get(2))) {
                    throw new AssertionError();
                }

                builder.put(alphabet, text);
            }
        }

        return new ImmutablePair<>(builder.build(), mainAcc);
    }

    AlphabetId readMainAlphabetFromAlphabet(AlphabetId alphabet) {
        final LangbookDbSchema.AlphabetsTable alpTable = alphabets;
        final LangbookDbSchema.LanguagesTable langTable = Tables.languages;
        final DbQuery query = new DbQueryBuilder(alpTable)
                .join(langTable, alpTable.getLanguageColumnIndex(), langTable.getIdColumnIndex())
                .where(alpTable.getIdColumnIndex(), alphabet)
                .select(alpTable.columns().size() + langTable.getMainAlphabetColumnIndex());

        return _alphabetIdSetter.getKeyFromDbValue(selectSingleRow(query).get(0));
    }

    private ImmutableSet<AcceptationId> readAllAcceptations(AlphabetId alphabet) {
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(strings)
                .where(strings.getStringAlphabetColumnIndex(), alphabet)
                .whereColumnValueMatch(strings.getMainAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .select(strings.getDynamicAcceptationColumnIndex());

        return _db.select(query).map(row -> _acceptationIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    private ImmutableSet<AcceptationId> readAllAcceptationsInBunch(AlphabetId alphabet, BunchId bunch) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final DbQuery query = new DbQueryBuilder(bunchAcceptations)
                .join(strings, bunchAcceptations.getAcceptationColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .where(bunchAcceptations.columns().size() + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(bunchAcceptations.getAcceptationColumnIndex());

        return _db.select(query).map(row -> _acceptationIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    private ImmutableSet<AcceptationId> readAllPossibleSynonymOrTranslationAcceptations(AlphabetId alphabet) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int strOffset = acceptations.columns().size() * 2;
        final DbQuery query = new DbQueryBuilder(acceptations)
                .join(acceptations, acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, acceptations.columns().size() + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .whereColumnValueDiffer(acceptations.getIdColumnIndex(), acceptations.columns().size() + acceptations.getIdColumnIndex())
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(acceptations.getIdColumnIndex());
        return _db.select(query).map(row -> _acceptationIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    private ImmutableSet<AcceptationId> readAllPossibleSynonymOrTranslationAcceptationsInBunch(AlphabetId alphabet, BunchId bunch) {
        final LangbookDbSchema.AcceptationsTable acceptations = Tables.acceptations;
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;

        final int accOffset1 = bunchAcceptations.columns().size();
        final int accOffset2 = accOffset1 + acceptations.columns().size();
        final int strOffset = accOffset2 + acceptations.columns().size();

        final DbQuery query = new DbQueryBuilder(bunchAcceptations)
                .join(acceptations, bunchAcceptations.getAcceptationColumnIndex(), acceptations.getIdColumnIndex())
                .join(acceptations, accOffset1 + acceptations.getConceptColumnIndex(), acceptations.getConceptColumnIndex())
                .join(strings, accOffset2 + acceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .whereColumnValueDiffer(accOffset1 + acceptations.getIdColumnIndex(), accOffset2 + acceptations.getIdColumnIndex())
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(accOffset1 + acceptations.getIdColumnIndex());
        return _db.select(query).map(row -> _acceptationIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    private ImmutableSet<AcceptationId> readAllRulableAcceptations(AlphabetId alphabet, RuleId rule) {
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = Tables.agents;

        final int agentOffset = ruledAcceptations.columns().size();
        final int strOffset = agentOffset + agents.columns().size();
        final DbQuery query = new DbQueryBuilder(ruledAcceptations)
                .join(agents, ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .join(strings, ruledAcceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(agentOffset + agents.getRuleColumnIndex(), rule)
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(ruledAcceptations.getAcceptationColumnIndex());
        return _db.select(query).map(row -> _acceptationIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    private ImmutableSet<AcceptationId> readAllRulableAcceptationsInBunch(AlphabetId alphabet, RuleId rule, BunchId bunch) {
        final LangbookDbSchema.BunchAcceptationsTable bunchAcceptations = Tables.bunchAcceptations;
        final LangbookDbSchema.StringQueriesTable strings = Tables.stringQueries;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = Tables.agents;

        final int ruledAccOffset = bunchAcceptations.columns().size();
        final int agentOffset = ruledAccOffset + ruledAcceptations.columns().size();
        final int strOffset = agentOffset + agents.columns().size();
        final DbQuery query = new DbQueryBuilder(bunchAcceptations)
                .join(ruledAcceptations, bunchAcceptations.getAcceptationColumnIndex(), ruledAcceptations.getAcceptationColumnIndex())
                .join(agents, ruledAccOffset + ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .join(strings, ruledAccOffset + ruledAcceptations.getIdColumnIndex(), strings.getDynamicAcceptationColumnIndex())
                .where(bunchAcceptations.getBunchColumnIndex(), bunch)
                .where(agentOffset + agents.getRuleColumnIndex(), rule)
                .where(strOffset + strings.getStringAlphabetColumnIndex(), alphabet)
                .select(bunchAcceptations.getAcceptationColumnIndex());
        return _db.select(query).map(row -> _acceptationIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    private ImmutableSet<AcceptationId> readAllPossibleAcceptationForField(BunchId bunch, QuestionFieldDetails<AlphabetId, RuleId> field) {
        switch (field.getType()) {
            case LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_ACC:
                return bunch.isNoBunchForQuiz()? readAllAcceptations(field.alphabet) : readAllAcceptationsInBunch(field.alphabet, bunch);

            case LangbookDbSchema.QuestionFieldFlags.TYPE_SAME_CONCEPT:
                return bunch.isNoBunchForQuiz()? readAllPossibleSynonymOrTranslationAcceptations(field.alphabet) :
                        readAllPossibleSynonymOrTranslationAcceptationsInBunch(field.alphabet, bunch);

            case LangbookDbSchema.QuestionFieldFlags.TYPE_APPLY_RULE:
                return bunch.isNoBunchForQuiz()? readAllRulableAcceptations(field.alphabet, field.rule) :
                        readAllRulableAcceptationsInBunch(field.alphabet, field.rule, bunch);

            default:
                throw new AssertionError();
        }
    }

    ImmutableSet<AcceptationId> readAllPossibleAcceptations(BunchId bunch, ImmutableSet<QuestionFieldDetails<AlphabetId, RuleId>> fields) {
        final Function<QuestionFieldDetails<AlphabetId, RuleId>, ImmutableSet<AcceptationId>> mapFunc = field -> readAllPossibleAcceptationForField(bunch, field);
        return fields.map(mapFunc).reduce((a, b) -> a.filter(b::contains));
    }

    ImmutableSet<AcceptationId> getAllRuledAcceptationsForAgent(AgentId agentId) {
        final LangbookDbSchema.RuledAcceptationsTable table = Tables.ruledAcceptations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .select(table.getIdColumnIndex());

        return _db.select(query).map(row -> _acceptationIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    ImmutableMap<AcceptationId, AcceptationId> getFilteredAgentProcessedMap(AgentId agentId, Set<AcceptationId> acceptations) {
        final LangbookDbSchema.RuledAcceptationsTable table = Tables.ruledAcceptations;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getAgentColumnIndex(), agentId)
                .select(table.getAcceptationColumnIndex(), table.getIdColumnIndex());

        final ImmutableMap.Builder<AcceptationId, AcceptationId> builder = new ImmutableHashMap.Builder<>();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final AcceptationId acceptation = _acceptationIdSetter.getKeyFromDbValue(row.get(0));
                if (acceptations.contains(acceptation)) {
                    builder.put(acceptation, _acceptationIdSetter.getKeyFromDbValue(row.get(1)));
                }
            }
        }

        return builder.build();
    }

    ImmutableIntValueMap<SentenceSpan<AcceptationId>> getSentenceSpansWithIds(SentenceId sentenceId) {
        final LangbookDbSchema.SpanTable table = Tables.spans;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSentenceIdColumnIndex(), sentenceId)
                .select(table.getIdColumnIndex(), table.getStartColumnIndex(), table.getLengthColumnIndex(), table.getDynamicAcceptationColumnIndex());
        final ImmutableIntValueHashMap.Builder<SentenceSpan<AcceptationId>> builder = new ImmutableIntValueHashMap.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int id = row.get(0).toInt();
                final int start = row.get(1).toInt();
                final int length = row.get(2).toInt();
                final AcceptationId acc = _acceptationIdSetter.getKeyFromDbValue(row.get(3));
                final ImmutableIntRange range = new ImmutableIntRange(start, start + length - 1);
                builder.put(new SentenceSpan<>(range, acc), id);
            }
        }

        return builder.build();
    }

    /**
     * Returns a set for all sentences linked to the given symbolArray.
     */
    ImmutableSet<SentenceId> findSentencesBySymbolArrayId(SymbolArrayId symbolArrayId) {
        final LangbookDbSchema.SentencesTable table = Tables.sentences;

        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getSymbolArrayColumnIndex(), symbolArrayId)
                .select(table.getIdColumnIndex());
        return _db.select(query).map(row -> _sentenceIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    SymbolArrayId getSentenceSymbolArray(SentenceId sentenceId) {
        final LangbookDbSchema.SentencesTable table = Tables.sentences;
        final DbQuery query = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), sentenceId)
                .select(table.getSymbolArrayColumnIndex());
        return _symbolArrayIdSetter.getKeyFromDbValue(selectOptionalFirstDbValue(query));
    }

    /**
     * Finds the unicode char linked to the given identifier.
     * @param characterId Identifier for the character to look up.
     * @return The linked unicode character if any, or {@link sword.langbook3.android.models.CharacterCompositionRepresentation#INVALID_CHARACTER} if the character has no linked character.
     */
    private char getUnicode(CharacterId characterId) {
        final LangbookDbSchema.UnicodeCharactersTable table = Tables.unicodeCharacters;
        final DbQuery dbQuery = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), characterId)
                .select(table.getUnicodeColumnIndex());

        final DbValue dbValue = selectOptionalFirstDbValue(dbQuery);
        return (dbValue != null)? (char) dbValue.toInt() : INVALID_CHARACTER;
    }

    private String getToken(CharacterId characterId) {
        final LangbookDbSchema.CharacterTokensTable table = Tables.characterTokens;
        final DbQuery dbQuery = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), characterId)
                .select(table.getTokenColumnIndex());

        final DbValue dbValue = selectOptionalFirstDbValue(dbQuery);
        return (dbValue != null)? dbValue.toText() : null;
    }

    boolean isCharacterComposition(CharacterId characterId) {
        final LangbookDbSchema.CharacterCompositionsTable table = Tables.characterCompositions;
        final DbQuery dbQuery = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), characterId)
                .select(table.getIdColumnIndex());

        return selectExistAtLeastOneRow(dbQuery);
    }

    private boolean isCharacterCompositionPart(CharacterId characterId, int columnToMatch) {
        final LangbookDbSchema.CharacterCompositionsTable table = Tables.characterCompositions;
        final DbQuery dbQuery = new DbQueryBuilder(table)
                .where(columnToMatch, characterId)
                .select(table.getIdColumnIndex());

        return selectExistAtLeastOneRow(dbQuery);
    }

    private ImmutableSet<CharacterId> findCharacterCompositionForPart(CharacterId characterId, int columnToMatch) {
        final LangbookDbSchema.CharacterCompositionsTable table = Tables.characterCompositions;
        final DbQuery dbQuery = new DbQueryBuilder(table)
                .where(columnToMatch, characterId)
                .select(table.getIdColumnIndex());

        return _db.select(dbQuery).map(row -> _characterIdSetter.getKeyFromDbValue(row.get(0))).toSet().toImmutable();
    }

    private boolean isCharacterCompositionPart(CharacterId characterId) {
        final LangbookDbSchema.CharacterCompositionsTable table = Tables.characterCompositions;
        // TODO: Avoid making 2 database queries whenever the where clause can be x OR y
        return isCharacterCompositionPart(characterId, table.getFirstCharacterColumnIndex()) ||
                isCharacterCompositionPart(characterId, table.getSecondCharacterColumnIndex());
    }

    private CharacterCompositionRepresentation getCharacterCompositionRepresentation(CharacterId partId) {
        final char unicode = getUnicode(partId);
        final String token = (unicode == INVALID_CHARACTER)? getToken(partId) : null;
        return new CharacterCompositionRepresentation(unicode, token);
    }

    private CharacterCompositionPart<CharacterId> getCharacterCompositionPart(CharacterId partId) {
        return new CharacterCompositionPart<>(partId, getCharacterCompositionRepresentation(partId));
    }

    @Override
    public CharacterCompositionEditorModel<CharacterId> getCharacterCompositionDetails(CharacterId characterId) {
        final CharacterCompositionRepresentation representation = getCharacterCompositionRepresentation(characterId);

        final LangbookDbSchema.CharacterCompositionsTable table = Tables.characterCompositions;
        final DbQuery dbQuery = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), characterId)
                .select(
                        table.getFirstCharacterColumnIndex(),
                        table.getSecondCharacterColumnIndex(),
                        table.getCompositionTypeColumnIndex());

        final List<DbValue> row = selectOptionalSingleRow(dbQuery);
        final int compositionType;
        final CharacterCompositionPart<CharacterId> firstPart;
        final CharacterCompositionPart<CharacterId> secondPart;
        if (row != null) {
            final CharacterId first = _characterIdSetter.getKeyFromDbValue(row.get(0));
            final CharacterId second = _characterIdSetter.getKeyFromDbValue(row.get(1));
            compositionType = row.get(2).toInt();
            firstPart = getCharacterCompositionPart(first);
            secondPart = getCharacterCompositionPart(second);
        }
        else {
            compositionType = UNKNOWN_COMPOSITION_TYPE;
            firstPart = null;
            secondPart = null;
        }

        return new CharacterCompositionEditorModel<>(representation, firstPart, secondPart, compositionType);
    }

    @Override
    public CharacterDetailsModel<CharacterId, AcceptationId> getCharacterDetails(CharacterId characterId) {
        final CharacterCompositionRepresentation representation = getCharacterCompositionRepresentation(characterId);

        final LangbookDbSchema.CharacterCompositionsTable table = Tables.characterCompositions;
        final DbQuery dbQuery = new DbQueryBuilder(table)
                .where(table.getIdColumnIndex(), characterId)
                .select(
                        table.getFirstCharacterColumnIndex(),
                        table.getSecondCharacterColumnIndex(),
                        table.getCompositionTypeColumnIndex());

        final List<DbValue> row = selectOptionalSingleRow(dbQuery);
        final int compositionType;
        final CharacterCompositionPart<CharacterId> firstPart;
        final CharacterCompositionPart<CharacterId> secondPart;
        if (row != null) {
            final CharacterId first = _characterIdSetter.getKeyFromDbValue(row.get(0));
            final CharacterId second = _characterIdSetter.getKeyFromDbValue(row.get(1));
            compositionType = row.get(2).toInt();

            firstPart = getCharacterCompositionPart(first);
            secondPart = getCharacterCompositionPart(second);
        }
        else {
            compositionType = UNKNOWN_COMPOSITION_TYPE;
            firstPart = null;
            secondPart = null;
        }

        final ImmutableSet<CharacterId> whereIsFirst = findCharacterCompositionForPart(characterId, Tables.characterCompositions.getFirstCharacterColumnIndex());
        final ImmutableList<CharacterCompositionPart<CharacterId>> asFirst = whereIsFirst.map(chId ->
                new CharacterCompositionPart<>(chId, getCharacterCompositionRepresentation(chId)));

        final ImmutableSet<CharacterId> whereIsSecond = findCharacterCompositionForPart(characterId, Tables.characterCompositions.getSecondCharacterColumnIndex());
        final ImmutableList<CharacterCompositionPart<CharacterId>> asSecond = whereIsSecond.map(chId ->
                new CharacterCompositionPart<>(chId, getCharacterCompositionRepresentation(chId)));

        final ImmutableMap<AcceptationId, CharacterDetailsModel.AcceptationInfo> acceptationsWhereIncluded;
        if (representation.character != INVALID_CHARACTER) {
            final ImmutableList<SearchResult<AcceptationId, RuleId>> results = findAcceptationFromText("" + representation.character, DbQuery.RestrictionStringTypes.CONTAINS, new ImmutableIntRange(0, 49));
            final MutableMap<AcceptationId, CharacterDetailsModel.AcceptationInfo> builder = MutableHashMap.empty();
            for (SearchResult<AcceptationId, RuleId> searchResult : results) {
                builder.put(searchResult.getId(), new CharacterDetailsModel.AcceptationInfo(searchResult.getStr(), searchResult.isDynamic()));
            }

            acceptationsWhereIncluded = builder.toImmutable();
        }
        else {
            acceptationsWhereIncluded = ImmutableHashMap.empty();
        }

        return new CharacterDetailsModel<>(representation, firstPart, secondPart, compositionType, asFirst, asSecond, acceptationsWhereIncluded);
    }

    private CharacterPickerItem<CharacterId> getCharacterPickerItem(char character) {
        final CharacterId id = findCharacter(character);
        boolean isComposition = false;
        boolean isCompositionPart = false;
        if (id != null) {
            isComposition = isCharacterComposition(id);
            isCompositionPart = isCharacterCompositionPart(id);
        }

        return new CharacterPickerItem<>(id, "" + character, isComposition, isCompositionPart);
    }

    @Override
    public ImmutableList<CharacterPickerItem<CharacterId>> getCharacterPickerItems(String items) {
        return StringUtils.stringToCharList(items).map(this::getCharacterPickerItem);
    }
}
