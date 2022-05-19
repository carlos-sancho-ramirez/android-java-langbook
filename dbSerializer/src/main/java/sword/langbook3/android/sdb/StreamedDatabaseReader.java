package sword.langbook3.android.sdb;

import java.io.IOException;
import java.io.InputStream;

import sword.bitstream.InputStreamWrapper;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.IntList;
import sword.collections.List;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntList;
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
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.db.LangbookDbSchema.Tables;
import sword.langbook3.android.sdb.StreamedDatabase0Reader.AgentReadResult;
import sword.langbook3.android.sdb.StreamedDatabase0Reader.Language;
import sword.langbook3.android.sdb.StreamedDatabase0Reader.SymbolArrayReadResult;

import static sword.langbook3.android.db.LangbookDbSchema.CHARACTER_COMPOSITION_DEFINITION_VIEW_PORT;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.insertAlphabet;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.insertCharacters;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.insertLanguage;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.insertMissingSentences;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.readAcceptations;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.readAgents;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.readBunchAcceptations;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.readComplementedConcepts;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.readConversions;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.readCorrelationArrays;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.readCorrelations;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.readRelevantRuledAcceptations;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.readSentenceMeanings;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.readSentenceSpans;
import static sword.langbook3.android.sdb.StreamedDatabase0Reader.readSymbolArrays;

public final class StreamedDatabaseReader implements StreamedDatabaseReaderInterface {

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

                insertCharacters(_db, missingCharacters, characters.size());
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
            final MutableHashSet<Character> characters = MutableHashSet.empty(StreamedDatabase0Reader::suitableCharacterMapLength);
            final SymbolArrayReadResult symbolArraysReadResult = readSymbolArrays(_db, ibs, characters);
            insertCharacters(_db, characters, 0);
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
                final Conversion[] conversions = readConversions(_db, ibs, validAlphabets, 0,
                        maxSymbolArrayIndex, symbolArraysIdMap);

                // Export the amount of words and concepts in order to range integers
                final int minValidConcept = StreamedDatabaseConstants.minValidConcept;
                final int maxConcept =
                        ibs.readHuffmanSymbol(naturalNumberTable) + StreamedDatabaseConstants.minValidConcept - 1;

                // Import correlations
                setProgress(0.15f, "Reading correlations");
                int[] correlationIdMap = readCorrelations(_db, ibs, validAlphabets, symbolArraysIdMap);

                // Import correlation arrays
                setProgress(0.30f, "Reading correlation arrays");
                int[] correlationArrayIdMap = readCorrelationArrays(_db, ibs, correlationIdMap);

                // Import acceptations
                setProgress(0.5f, "Reading acceptations");
                final ImmutableIntRange validConcepts = new ImmutableIntRange(minValidConcept, maxConcept);
                int[] acceptationIdMap = readAcceptations(_db, ibs, validConcepts, correlationArrayIdMap);

                // Import bunchConcepts
                setProgress(0.6f, "Reading bunch concepts");
                readComplementedConcepts(_db, ibs, validConcepts);

                setProgress(0.65f, "Reading character composition definitions");
                final MutableIntList definitionIds = MutableIntList.empty();
                readCharacterCompositionDefinitions(ibs, validConcepts, definitionIds);

                setProgress(0.7f, "Reading character compositions");
                readCharacterCompositions(ibs, characters, definitionIds);

                // Import bunchAcceptations
                setProgress(0.75f, "Reading bunch acceptations");
                readBunchAcceptations(_db, ibs, validConcepts, acceptationIdMap);

                // Import agents
                setProgress(0.8f, "Reading agents");
                final AgentReadResult agentReadResult = readAgents(_db, _listener, ibs, validConcepts, correlationIdMap, correlationArrayIdMap);

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
                final ImmutableIntSet insertedSentences = readSentenceMeanings(_db, ibs, symbolArraysIdMap, spans);

                insertMissingSentences(_db, spans, insertedSentences);
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
