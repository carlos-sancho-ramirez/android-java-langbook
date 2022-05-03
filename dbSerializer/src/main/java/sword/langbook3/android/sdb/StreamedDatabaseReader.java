package sword.langbook3.android.sdb;

import java.io.IOException;
import java.io.InputStream;

import sword.bitstream.InputHuffmanStream;
import sword.bitstream.InputStreamWrapper;
import sword.bitstream.IntDecoder;
import sword.bitstream.NatDecoder;
import sword.bitstream.RangedIntSetDecoder;
import sword.bitstream.huffman.DefinedIntHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.IntHuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;
import sword.bitstream.huffman.RangedIntHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;
import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.IntKeyMap;
import sword.collections.IntList;
import sword.collections.IntPairMap;
import sword.collections.IntResultFunction;
import sword.collections.IntSet;
import sword.collections.IntTraverser;
import sword.collections.List;
import sword.collections.MutableHashMap;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntList;
import sword.collections.MutableIntPairMap;
import sword.collections.MutableIntSet;
import sword.collections.MutableMap;
import sword.collections.MutableSet;
import sword.collections.MutableSortedSet;
import sword.collections.Set;
import sword.collections.SortUtils;
import sword.database.DbExporter;
import sword.database.DbImporter.Database;
import sword.database.DbInsertQuery;
import sword.database.DbInserter;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbValue;
import sword.langbook3.android.collections.SyncCacheIntKeyNonNullValueMap;
import sword.langbook3.android.collections.SyncCacheIntValueMap;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.db.LangbookDbSchema.Tables;
import sword.langbook3.android.sdb.StreamedDatabase0Reader.AgentReadResult;
import sword.langbook3.android.sdb.StreamedDatabase0Reader.CharHuffmanSymbolDiffReader;
import sword.langbook3.android.sdb.StreamedDatabase0Reader.CharReader;
import sword.langbook3.android.sdb.StreamedDatabase0Reader.IntHuffmanSymbolDiffReader;
import sword.langbook3.android.sdb.StreamedDatabase0Reader.IntHuffmanSymbolReader;
import sword.langbook3.android.sdb.StreamedDatabase0Reader.IntReader;
import sword.langbook3.android.sdb.StreamedDatabase0Reader.IntValueDecoder;
import sword.langbook3.android.sdb.StreamedDatabase0Reader.Language;
import sword.langbook3.android.sdb.StreamedDatabase0Reader.SymbolArrayReadResult;
import sword.langbook3.android.sdb.models.AgentRegister;

import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
import static sword.langbook3.android.db.LangbookDbSchema.EMPTY_CORRELATION_ARRAY_ID;
import static sword.langbook3.android.db.LangbookDbSchema.EMPTY_CORRELATION_ID;
import static sword.langbook3.android.sdb.DatabaseInflater.concatenateTexts;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.addDefinition;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.getMaxConcept;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.getSymbolArray;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.obtainSymbolArray;

public final class StreamedDatabaseReader implements StreamedDatabaseReaderInterface {

    private static final int CHARACTER_MAP_GRANULARITY = 64;

    static final NaturalNumberHuffmanTable naturalNumberTable = new NaturalNumberHuffmanTable(8);
    static final RangedIntegerHuffmanTable characterCompositionCoordinateTable = new RangedIntegerHuffmanTable(0, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - 1);

    static ImmutableIntKeyMap<String> getCorrelationWithText(DbExporter.Database db, int correlationId) {
        final LangbookDbSchema.CorrelationsTable correlations = Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbolArrays = Tables.symbolArrays;

        final DbQuery query = new DbQuery.Builder(correlations)
                .join(symbolArrays, correlations.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .where(correlations.getCorrelationIdColumnIndex(), correlationId)
                .select(correlations.getAlphabetColumnIndex(), correlations.columns().size() + symbolArrays.getStrColumnIndex());
        final ImmutableIntKeyMap.Builder<String> builder = new ImmutableIntKeyMap.Builder<>();
        try (DbResult result = db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                builder.put(row.get(0).toInt(), row.get(1).toText());
            }
        }
        return builder.build();
    }

    private static void insertAlphabet(DbInserter db, int id, int language) {
        final LangbookDbSchema.AlphabetsTable table = Tables.alphabets;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getLanguageColumnIndex(), language)
                .build();

        if (db.insert(query) != id) {
            throw new AssertionError();
        }
    }

    private static void insertLanguage(DbInserter db, int id, String code, int mainAlphabet) {
        final LangbookDbSchema.LanguagesTable table = Tables.languages;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getCodeColumnIndex(), code)
                .put(table.getMainAlphabetColumnIndex(), mainAlphabet)
                .build();
        db.insert(query);
    }

    private static void insertCorrelationEntry(DbInserter db, int correlationId, int alphabet, int symbolArray) {
        final LangbookDbSchema.CorrelationsTable table = Tables.correlations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getCorrelationIdColumnIndex(), correlationId)
                .put(table.getAlphabetColumnIndex(), alphabet)
                .put(table.getSymbolArrayColumnIndex(), symbolArray)
                .build();
        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    private static void insertCorrelation(DbInserter db, int correlationId, IntPairMap correlation) {
        final int mapLength = correlation.size();
        if (mapLength == 0) {
            throw new IllegalArgumentException();
        }

        for (int i = 0; i < mapLength; i++) {
            insertCorrelationEntry(db, correlationId, correlation.keyAt(i), correlation.valueAt(i));
        }
    }

    private static void insertCorrelationArray(DbInserter db, int arrayId, IntList array) {
        final IntTraverser iterator = array.iterator();
        if (!array.iterator().hasNext()) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.CorrelationArraysTable table = Tables.correlationArrays;
        for (int i = 0; iterator.hasNext(); i++) {
            final int correlation = iterator.next();
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getArrayIdColumnIndex(), arrayId)
                    .put(table.getArrayPositionColumnIndex(), i)
                    .put(table.getCorrelationColumnIndex(), correlation)
                    .build();

            if (db.insert(query) == null) {
                throw new AssertionError();
            }
        }
    }

    private static void insertConversion(DbInserter db, int sourceAlphabet, int targetAlphabet, int source, int target) {
        final LangbookDbSchema.ConversionsTable table = Tables.conversions;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getSourceAlphabetColumnIndex(), sourceAlphabet)
                .put(table.getTargetAlphabetColumnIndex(), targetAlphabet)
                .put(table.getSourceColumnIndex(), source)
                .put(table.getTargetColumnIndex(), target)
                .build();
        db.insert(query);
    }

    static int insertAcceptation(DbInserter db, int concept, int correlationArray) {
        final LangbookDbSchema.AcceptationsTable table = Tables.acceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getConceptColumnIndex(), concept)
                .put(table.getCorrelationArrayColumnIndex(), correlationArray)
                .build();
        return db.insert(query);
    }

    static void insertBunchAcceptation(DbInserter db, int bunch, int acceptation, int agent) {
        final LangbookDbSchema.BunchAcceptationsTable table = Tables.bunchAcceptations;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getBunchColumnIndex(), bunch)
                .put(table.getAcceptationColumnIndex(), acceptation)
                .put(table.getAgentColumnIndex(), agent)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static void insertCharacterCompositionDefinition(DbInserter db, int id,
            int firstX, int firstY, int firstWidth, int firstHeight,
            int secondX, int secondY, int secondWidth, int secondHeight) {
        final LangbookDbSchema.CharacterCompositionDefinitionsTable table = Tables.characterCompositionDefinitions;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getFirstXColumnIndex(), firstX)
                .put(table.getFirstYColumnIndex(), firstY)
                .put(table.getFirstWidthColumnIndex(), firstWidth)
                .put(table.getFirstHeightColumnIndex(), firstHeight)
                .put(table.getSecondXColumnIndex(), secondX)
                .put(table.getSecondYColumnIndex(), secondY)
                .put(table.getSecondWidthColumnIndex(), secondWidth)
                .put(table.getSecondHeightColumnIndex(), secondHeight)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    static void insertCharacterComposition(DbInserter db, int id,
            int first, int second, int typeId) {
        final LangbookDbSchema.CharacterCompositionsTable table = Tables.characterCompositions;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), id)
                .put(table.getFirstCharacterColumnIndex(), first)
                .put(table.getSecondCharacterColumnIndex(), second)
                .put(table.getCompositionTypeColumnIndex(), typeId)
                .build();

        if (db.insert(query) == null) {
            throw new AssertionError();
        }
    }

    private static void insertBunchSet(DbInserter db, int setId, IntSet bunches) {
        if (bunches.isEmpty()) {
            throw new IllegalArgumentException();
        }

        final LangbookDbSchema.BunchSetsTable table = Tables.bunchSets;
        for (int bunch : bunches) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getSetIdColumnIndex(), setId)
                    .put(table.getBunchColumnIndex(), bunch)
                    .build();

            if (db.insert(query) == null) {
                throw new AssertionError();
            }
        }
    }

    private static Integer insertAgent(DbInserter db, AgentRegister register) {
        final LangbookDbSchema.AgentsTable table = Tables.agents;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getTargetBunchSetColumnIndex(), register.targetBunchSetId)
                .put(table.getSourceBunchSetColumnIndex(), register.sourceBunchSetId)
                .put(table.getDiffBunchSetColumnIndex(), register.diffBunchSetId)
                .put(table.getStartMatcherColumnIndex(), register.startMatcherId)
                .put(table.getStartAdderArrayColumnIndex(), register.startAdderId)
                .put(table.getEndMatcherColumnIndex(), register.endMatcherId)
                .put(table.getEndAdderArrayColumnIndex(), register.endAdderId)
                .put(table.getRuleColumnIndex(), register.rule)
                .build();
        return db.insert(query);
    }

    private static void insertSentenceWithId(DbInserter db, int sentenceId, int concept, int symbolArray) {
        final LangbookDbSchema.SentencesTable table = Tables.sentences;
        final DbInsertQuery query = new DbInsertQuery.Builder(table)
                .put(table.getIdColumnIndex(), sentenceId)
                .put(table.getConceptColumnIndex(), concept)
                .put(table.getSymbolArrayColumnIndex(), symbolArray)
                .build();

        db.insert(query);
    }

    private final Database _db;
    private final InputStream _is;
    private final ProgressListener _listener;

    /**
     * Prepares the reader with the given parameters.
     *
     * @param db It is assumed to be a database where all the tables and indexes has been
     *           initialized according to the {@link sword.langbook3.android.db.LangbookDbSchema},
     *           but they are empty.
     * @param is Input stream for the file to be read.
     *           This input stream should not be in the first position of the file, but 20 bytes
     *           after, skipping the header and hash.
     * @param listener Optional callback to display in the UI the current state. This can be null.
     */
    public StreamedDatabaseReader(Database db, InputStream is, ProgressListener listener) {
        _db = db;
        _is = is;
        _listener = listener;
    }

    private void setProgress(float progress, String message) {
        if (_listener != null) {
            _listener.setProgress(progress, message);
        }
    }

    private SymbolArrayReadResult readSymbolArrays(InputHuffmanStream ibs, MutableSet<Character> characters) throws IOException {
        final int symbolArraysLength = ibs.readHuffmanSymbol(naturalNumberTable);
        if (symbolArraysLength == 0) {
            final int[] emptyArray = new int[0];
            return new SymbolArrayReadResult(emptyArray, emptyArray);
        }

        final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
        final NaturalNumberHuffmanTable nat4Table = new NaturalNumberHuffmanTable(4);

        final HuffmanTable<Character> charHuffmanTable =
                ibs.readHuffmanTable(new CharReader(ibs), new CharHuffmanSymbolDiffReader(ibs, nat4Table));

        final IntHuffmanTable symbolArraysLengthTable =
                ibs.readIntHuffmanTable(new IntReader(ibs), new IntHuffmanSymbolDiffReader(ibs, nat3Table));

        final int[] idMap = new int[symbolArraysLength];
        final int[] lengths = new int[symbolArraysLength];
        for (int index = 0; index < symbolArraysLength; index++) {
            final int length = ibs.readIntHuffmanSymbol(symbolArraysLengthTable);
            final StringBuilder builder = new StringBuilder();
            for (int pos = 0; pos < length; pos++) {
                final Character ch = ibs.readHuffmanSymbol(charHuffmanTable);
                builder.append(ch);
                characters.add(ch);
            }

            idMap[index] = obtainSymbolArray(_db, builder.toString());
            lengths[index] = length;
        }

        return new SymbolArrayReadResult(idMap, lengths);
    }

    private Conversion[] readConversions(InputHuffmanStream ibs, ImmutableIntRange validAlphabets, int minSymbolArrayIndex, int maxSymbolArrayIndex, int[] symbolArraysIdMap) throws IOException {
        final int conversionsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final Conversion[] conversions = new Conversion[conversionsLength];
        final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(minSymbolArrayIndex, maxSymbolArrayIndex);

        final int minValidAlphabet = validAlphabets.min();
        final int maxValidAlphabet = validAlphabets.max();
        int minSourceAlphabet = validAlphabets.min();
        int minTargetAlphabet = minValidAlphabet;
        for (int i = 0; i < conversionsLength; i++) {
            final RangedIntegerHuffmanTable sourceAlphabetTable = new RangedIntegerHuffmanTable(minSourceAlphabet, maxValidAlphabet);
            final int sourceAlphabet = ibs.readHuffmanSymbol(sourceAlphabetTable);

            if (minSourceAlphabet != sourceAlphabet) {
                minTargetAlphabet = minValidAlphabet;
                minSourceAlphabet = sourceAlphabet;
            }

            final RangedIntegerHuffmanTable targetAlphabetTable = new RangedIntegerHuffmanTable(minTargetAlphabet, maxValidAlphabet);
            final int targetAlphabet = ibs.readHuffmanSymbol(targetAlphabetTable);
            minTargetAlphabet = targetAlphabet + 1;

            final int pairCount = ibs.readHuffmanSymbol(naturalNumberTable);
            final MutableMap<String, String> conversionMap = MutableHashMap.empty();
            for (int j = 0; j < pairCount; j++) {
                final int source = symbolArraysIdMap[ibs.readHuffmanSymbol(symbolArrayTable)];
                final int target = symbolArraysIdMap[ibs.readHuffmanSymbol(symbolArrayTable)];
                insertConversion(_db, sourceAlphabet, targetAlphabet, source, target);

                conversionMap.put(getSymbolArray(_db, source), getSymbolArray(_db, target));
            }

            conversions[i] = new Conversion(sourceAlphabet, targetAlphabet, conversionMap);
        }

        return conversions;
    }

    private int[] readCorrelations(InputStreamWrapper ibs, ImmutableIntRange validAlphabets, int[] symbolArraysIdMap) throws IOException {
        final int correlationsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final int[] result = new int[correlationsLength];
        if (correlationsLength > 0) {
            final RangedIntHuffmanTable symbolArrayTable = new RangedIntHuffmanTable(new ImmutableIntRange(0, symbolArraysIdMap.length - 1));
            final IntDecoder intDecoder = new IntDecoder(ibs);
            final IntHuffmanTable lengthTable = ibs.readIntHuffmanTable(intDecoder, intDecoder);
            final RangedIntSetDecoder keyDecoder = new RangedIntSetDecoder(ibs, lengthTable, validAlphabets);
            final IntValueDecoder valueDecoder = new IntValueDecoder(ibs, symbolArrayTable);

            for (int i = 0; i < correlationsLength; i++) {
                IntPairMap corrMap = ibs.readIntPairMap(keyDecoder, keyDecoder, keyDecoder, valueDecoder);
                MutableIntPairMap corr = MutableIntPairMap.empty();
                for (IntPairMap.Entry entry : corrMap.entries()) {
                    corr.put(entry.key(), symbolArraysIdMap[entry.value()]);
                }

                final int correlationId;
                if (corr.size() == 0) {
                    correlationId = EMPTY_CORRELATION_ID;
                }
                else {
                    correlationId = i + 1;
                    insertCorrelation(_db, correlationId, corr);
                }
                result[i] = correlationId;
            }
        }

        return result;
    }

    private int[] readCorrelationArrays(InputHuffmanStream ibs, int[] correlationIdMap) throws IOException {
        final int arraysLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final int[] result = new int[arraysLength];
        if (arraysLength > 0) {
            final RangedIntegerHuffmanTable correlationTable = new RangedIntegerHuffmanTable(0, correlationIdMap.length - 1);
            final IntDecoder intDecoder = new IntDecoder(ibs);
            final IntHuffmanTable lengthTable = ibs.readIntHuffmanTable(intDecoder, intDecoder);

            for (int i = 0; i < arraysLength; i++) {
                final int arrayLength = ibs.readIntHuffmanSymbol(lengthTable);

                final int arrayId;
                if (arrayLength == 0) {
                    arrayId = EMPTY_CORRELATION_ARRAY_ID;
                }
                else {
                    final ImmutableIntList.Builder builder = new ImmutableIntList.Builder();
                    for (int j = 0; j < arrayLength; j++) {
                        builder.add(correlationIdMap[ibs.readHuffmanSymbol(correlationTable)]);
                    }

                    arrayId = i + 1;
                    insertCorrelationArray(_db, arrayId, builder.build());
                }
                result[i] = arrayId;
            }
        }

        return result;
    }

    private int[] readAcceptations(InputStreamWrapper ibs, ImmutableIntRange validConcepts, int[] correlationArrayIdMap) throws IOException {
        final int acceptationsLength = ibs.readHuffmanSymbol(naturalNumberTable);

        final int[] acceptationsIdMap = new int[acceptationsLength];
        if (acceptationsLength > 0) {
            final IntDecoder intDecoder = new IntDecoder(ibs);
            final IntHuffmanTable corrArraySetLengthTable = ibs.readIntHuffmanTable(intDecoder, intDecoder);
            final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(validConcepts.min(), validConcepts.max());
            for (int i = 0; i < acceptationsLength; i++) {
                final int concept = ibs.readHuffmanSymbol(conceptTable);
                final ImmutableIntRange range = new ImmutableIntRange(0, correlationArrayIdMap.length - 1);
                final RangedIntSetDecoder decoder = new RangedIntSetDecoder(ibs, corrArraySetLengthTable, range);
                for (int corrArray : ibs.readIntSet(decoder, decoder, decoder)) {
                    // TODO: Separate acceptations and correlations in 2 tables to avoid overlapping if there is more than one correlation array
                    acceptationsIdMap[i] = insertAcceptation(_db, concept, correlationArrayIdMap[corrArray]);
                }
            }
        }

        return acceptationsIdMap;
    }

    private void readComplementedConcepts(InputStreamWrapper ibs, ImmutableIntRange validConcepts) throws IOException {
        final int bunchConceptsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final NatDecoder natDecoder = new NatDecoder(ibs);
        final IntHuffmanTable bunchConceptsLengthTable = (bunchConceptsLength > 0)?
                ibs.readIntHuffmanTable(natDecoder, natDecoder) : null;

        int minBunchConcept = validConcepts.min();
        final int maxValidBunch = validConcepts.max();
        for (int maxBunchConcept = validConcepts.max() - bunchConceptsLength + 1; maxBunchConcept <= maxValidBunch; maxBunchConcept++) {
            final int base = ibs.readHuffmanSymbol(new RangedIntegerHuffmanTable(minBunchConcept, maxBunchConcept));
            minBunchConcept = base + 1;

            final RangedIntSetDecoder decoder = new RangedIntSetDecoder(ibs, bunchConceptsLengthTable, validConcepts);
            final IntKeyMap<ImmutableIntSet> map = ibs.readIntKeyMap(decoder, decoder, decoder, () -> {
                final MutableIntSet complements = MutableIntArraySet.empty();
                int minValidConcept = validConcepts.min();
                while (minValidConcept < validConcepts.max() && ibs.readBoolean()) {
                    final int concept = ibs.readHuffmanSymbol(new RangedIntegerHuffmanTable(minValidConcept, validConcepts.max()));
                    complements.add(concept);
                    minValidConcept = concept + 1;
                }

                return complements.toImmutable();
            });

            for (IntKeyMap.Entry<ImmutableIntSet> entry : map.entries()) {
                addDefinition(_db, base, entry.key(), entry.value());
            }
        }
    }

    private void readCharacterCompositionDefinitions(InputStreamWrapper ibs, ImmutableIntRange validConcepts, MutableIntList definitionIds) throws IOException {
        final int definitionsCount = ibs.readHuffmanSymbol(naturalNumberTable);
        int firstValidConcept = validConcepts.min();
        for (int definitionIndex = 0; definitionIndex < definitionsCount; definitionIndex++) {
            final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(firstValidConcept, validConcepts.max());
            final int definitionId = ibs.readHuffmanSymbol(conceptTable);

            final int firstX = ibs.readHuffmanSymbol(characterCompositionCoordinateTable);
            final int firstWidth = ibs.readHuffmanSymbol(new RangedIntegerHuffmanTable(1, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - firstX));

            final int firstY = ibs.readHuffmanSymbol(characterCompositionCoordinateTable);
            final int firstHeight = ibs.readHuffmanSymbol(new RangedIntegerHuffmanTable(1, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - firstY));

            final int secondX = ibs.readHuffmanSymbol(characterCompositionCoordinateTable);
            final int secondWidth = ibs.readHuffmanSymbol(new RangedIntegerHuffmanTable(1, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - secondX));

            final int secondY = ibs.readHuffmanSymbol(characterCompositionCoordinateTable);
            final int secondHeight = ibs.readHuffmanSymbol(new RangedIntegerHuffmanTable(1, CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT - secondY));

            insertCharacterCompositionDefinition(_db, definitionId, firstX, firstY, firstWidth, firstHeight, secondX, secondY, secondWidth, secondHeight);

            firstValidConcept = definitionId + 1;
            definitionIds.append(definitionId);
        }
    }

    private void readCharacterCompositions(InputStreamWrapper ibs, Set<Character> characters, IntList definitionDbIds) throws IOException {
        final int compositionsCount = ibs.readHuffmanSymbol(naturalNumberTable);
        if (compositionsCount > 0) {
            final int missingCharactersCount = ibs.readHuffmanSymbol(naturalNumberTable);
            final MutableSet<Character> missingCharacters = MutableHashSet.empty();
            if (missingCharactersCount > 0) {
                int firstValidCharacter = 0;
                for (int i = 0; i < missingCharactersCount; i++) {
                    final int intCh = ibs.readHuffmanSymbol(naturalNumberTable) + firstValidCharacter;
                    missingCharacters.add((char) intCh);
                    firstValidCharacter = intCh + 1;
                }

                insertCharacters(missingCharacters, characters.size());
            }

            final int tokenCount = ibs.readHuffmanSymbol(naturalNumberTable);
            final MutableSortedSet<String> tokens = MutableSortedSet.empty(SortUtils::compareCharSequenceByUnicode);
            if (tokenCount > 0) {
                final RangedIntegerHuffmanTable tokenCharacterTable = new RangedIntegerHuffmanTable(0, 53);
                String previousToken = null;
                String token = "";
                while (tokens.size() < tokenCount) {
                    final RangedIntegerHuffmanTable huffmanTable;
                    if (previousToken != null && token.length() < previousToken.length()) {
                        final char prevCh = previousToken.charAt(token.length());
                        final int prevChInt = (prevCh == ' ')? 0 : (prevCh >= 'A' && prevCh <= 'Z')? prevCh - 'A' + 1 : prevCh - 'a' + 27;
                        huffmanTable = new RangedIntegerHuffmanTable(prevChInt, 53);
                    }
                    else {
                        previousToken = null;
                        huffmanTable = tokenCharacterTable;
                    }

                    final int chInt = ibs.readHuffmanSymbol(huffmanTable);
                    if (chInt == 53) {
                        tokens.add(token);
                        previousToken = token;
                        token = "";
                    }
                    else {
                        final char ch = (chInt == 0) ? ' ' : (chInt <= 26) ? (char) (chInt + 'A' - 1) : (char) (chInt + 'a' - 27);
                        if (previousToken != null && previousToken.charAt(token.length()) != ch) {
                            previousToken = null;
                        }
                        token += ch;
                    }
                }

                insertTokens(tokens, characters.size() + missingCharactersCount);
            }

            final int lastValidCharFileId = characters.size() + missingCharactersCount + tokenCount - 1;
            final RangedIntegerHuffmanTable charactersTable = new RangedIntegerHuffmanTable(0, lastValidCharFileId);
            final RangedIntegerHuffmanTable definitionsTable = new RangedIntegerHuffmanTable(0, definitionDbIds.size() - 1);

            int firstValidCharFileId = 0;
            for (int compositionIndex = 0; compositionIndex < compositionsCount; compositionIndex++) {
                final RangedIntegerHuffmanTable charFileIdTable = new RangedIntegerHuffmanTable(firstValidCharFileId, lastValidCharFileId);
                final int id = ibs.readHuffmanSymbol(charFileIdTable) + 1;
                firstValidCharFileId = id;

                final int first = ibs.readHuffmanSymbol(charactersTable) + 1;
                final int second = ibs.readHuffmanSymbol(charactersTable) + 1;
                final int definition = ibs.readHuffmanSymbol(definitionsTable);

                insertCharacterComposition(_db, id, first, second, definitionDbIds.valueAt(definition));
            }
        }
    }

    private void readBunchAcceptations(InputStreamWrapper ibs, ImmutableIntRange validConcepts, int[] acceptationsIdMap) throws IOException {
        final int bunchAcceptationsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final NatDecoder natDecoder = new NatDecoder(ibs);
        final IntHuffmanTable bunchAcceptationsLengthTable = (bunchAcceptationsLength > 0)?
                ibs.readIntHuffmanTable(natDecoder, natDecoder) : null;

        int minBunch = validConcepts.min();
        final int maxValidBunch = validConcepts.max();
        for (int maxBunch = validConcepts.max() - bunchAcceptationsLength + 1; maxBunch <= maxValidBunch; maxBunch++) {
            final int bunch = ibs.readHuffmanSymbol(new RangedIntegerHuffmanTable(minBunch, maxBunch));
            minBunch = bunch + 1;

            final ImmutableIntRange range = new ImmutableIntRange(0, acceptationsIdMap.length - 1);
            final RangedIntSetDecoder decoder = new RangedIntSetDecoder(ibs, bunchAcceptationsLengthTable, range);
            for (int acceptation : ibs.readIntSet(decoder, decoder, decoder)) {
                insertBunchAcceptation(_db, bunch, acceptationsIdMap[acceptation], 0);
            }
        }
    }

    private ImmutableIntKeyMap<String> getCorrelation(int correlationId) {
        if (correlationId == LangbookDbSchema.EMPTY_CORRELATION_ID) {
            return ImmutableIntKeyMap.empty();
        }

        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final LangbookDbSchema.SymbolArraysTable symbolArrays = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
                .join(symbolArrays, table.getSymbolArrayColumnIndex(), symbolArrays.getIdColumnIndex())
                .where(table.getCorrelationIdColumnIndex(), correlationId)
                .select(table.getAlphabetColumnIndex(), table.columns().size() + symbolArrays.getStrColumnIndex());

        final MutableIntKeyMap<String> result = MutableIntKeyMap.empty();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                result.put(row.get(0).toInt(), row.get(1).toText());
            }
        }

        return result.toImmutable();
    }

    private ImmutableIntList getCorrelationArray(int arrayId) {
        if (arrayId == LangbookDbSchema.EMPTY_CORRELATION_ARRAY_ID) {
            return ImmutableIntList.empty();
        }

        final LangbookDbSchema.CorrelationArraysTable correlationArrays = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(correlationArrays)
                .where(correlationArrays.getArrayIdColumnIndex(), arrayId)
                .orderBy(correlationArrays.getArrayPositionColumnIndex())
                .select(correlationArrays.getCorrelationColumnIndex());

        return _db.select(query).mapToInt(row -> row.get(0).toInt()).toList().toImmutable();
    }

    private AgentReadResult readAgents(
            InputStreamWrapper ibs, ImmutableIntRange validConcepts, int[] correlationIdMap, int[] correlationArrayIdMap) throws IOException {

        final int agentsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final ImmutableIntKeyMap.Builder<AgentBunches> builder = new ImmutableIntKeyMap.Builder<>();
        final MutableIntPairMap agentRules = MutableIntPairMap.empty();

        if (agentsLength > 0) {
            final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
            final IntHuffmanSymbolReader intHuffmanSymbolReader = new IntHuffmanSymbolReader(ibs, nat3Table);
            final IntHuffmanTable bunchSetLengthTable = ibs.readIntHuffmanTable(intHuffmanSymbolReader, null);

            int minTarget = validConcepts.min();
            int minSource = validConcepts.min();
            int minDiff = validConcepts.min();
            final SyncCacheIntValueMap<ImmutableIntSet> insertedBunchSets = new SyncCacheIntValueMap<>(new IntResultFunction<ImmutableIntSet>() {
                private int lastAssignedKey = LangbookDbSchema.Tables.bunchSets.nullReference();

                @Override
                public int apply(ImmutableIntSet bunchSet) {
                    if (bunchSet.isEmpty()) {
                        return LangbookDbSchema.Tables.bunchSets.nullReference();
                    }
                    else {
                        insertBunchSet(_db, ++lastAssignedKey, bunchSet);
                        return lastAssignedKey;
                    }
                }
            });

            final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(validConcepts.min(), validConcepts.max());
            final RangedIntegerHuffmanTable correlationTable = new RangedIntegerHuffmanTable(0, correlationIdMap.length - 1);
            final RangedIntegerHuffmanTable correlationArrayTable = new RangedIntegerHuffmanTable(0, correlationArrayIdMap.length - 1);

            final SyncCacheIntKeyNonNullValueMap<ImmutableIntKeyMap<String>> correlationSyncCache = new SyncCacheIntKeyNonNullValueMap<>(this::getCorrelation);
            final SyncCacheIntKeyNonNullValueMap<ImmutableIntList> correlationArraysSyncCache = new SyncCacheIntKeyNonNullValueMap<>(this::getCorrelationArray);

            ImmutableIntSet lastTargets = ImmutableIntArraySet.empty();
            ImmutableIntSet lastSources = ImmutableIntArraySet.empty();
            for (int i = 0; i < agentsLength; i++) {
                setProgress((0.99f - 0.8f) * i / ((float) agentsLength) + 0.8f, "Reading agent " + (i + 1) + "/" + agentsLength);
                final ImmutableIntRange targetRange = new ImmutableIntRange(minTarget, validConcepts.max());
                final RangedIntSetDecoder targetDecoder = new RangedIntSetDecoder(ibs, bunchSetLengthTable, targetRange);
                final ImmutableIntSet targetBunches = ibs.readIntSet(targetDecoder, targetDecoder, targetDecoder).toImmutable();

                if (!targetBunches.equalSet(lastTargets)) {
                    minSource = validConcepts.min();
                    minDiff = validConcepts.min();
                    lastSources = ImmutableIntArraySet.empty();
                }

                if (!targetBunches.isEmpty()) {
                    minTarget = targetBunches.min();
                }
                lastTargets = targetBunches;

                final ImmutableIntRange sourceRange = new ImmutableIntRange(minSource, validConcepts.max());
                final RangedIntSetDecoder sourceDecoder = new RangedIntSetDecoder(ibs, bunchSetLengthTable, sourceRange);
                final ImmutableIntSet sourceBunches = ibs.readIntSet(sourceDecoder, sourceDecoder, sourceDecoder).toImmutable();

                if (!sourceBunches.equalSet(lastSources)) {
                    minDiff = validConcepts.min();
                }

                if (!sourceBunches.isEmpty()) {
                    minSource = sourceBunches.min();
                }
                lastSources = sourceBunches;

                final ImmutableIntRange diffRange = new ImmutableIntRange(minDiff, validConcepts.max());
                final RangedIntSetDecoder diffDecoder = new RangedIntSetDecoder(ibs, bunchSetLengthTable, diffRange);
                final ImmutableIntSet diffBunches = ibs.readIntSet(diffDecoder, diffDecoder, diffDecoder).toImmutable();

                if (!diffBunches.isEmpty()) {
                    minDiff = diffBunches.min();
                }

                final int startMatcherId = correlationIdMap[ibs.readHuffmanSymbol(correlationTable)];
                final int startAdderArrayId = correlationArrayIdMap[ibs.readHuffmanSymbol(correlationArrayTable)];
                final int endMatcherId = correlationIdMap[ibs.readHuffmanSymbol(correlationTable)];
                final int endAdderArrayId = correlationArrayIdMap[ibs.readHuffmanSymbol(correlationArrayTable)];

                final ImmutableIntKeyMap<String> startMatcher = correlationSyncCache.get(startMatcherId);
                final ImmutableIntKeyMap<String> plainStartAdder = concatenateTexts(correlationArraysSyncCache.get(startAdderArrayId).map(correlationSyncCache::get));
                final ImmutableIntKeyMap<String> endMatcher = correlationSyncCache.get(endMatcherId);
                final ImmutableIntKeyMap<String> plainEndAdder = concatenateTexts(correlationArraysSyncCache.get(endAdderArrayId).map(correlationSyncCache::get));

                final boolean hasRule = !startMatcher.equalMap(plainStartAdder) || !endMatcher.equalMap(plainEndAdder);
                final int rule = hasRule?
                        ibs.readHuffmanSymbol(conceptTable) :
                        StreamedDatabaseConstants.nullRuleId;

                final int targetBunchSetId = insertedBunchSets.get(targetBunches);
                final int sourceBunchSetId = insertedBunchSets.get(sourceBunches);
                final int diffBunchSetId = insertedBunchSets.get(diffBunches);

                final AgentRegister register = new AgentRegister(targetBunchSetId, sourceBunchSetId, diffBunchSetId, startMatcherId, startAdderArrayId, endMatcherId, endAdderArrayId, rule);
                final int agentId = insertAgent(_db, register);

                builder.put(agentId, new AgentBunches(targetBunches, sourceBunches, diffBunches));

                if (hasRule) {
                    agentRules.put(agentId, rule);
                }
            }
        }

        return new AgentReadResult(builder.build(), agentRules.toImmutable());
    }

    private AgentAcceptationPair[] readRelevantRuledAcceptations(InputHuffmanStream ibs, int[] accIdMap, ImmutableIntSet agentsWithRule) throws IOException {
        final int pairsCount = ibs.readHuffmanSymbol(naturalNumberTable);
        final AgentAcceptationPair[] pairs = new AgentAcceptationPair[pairsCount];
        if (pairsCount > 0) {
            final int maxAcc = accIdMap.length + pairsCount - 1;
            final int maxAgentsWithRule = agentsWithRule.size() - 1;
            final RangedIntegerHuffmanTable agentTable = new RangedIntegerHuffmanTable(0, maxAgentsWithRule);
            final RangedIntegerHuffmanTable mainAcceptationTable = new RangedIntegerHuffmanTable(0, maxAcc);
            for (int pairIndex = 0; pairIndex < pairsCount; pairIndex++) {
                final int agentWithRuleIndex = ibs.readHuffmanSymbol(agentTable);
                final int agent = agentsWithRule.valueAt(agentWithRuleIndex);
                int baseAccIndex = ibs.readHuffmanSymbol(mainAcceptationTable);

                pairs[pairIndex] = new AgentAcceptationPair(agent, baseAccIndex);
            }
        }

        return pairs;
    }

    private SentenceSpan[] readSentenceSpans(InputHuffmanStream ibs, int extendedAccCount, int[] symbolArrayIdMap, int[] symbolArrayLengths) throws IOException {
        final int maxSymbolArray = symbolArrayLengths.length - 1;
        final int spanCount = ibs.readHuffmanSymbol(naturalNumberTable);
        final SentenceSpan[] spans = new SentenceSpan[spanCount];
        if (spanCount > 0) {
            final RangedIntegerHuffmanTable accTable = new RangedIntegerHuffmanTable(0, extendedAccCount - 1);
            int previousSymbolArray = 0;
            int previousStart = 0;

            final MutableIntPairMap sentenceIdMap = MutableIntPairMap.empty();
            for (int spanIndex = 0; spanIndex < spanCount; spanIndex++) {
                final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(previousSymbolArray, maxSymbolArray);
                final int symbolArrayFileId = ibs.readHuffmanSymbol(symbolArrayTable);
                final int symbolArray = symbolArrayIdMap[symbolArrayFileId];
                if (symbolArrayFileId != previousSymbolArray) {
                    previousStart = 0;
                }

                final int sentenceLength = symbolArrayLengths[symbolArrayFileId];
                final RangedIntegerHuffmanTable startTable = new RangedIntegerHuffmanTable(previousStart, sentenceLength - 1);
                final int start = ibs.readHuffmanSymbol(startTable);

                final RangedIntegerHuffmanTable lengthTable = new RangedIntegerHuffmanTable(1, sentenceLength - start);
                final int length = ibs.readHuffmanSymbol(lengthTable);

                final int accIndex = ibs.readHuffmanSymbol(accTable);

                int sentenceId = sentenceIdMap.get(symbolArray, 0);
                if (sentenceId == 0) {
                    sentenceId = sentenceIdMap.size() + 1;
                    sentenceIdMap.put(symbolArray, sentenceId);
                }

                spans[spanIndex] = new SentenceSpan(sentenceId, symbolArray, start, length, accIndex);
                previousSymbolArray = symbolArrayFileId;
                previousStart = start;
            }
        }

        return spans;
    }

    private ImmutableIntSet readSentenceMeanings(InputStreamWrapper ibs, int[] symbolArrayIdMap, SentenceSpan[] spans) throws IOException {
        final MutableIntSet insertedSentences = MutableIntArraySet.empty();
        final int meaningCount = ibs.readHuffmanSymbol(naturalNumberTable);
        if (meaningCount > 0) {
            final MutableIntPairMap sentenceIds = MutableIntPairMap.empty();
            for (SentenceSpan span : spans) {
                sentenceIds.put(span.symbolArray, span.sentenceId);
            }

            final IntDecoder intDecoder = new IntDecoder(ibs);
            DefinedIntHuffmanTable lengthTable = ibs.readIntHuffmanTable(intDecoder, intDecoder);

            final int baseConcept = getMaxConcept(_db) + 1;
            int previousMin = 0;
            for (int meaningIndex = 0; meaningIndex < meaningCount; meaningIndex++) {
                final ImmutableIntRange range = new ImmutableIntRange(previousMin, symbolArrayIdMap.length - 1);
                final RangedIntSetDecoder decoder = new RangedIntSetDecoder(ibs, lengthTable, range);
                final ImmutableIntSet set = ibs.readIntSet(decoder, decoder, decoder).toImmutable();

                previousMin = set.min();
                for (int symbolArrayFileId : set) {
                    final int concept = baseConcept + meaningIndex;
                    final int symbolArray = symbolArrayIdMap[symbolArrayFileId];
                    final int knownSentenceId = sentenceIds.get(symbolArray, 0);
                    final int sentenceId;
                    if (knownSentenceId != 0) {
                        sentenceId = knownSentenceId;
                    }
                    else {
                        sentenceId = sentenceIds.size() + 1;
                        sentenceIds.put(symbolArray, sentenceId);
                    }

                    insertSentenceWithId(_db, sentenceId, concept, symbolArray);
                    if (!insertedSentences.add(sentenceId)) {
                        throw new AssertionError();
                    }
                }
            }
        }

        return insertedSentences.toImmutable();
    }

    private ImmutableIntRange readLanguagesAndAlphabets(InputStreamWrapper ibs) throws IOException {
        final int languageCount = ibs.readHuffmanSymbol(naturalNumberTable);
        final Language[] languages = new Language[languageCount];

        final NaturalNumberHuffmanTable nat2Table = new NaturalNumberHuffmanTable(2);
        final int minValidAlphabet = StreamedDatabaseConstants.minValidConcept + languageCount;
        int nextMinAlphabet = minValidAlphabet;

        final int lastValidLangIntCode = 26 * 26 - 1;
        int firstValidLangIntCode = 0;
        for (int languageIndex = 0; languageIndex < languageCount; languageIndex++) {
            final HuffmanTable<Integer> huffmanTable = new RangedIntegerHuffmanTable(firstValidLangIntCode, lastValidLangIntCode);
            final int langIntCode = ibs.readHuffmanSymbol(huffmanTable);
            final String code = "" + (char) (langIntCode / 26 + 'a') + (char) (langIntCode % 26 + 'a');
            firstValidLangIntCode = langIntCode + 1;

            final int alphabetCount = ibs.readHuffmanSymbol(nat2Table);
            languages[languageIndex] = new Language(code, nextMinAlphabet, alphabetCount);
            nextMinAlphabet += alphabetCount;
        }

        final int maxValidAlphabet = nextMinAlphabet - 1;
        final int minLanguage = StreamedDatabaseConstants.minValidConcept;

        for (int i = minValidAlphabet; i <= maxValidAlphabet; i++) {
            for (int j = 0; j < languageCount; j++) {
                Language lang = languages[j];
                if (lang.containsAlphabet(i)) {
                    insertAlphabet(_db, i, minLanguage + j);
                    break;
                }
            }
        }

        for (int i = 0; i < languageCount; i++) {
            final Language lang = languages[i];
            insertLanguage(_db, minLanguage + i, lang.getCode(), lang.getMainAlphabet());
        }

        return (languageCount == 0)? null : new ImmutableIntRange(minValidAlphabet, maxValidAlphabet);
    }

    private void insertMissingSentences(SentenceSpan[] spans, ImmutableIntSet insertedSentences) {
        final MutableIntPairMap map = MutableIntPairMap.empty();
        for (SentenceSpan span : spans) {
            map.put(span.sentenceId, span.symbolArray);
        }

        int concept = getMaxConcept(_db);
        for (int sentenceId : map.keySet().filterNot(insertedSentences::contains)) {
            insertSentenceWithId(_db, sentenceId, ++concept, map.get(sentenceId));
        }
    }

    private static int suitableCharacterMapLength(int currentSize, int newSize) {
        int s = ((newSize + CHARACTER_MAP_GRANULARITY - 1) / CHARACTER_MAP_GRANULARITY) * CHARACTER_MAP_GRANULARITY;
        return (s > 0)? s : CHARACTER_MAP_GRANULARITY;
    }

    private void insertCharacters(Set<Character> characters, int lastId) {
        final LangbookDbSchema.UnicodeCharactersTable table = Tables.unicodeCharacters;
        for (char ch : characters) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getIdColumnIndex(), ++lastId)
                    .put(table.getUnicodeColumnIndex(), ch)
                    .build();

            if (lastId != _db.insert(query)) {
                throw new AssertionError();
            }
        }
    }

    private void insertTokens(Set<String> tokens, int lastId) {
        final LangbookDbSchema.CharacterTokensTable table = Tables.characterTokens;
        for (String token : tokens) {
            final DbInsertQuery query = new DbInsertQuery.Builder(table)
                    .put(table.getIdColumnIndex(), ++lastId)
                    .put(table.getTokenColumnIndex(), token)
                    .build();

            if (lastId != _db.insert(query)) {
                throw new AssertionError();
            }
        }
    }

    @Override
    public Result read() throws IOException {
        try {
            setProgress(0, "Reading symbol arrays");
            final InputStreamWrapper ibs = new InputStreamWrapper(_is);
            final MutableHashSet<Character> characters = MutableHashSet.empty(StreamedDatabaseReader::suitableCharacterMapLength);
            final SymbolArrayReadResult symbolArraysReadResult = readSymbolArrays(ibs, characters);
            insertCharacters(characters, 0);
            final int[] symbolArraysIdMap = symbolArraysReadResult.idMap;

            // Read languages and its alphabets
            setProgress(0.09f, "Reading languages and its alphabets");
            final ImmutableIntRange validAlphabets = readLanguagesAndAlphabets(ibs);

            if (symbolArraysIdMap.length == 0) {
                final int validConceptCount = ibs.readHuffmanSymbol(naturalNumberTable);
                // Writer does always write a 0 here, so it is expected by the reader.
                if (validConceptCount != 0) {
                    throw new IOException();
                }

                return new Result(new Conversion[0], ImmutableIntKeyMap.empty(), ImmutableIntPairMap.empty(), new int[0], new AgentAcceptationPair[0], new SentenceSpan[0], 0, 0);
            }
            else {
                final int maxSymbolArrayIndex = symbolArraysIdMap.length - 1;

                // Read conversions
                setProgress(0.1f, "Reading conversions");
                final Conversion[] conversions = readConversions(ibs, validAlphabets, 0,
                        maxSymbolArrayIndex, symbolArraysIdMap);

                // Export the amount of words and concepts in order to range integers
                final int minValidConcept = StreamedDatabaseConstants.minValidConcept;
                final int maxConcept =
                        ibs.readHuffmanSymbol(naturalNumberTable) + StreamedDatabaseConstants.minValidConcept - 1;

                // Import correlations
                setProgress(0.15f, "Reading correlations");
                int[] correlationIdMap = readCorrelations(ibs, validAlphabets, symbolArraysIdMap);

                // Import correlation arrays
                setProgress(0.30f, "Reading correlation arrays");
                int[] correlationArrayIdMap = readCorrelationArrays(ibs, correlationIdMap);

                // Import acceptations
                setProgress(0.5f, "Reading acceptations");
                final ImmutableIntRange validConcepts = new ImmutableIntRange(minValidConcept, maxConcept);
                int[] acceptationIdMap = readAcceptations(ibs, validConcepts, correlationArrayIdMap);

                // Import bunchConcepts
                setProgress(0.6f, "Reading bunch concepts");
                readComplementedConcepts(ibs, validConcepts);

                setProgress(0.65f, "Reading character composition definitions");
                final MutableIntList definitionIds = MutableIntList.empty();
                readCharacterCompositionDefinitions(ibs, validConcepts, definitionIds);

                setProgress(0.7f, "Reading character compositions");
                readCharacterCompositions(ibs, characters, definitionIds);

                // Import bunchAcceptations
                setProgress(0.75f, "Reading bunch acceptations");
                readBunchAcceptations(ibs, validConcepts, acceptationIdMap);

                // Import agents
                setProgress(0.8f, "Reading agents");
                final AgentReadResult agentReadResult = readAgents(ibs, validConcepts, correlationIdMap, correlationArrayIdMap);

                // Import relevant dynamic acceptations
                setProgress(0.9f, "Reading referenced dynamic acceptations");
                final AgentAcceptationPair[] agentAcceptationPairs = readRelevantRuledAcceptations(ibs, acceptationIdMap,
                        agentReadResult.agentRules.keySet());

                // Import sentence spans
                setProgress(0.93f, "Writing sentence spans");
                final SentenceSpan[] spans = readSentenceSpans(ibs,
                        acceptationIdMap.length + agentAcceptationPairs.length, symbolArraysIdMap,
                        symbolArraysReadResult.lengths);

                setProgress(0.98f, "Writing sentence meanings");
                final ImmutableIntSet insertedSentences = readSentenceMeanings(ibs, symbolArraysIdMap, spans);

                insertMissingSentences(spans, insertedSentences);
                return new Result(conversions, agentReadResult.agents, agentReadResult.agentRules, acceptationIdMap, agentAcceptationPairs, spans, correlationIdMap.length, correlationArrayIdMap.length);
            }
        }
        finally {
            try {
                if (_is != null) {
                    _is.close();
                }
            }
            catch (IOException e) {
                // Nothing can be done
            }
        }
    }
}
