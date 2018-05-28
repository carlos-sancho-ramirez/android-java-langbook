package sword.langbook3.android.sdb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import sword.bitstream.FunctionWithIOException;
import sword.bitstream.InputBitStream;
import sword.bitstream.IntegerDecoder;
import sword.bitstream.NaturalDecoder;
import sword.bitstream.RangedIntegerSetDecoder;
import sword.bitstream.SupplierWithIOException;
import sword.bitstream.huffman.CharHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.MutableIntPairMap;
import sword.langbook3.android.LangbookDbInserter;
import sword.langbook3.android.LangbookDbSchema;
import sword.langbook3.android.db.DbImporter;
import sword.langbook3.android.db.DbImporter.Database;
import sword.langbook3.android.db.DbInsertQuery;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbTable;

import static sword.langbook3.android.LangbookDatabase.insertCorrelation;
import static sword.langbook3.android.LangbookDatabase.insertCorrelationArray;
import static sword.langbook3.android.LangbookDatabase.obtainSymbolArray;
import static sword.langbook3.android.LangbookDbInserter.insertAcceptation;
import static sword.langbook3.android.LangbookDbInserter.insertAgent;
import static sword.langbook3.android.LangbookDbInserter.insertAlphabet;
import static sword.langbook3.android.LangbookDbInserter.insertBunchAcceptation;
import static sword.langbook3.android.LangbookDbInserter.insertBunchConcept;
import static sword.langbook3.android.LangbookDbInserter.insertConversion;
import static sword.langbook3.android.LangbookDbInserter.insertLanguage;
import static sword.langbook3.android.LangbookReadableDatabase.findBunchSet;
import static sword.langbook3.android.LangbookReadableDatabase.findCorrelation;
import static sword.langbook3.android.LangbookReadableDatabase.findSymbolArray;
import static sword.langbook3.android.LangbookReadableDatabase.getCorrelation;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxBunchSetId;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxCorrelationArrayId;
import static sword.langbook3.android.LangbookReadableDatabase.getMaxCorrelationId;
import static sword.langbook3.android.LangbookReadableDatabase.getSymbolArray;

public final class StreamedDatabaseReader {

    static final NaturalNumberHuffmanTable naturalNumberTable = new NaturalNumberHuffmanTable(8);
    private final Database _db;
    private final InputStream _is;
    private final ProgressListener _listener;

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

    private static class CharReader implements SupplierWithIOException<Character> {

        private final CharHuffmanTable _table = new CharHuffmanTable(8);
        private final InputBitStream _ibs;

        CharReader(InputBitStream ibs) {
            _ibs = ibs;
        }

        @Override
        public Character apply() throws IOException {
            return _ibs.readHuffmanSymbol(_table);
        }
    }

    private static class CharHuffmanSymbolDiffReader implements FunctionWithIOException<Character, Character> {

        private final InputBitStream _ibs;
        private final NaturalNumberHuffmanTable _table;

        CharHuffmanSymbolDiffReader(InputBitStream ibs, NaturalNumberHuffmanTable table) {
            _ibs = ibs;
            _table = table;
        }

        @Override
        public Character apply(Character previous) throws IOException {
            int value = _ibs.readHuffmanSymbol(_table);
            return (char) (value + previous + 1);
        }
    }

    private class IntReader implements SupplierWithIOException<Integer> {

        private final InputBitStream _ibs;

        IntReader(InputBitStream ibs) {
            _ibs = ibs;
        }

        @Override
        public Integer apply() throws IOException {
            return _ibs.readHuffmanSymbol(naturalNumberTable);
        }
    }

    private static class IntHuffmanSymbolReader implements SupplierWithIOException<Integer> {

        private final InputBitStream _ibs;
        private final NaturalNumberHuffmanTable _table;

        IntHuffmanSymbolReader(InputBitStream ibs, NaturalNumberHuffmanTable table) {
            _ibs = ibs;
            _table = table;
        }

        @Override
        public Integer apply() throws IOException {
            return _ibs.readHuffmanSymbol(_table);
        }
    }

    private class IntDiffReader implements FunctionWithIOException<Integer, Integer> {

        private final InputBitStream _ibs;

        IntDiffReader(InputBitStream ibs) {
            _ibs = ibs;
        }

        @Override
        public Integer apply(Integer previous) throws IOException {
            return _ibs.readHuffmanSymbol(naturalNumberTable) + previous + 1;
        }
    }

    private static class IntHuffmanSymbolDiffReader implements FunctionWithIOException<Integer, Integer> {

        private final InputBitStream _ibs;
        private final NaturalNumberHuffmanTable _table;

        IntHuffmanSymbolDiffReader(InputBitStream ibs, NaturalNumberHuffmanTable table) {
            _ibs = ibs;
            _table = table;
        }

        @Override
        public Integer apply(Integer previous) throws IOException {
            return _ibs.readHuffmanSymbol(_table) + previous + 1;
        }
    }

    private static class ValueDecoder<E> implements SupplierWithIOException<E> {

        private final InputBitStream _ibs;
        private final HuffmanTable<E> _table;

        ValueDecoder(InputBitStream ibs, HuffmanTable<E> table) {
            if (ibs == null || table == null) {
                throw new IllegalArgumentException();
            }

            _ibs = ibs;
            _table = table;
        }

        @Override
        public E apply() throws IOException {
            return _ibs.readHuffmanSymbol(_table);
        }
    }

    private ImmutableIntSet readRangedNumberSet(InputBitStream ibs, HuffmanTable<Integer> lengthTable, int min, int max) throws IOException {
        final RangedIntegerSetDecoder decoder = new RangedIntegerSetDecoder(ibs, lengthTable, min, max);
        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        for (int value : ibs.readSet(decoder, decoder, decoder)) {
            builder.add(value);
        }
        return builder.build();
    }

    private static final class Language {

        private final String _code;
        private final int _minAlphabet;
        private final int _maxAlphabet;

        Language(String code, int minAlphabet, int alphabetCount) {
            if (code.length() != 2) {
                throw new IllegalArgumentException("Invalid language code");
            }

            if (alphabetCount <= 0) {
                throw new IllegalArgumentException("Alphabet count must be positive");
            }

            _code = code;
            _minAlphabet = minAlphabet;
            _maxAlphabet = minAlphabet + alphabetCount - 1;
        }

        boolean containsAlphabet(int alphabet) {
            return alphabet >= _minAlphabet && alphabet <= _maxAlphabet;
        }

        String getCode() {
            return _code;
        }

        int getMainAlphabet() {
            // For now, it is considered the main alphabet the first of them.
            return _minAlphabet;
        }

        @Override
        public String toString() {
            return "(" + _code + ", " + Integer.toString(_maxAlphabet - _minAlphabet + 1) + ')';
        }
    }

    public static final class Conversion {

        private final int _sourceAlphabet;
        private final int _targetAlphabet;
        private final String[] _sources;
        private final String[] _targets;

        Conversion(int sourceAlphabet, int targetAlphabet, String[] sources, String[] targets) {
            _sourceAlphabet = sourceAlphabet;
            _targetAlphabet = targetAlphabet;
            _sources = sources;
            _targets = targets;
        }

        public int getSourceAlphabet() {
            return _sourceAlphabet;
        }

        public int getTargetAlphabet() {
            return _targetAlphabet;
        }

        private String convert(String text, int index, String acc) {
            final int length = _sources.length;
            if (index == text.length()) {
                return acc;
            }

            for (int i = 0; i < length; i++) {
                if (text.startsWith(_sources[i], index)) {
                    return convert(text, index + _sources[i].length(), acc + _targets[i]);
                }
            }

            return null;
        }

        public String convert(String text) {
            return (text != null)? convert(text, 0, "") : null;
        }
    }

    public static class AgentBunches {

        private final int _target;
        private final ImmutableIntSet _sources;
        private final ImmutableIntSet _diff;

        AgentBunches(int target, ImmutableIntSet sources, ImmutableIntSet diff) {
            _target = target;
            _sources = sources;
            _diff = diff;
        }

        public boolean dependsOn(AgentBunches agent) {
            final int target = agent._target;
            return _sources.contains(target) || _diff.contains(target);
        }
    }

    private int obtainBunchSet(int setId, IntSet bunches) {
        if (bunches.isEmpty()) {
            return 0;
        }

        final Integer foundId = findBunchSet(_db, bunches);
        if (foundId != null) {
            return foundId;
        }

        LangbookDbInserter.insertBunchSet(_db, setId, bunches);
        return setId;
    }

    private int[] readSymbolArrays(InputBitStream ibs) throws IOException {
        final int symbolArraysLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
        final NaturalNumberHuffmanTable nat4Table = new NaturalNumberHuffmanTable(4);

        final HuffmanTable<Character> charHuffmanTable =
                ibs.readHuffmanTable(new CharReader(ibs), new CharHuffmanSymbolDiffReader(ibs, nat4Table));

        final HuffmanTable<Integer> symbolArraysLengthTable =
                ibs.readHuffmanTable(new IntReader(ibs), new IntHuffmanSymbolDiffReader(ibs, nat3Table));

        final int[] idMap = new int[symbolArraysLength];
        for (int index = 0; index < symbolArraysLength; index++) {
            final int length = ibs.readHuffmanSymbol(symbolArraysLengthTable);
            final StringBuilder builder = new StringBuilder();
            for (int pos = 0; pos < length; pos++) {
                builder.append(ibs.readHuffmanSymbol(charHuffmanTable));
            }

            idMap[index] = obtainSymbolArray(_db, builder.toString());
        }

        return idMap;
    }

    private Conversion[] readConversions(InputBitStream ibs, int minValidAlphabet, int maxValidAlphabet, int minSymbolArrayIndex, int maxSymbolArrayIndex, int[] symbolArraysIdMap) throws IOException {
        final int conversionsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final Conversion[] conversions = new Conversion[conversionsLength];
        final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(minSymbolArrayIndex, maxSymbolArrayIndex);

        int minSourceAlphabet = minValidAlphabet;
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
            final String[] sources = new String[pairCount];
            final String[] targets = new String[pairCount];
            for (int j = 0; j < pairCount; j++) {
                final int source = symbolArraysIdMap[ibs.readHuffmanSymbol(symbolArrayTable)];
                final int target = symbolArraysIdMap[ibs.readHuffmanSymbol(symbolArrayTable)];
                insertConversion(_db, sourceAlphabet, targetAlphabet, source, target);

                sources[j] = getSymbolArray(_db, source);
                targets[j] = getSymbolArray(_db, target);
            }

            conversions[i] = new Conversion(sourceAlphabet, targetAlphabet, sources, targets);
        }

        return conversions;
    }

    private int[] readCorrelations(InputBitStream ibs, int minAlphabet, int maxAlphabet, int[] symbolArraysIdMap) throws IOException {
        final int correlationsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final int[] result = new int[correlationsLength];
        if (correlationsLength > 0) {
            final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(0, symbolArraysIdMap.length - 1);
            final IntegerDecoder intDecoder = new IntegerDecoder(ibs);
            final HuffmanTable<Integer> lengthTable = ibs.readHuffmanTable(intDecoder, intDecoder);
            final RangedIntegerSetDecoder keyDecoder = new RangedIntegerSetDecoder(ibs, lengthTable, minAlphabet, maxAlphabet);
            final ValueDecoder<Integer> valueDecoder = new ValueDecoder<>(ibs, symbolArrayTable);

            for (int i = 0; i < correlationsLength; i++) {
                Map<Integer, Integer> corrMap = ibs.readMap(keyDecoder, keyDecoder, keyDecoder, valueDecoder);
                MutableIntPairMap corr = MutableIntPairMap.empty();
                for (Map.Entry<Integer, Integer> entry : corrMap.entrySet()) {
                    corr.put(entry.getKey(), symbolArraysIdMap[entry.getValue()]);
                }
                result[i] = insertCorrelation(_db, corr);
            }
        }

        return result;
    }

    private int[] readCorrelationArrays(InputBitStream ibs, int[] correlationIdMap) throws IOException {
        final int arraysLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final int[] result = new int[arraysLength];
        if (arraysLength > 0) {
            final RangedIntegerHuffmanTable correlationTable = new RangedIntegerHuffmanTable(0, correlationIdMap.length - 1);
            final IntegerDecoder intDecoder = new IntegerDecoder(ibs);
            final HuffmanTable<Integer> lengthTable = ibs.readHuffmanTable(intDecoder, intDecoder);

            for (int i = 0; i < arraysLength; i++) {
                final int arrayLength = ibs.readHuffmanSymbol(lengthTable);

                int[] corrArray = new int[arrayLength];
                for (int j = 0; j < arrayLength; j++) {
                    corrArray[j] = correlationIdMap[ibs.readHuffmanSymbol(correlationTable)];
                }
                result[i] = insertCorrelationArray(_db, corrArray);
            }
        }

        return result;
    }

    private int[] readAcceptations(InputBitStream ibs, int[] conceptIdMap, int[] correlationArrayIdMap) throws IOException {
        final int acceptationsLength = ibs.readHuffmanSymbol(naturalNumberTable);

        final int[] acceptationsIdMap = new int[acceptationsLength];
        if (acceptationsLength >= 0) {
            final IntegerDecoder intDecoder = new IntegerDecoder(ibs);
            final HuffmanTable<Integer> corrArraySetLengthTable = ibs.readHuffmanTable(intDecoder, intDecoder);
            final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(StreamedDatabaseConstants.minValidConcept, conceptIdMap.length - 1);
            for (int i = 0; i < acceptationsLength; i++) {
                final int concept = conceptIdMap[ibs.readHuffmanSymbol(conceptTable)];
                final ImmutableIntSet corrArraySet = readRangedNumberSet(ibs, corrArraySetLengthTable, 0, correlationArrayIdMap.length - 1);
                for (int corrArray : corrArraySet) {
                    // TODO: Separate acceptations and correlations in 2 tables to avoid overlapping if there is more than one correlation array
                    acceptationsIdMap[i] = insertAcceptation(_db, concept, correlationArrayIdMap[corrArray]);
                }
            }
        }

        return acceptationsIdMap;
    }

    private void readBunchConcepts(InputBitStream ibs, ImmutableIntRange validConcepts) throws IOException {
        final int bunchConceptsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final NaturalDecoder natDecoder = new NaturalDecoder(ibs);
        final HuffmanTable<Integer> bunchConceptsLengthTable = (bunchConceptsLength > 0)?
                ibs.readHuffmanTable(natDecoder, natDecoder) : null;

        int minBunchConcept = validConcepts.min();
        final int maxValidBunch = validConcepts.max();
        for (int maxBunchConcept = validConcepts.max() - bunchConceptsLength + 1; maxBunchConcept <= maxValidBunch; maxBunchConcept++) {
            final int bunch = ibs.readHuffmanSymbol(new RangedIntegerHuffmanTable(minBunchConcept, maxBunchConcept));
            minBunchConcept = bunch + 1;

            final ImmutableIntSet concepts = readRangedNumberSet(ibs, bunchConceptsLengthTable, validConcepts.min(), validConcepts.max());
            for (int concept : concepts) {
                insertBunchConcept(_db, bunch, concept);
            }
        }
    }

    private void readBunchAcceptations(InputBitStream ibs, ImmutableIntRange validConcepts, int[] acceptationsIdMap) throws IOException {
        final int bunchAcceptationsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final NaturalDecoder natDecoder = new NaturalDecoder(ibs);
        final HuffmanTable<Integer> bunchAcceptationsLengthTable = (bunchAcceptationsLength > 0)?
                ibs.readHuffmanTable(natDecoder, natDecoder) : null;

        final int maxValidAcceptation = acceptationsIdMap.length - 1;
        final int nullAgentSet = LangbookDbSchema.Tables.agentSets.nullReference();

        int minBunch = validConcepts.min();
        final int maxValidBunch = validConcepts.max();
        for (int maxBunch = validConcepts.max() - bunchAcceptationsLength + 1; maxBunch <= maxValidBunch; maxBunch++) {
            final int bunch = ibs.readHuffmanSymbol(new RangedIntegerHuffmanTable(minBunch, maxBunch));
            minBunch = bunch + 1;

            final ImmutableIntSet acceptations = readRangedNumberSet(ibs, bunchAcceptationsLengthTable, 0, maxValidAcceptation);
            for (int acceptation : acceptations) {
                insertBunchAcceptation(_db, bunch, acceptationsIdMap[acceptation], nullAgentSet);
            }
        }
    }

    private ImmutableIntKeyMap<AgentBunches> readAgents(
            InputBitStream ibs, int maxConcept, int[] correlationIdMap) throws IOException {

        final int agentsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final ImmutableIntKeyMap.Builder<AgentBunches> builder = new ImmutableIntKeyMap.Builder<>();

        if (agentsLength > 0) {
            final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
            final IntHuffmanSymbolReader intHuffmanSymbolReader = new IntHuffmanSymbolReader(ibs, nat3Table);
            final HuffmanTable<Integer> sourceSetLengthTable = ibs.readHuffmanTable(intHuffmanSymbolReader, null);

            int lastTarget = StreamedDatabaseConstants.nullBunchId;
            int minSource = StreamedDatabaseConstants.minValidConcept;
            int desiredSetId = getMaxBunchSetId(_db) + 1;
            final Map<ImmutableIntSet, Integer> insertedBunchSets = new HashMap<>();
            final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(StreamedDatabaseConstants.minValidConcept, maxConcept);
            final RangedIntegerHuffmanTable correlationTable = new RangedIntegerHuffmanTable(0, correlationIdMap.length - 1);

            for (int i = 0; i < agentsLength; i++) {
                setProgress((0.99f - 0.8f) * i / ((float) agentsLength) + 0.8f, "Reading agent " + (i + 1) + "/" + agentsLength);
                final RangedIntegerHuffmanTable thisConceptTable = new RangedIntegerHuffmanTable(lastTarget, maxConcept);
                final int targetBunch = ibs.readHuffmanSymbol(thisConceptTable);

                if (targetBunch != lastTarget) {
                    minSource = StreamedDatabaseConstants.minValidConcept;
                }

                final ImmutableIntSet sourceSet = readRangedNumberSet(ibs, sourceSetLengthTable, minSource, maxConcept);

                if (!sourceSet.isEmpty()) {
                    int min = Integer.MAX_VALUE;
                    for (int value : sourceSet) {
                        if (value < min) {
                            min = value;
                        }
                    }
                    minSource = min;
                }

                final int matcherId = correlationIdMap[ibs.readHuffmanSymbol(correlationTable)];
                final int adderId = correlationIdMap[ibs.readHuffmanSymbol(correlationTable)];
                final ImmutableIntPairMap adder = getCorrelation(_db, adderId);

                final boolean adderNonEmpty = adder.size() > 0;
                final int rule = adderNonEmpty?
                        ibs.readHuffmanSymbol(conceptTable) :
                        StreamedDatabaseConstants.nullRuleId;

                final boolean fromStart = (adderNonEmpty || getCorrelation(_db, matcherId).size() > 0) && ibs.readBoolean();
                final int flags = fromStart? 1 : 0;

                final Integer reusedBunchSetId = insertedBunchSets.get(sourceSet);
                final int sourceBunchSetId;
                if (reusedBunchSetId != null) {
                    sourceBunchSetId = reusedBunchSetId;
                }
                else {
                    sourceBunchSetId = obtainBunchSet(desiredSetId, sourceSet);
                    if (sourceBunchSetId == desiredSetId) {
                        ++desiredSetId;
                    }
                    insertedBunchSets.put(sourceSet, sourceBunchSetId);
                }

                final int diffBunchSetId = 0;

                if (rule != StreamedDatabaseConstants.nullRuleId && matcherId == adderId) {
                    throw new AssertionError("When rule is provided, modification is expected, but matcher and adder are the same");
                }

                final int agentId = insertAgent(_db, targetBunch, sourceBunchSetId, diffBunchSetId, matcherId, adderId, rule, flags);

                final ImmutableIntSet diffSet = new ImmutableIntSetBuilder().build();
                builder.put(agentId, new AgentBunches(targetBunch, sourceSet, diffSet));

                lastTarget = targetBunch;
            }
        }

        return builder.build();
    }

    public static final class Result {
        public final Conversion[] conversions;
        public final IntKeyMap<AgentBunches> agents;

        Result(Conversion[] conversions, IntKeyMap<AgentBunches> agents) {
            this.conversions = conversions;
            this.agents = agents;
        }
    }

    public Result read() throws IOException {
        try {
            setProgress(0, "Reading symbol arrays");
            final InputBitStream ibs = new InputBitStream(_is);
            final int[] symbolArraysIdMap = readSymbolArrays(ibs);
            final int minSymbolArrayIndex = 0;
            final int maxSymbolArrayIndex = symbolArraysIdMap.length - 1;
            final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(minSymbolArrayIndex,
                    maxSymbolArrayIndex);

            // Read languages and its alphabets
            setProgress(0.09f, "Reading languages and its alphabets");
            final int languageCount = ibs.readHuffmanSymbol(naturalNumberTable);
            final Language[] languages = new Language[languageCount];
            final int minValidAlphabet = StreamedDatabaseConstants.minValidAlphabet;
            int nextMinAlphabet = StreamedDatabaseConstants.minValidAlphabet;
            final NaturalNumberHuffmanTable nat2Table = new NaturalNumberHuffmanTable(2);

            for (int languageIndex = 0; languageIndex < languageCount; languageIndex++) {
                final int codeSymbolArrayIndex = ibs.readHuffmanSymbol(symbolArrayTable);
                final int alphabetCount = ibs.readHuffmanSymbol(nat2Table);
                final String code = getSymbolArray(_db, symbolArraysIdMap[codeSymbolArrayIndex]);
                languages[languageIndex] = new Language(code, nextMinAlphabet, alphabetCount);

                nextMinAlphabet += alphabetCount;
            }

            final int maxValidAlphabet = nextMinAlphabet - 1;
            final int minLanguage = nextMinAlphabet;

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

            // Read conversions
            setProgress(0.1f, "Reading conversions");
            final Conversion[] conversions = readConversions(ibs, minValidAlphabet, maxValidAlphabet, 0,
                    maxSymbolArrayIndex, symbolArraysIdMap);

            // Export the amount of words and concepts in order to range integers
            final int minValidConcept = StreamedDatabaseConstants.minValidConcept;
            final int maxConcept = ibs.readHuffmanSymbol(naturalNumberTable) - 1;

            int[] conceptIdMap = new int[maxConcept + 1];
            for (int i = 0; i <= maxConcept; i++) {
                conceptIdMap[i] = i;
            }

            // Import correlations
            setProgress(0.15f, "Reading correlations");
            int[] correlationIdMap = readCorrelations(ibs, StreamedDatabaseConstants.minValidAlphabet,
                    maxValidAlphabet, symbolArraysIdMap);

            // Import correlation arrays
            setProgress(0.30f, "Reading correlation arrays");
            int[] correlationArrayIdMap = readCorrelationArrays(ibs, correlationIdMap);

            // Import acceptations
            setProgress(0.5f, "Reading acceptations");
            int[] acceptationIdMap = readAcceptations(ibs, conceptIdMap, correlationArrayIdMap);

            // Import bunchConcepts
            setProgress(0.6f, "Reading bunch concepts");
            final ImmutableIntRange validConcepts = new ImmutableIntRange(minValidConcept, maxConcept);
            readBunchConcepts(ibs, validConcepts);

            // Import bunchAcceptations
            setProgress(0.7f, "Reading bunch acceptations");
            readBunchAcceptations(ibs, validConcepts, acceptationIdMap);

            // Import agents
            setProgress(0.8f, "Reading agents");
            ImmutableIntKeyMap<AgentBunches> agents = readAgents(ibs, maxConcept, correlationIdMap);

            // Import ruleConcepts
            setProgress(0.99f, "Reading rule concepts");
            if (ibs.readHuffmanSymbol(naturalNumberTable) != 0) {
                throw new UnsupportedOperationException("For now, this should always be an empty table");
            }

            return new Result(conversions, agents);
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
