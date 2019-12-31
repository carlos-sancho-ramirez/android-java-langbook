package sword.langbook3.android.sdb;

import java.io.IOException;
import java.io.InputStream;

import sword.bitstream.FunctionWithIOException;
import sword.bitstream.InputHuffmanStream;
import sword.bitstream.InputStreamWrapper;
import sword.bitstream.SupplierWithIOException;
import sword.bitstream.huffman.CharHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.MutableHashMap;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntPairMap;
import sword.collections.MutableIntSet;
import sword.collections.MutableIntValueHashMap;
import sword.collections.MutableMap;
import sword.database.DbImporter.Database;
import sword.bitstream.huffman.DefinedIntHuffmanTable;
import sword.bitstream.IntDecoder;
import sword.bitstream.huffman.IntHuffmanTable;
import sword.bitstream.IntSupplierWithIOException;
import sword.bitstream.IntToIntFunctionWithIOException;
import sword.bitstream.NatDecoder;
import sword.bitstream.huffman.RangedIntHuffmanTable;
import sword.bitstream.RangedIntSetDecoder;
import sword.langbook3.android.db.LangbookDbInserter;
import sword.langbook3.android.models.AgentRegister;
import sword.langbook3.android.models.Conversion;

import static sword.langbook3.android.db.LangbookDatabase.addDefinition;
import static sword.langbook3.android.db.LangbookDatabase.obtainCorrelation;
import static sword.langbook3.android.db.LangbookDatabase.obtainCorrelationArray;
import static sword.langbook3.android.db.LangbookDatabase.obtainSymbolArray;
import static sword.langbook3.android.db.LangbookDbInserter.insertAcceptation;
import static sword.langbook3.android.db.LangbookDbInserter.insertAgent;
import static sword.langbook3.android.db.LangbookDbInserter.insertAlphabet;
import static sword.langbook3.android.db.LangbookDbInserter.insertBunchAcceptation;
import static sword.langbook3.android.db.LangbookDbInserter.insertConversion;
import static sword.langbook3.android.db.LangbookDbInserter.insertLanguage;
import static sword.langbook3.android.db.LangbookDbInserter.insertSentenceWithId;
import static sword.langbook3.android.db.LangbookReadableDatabase.findBunchSet;
import static sword.langbook3.android.db.LangbookReadableDatabase.getMaxBunchSetId;
import static sword.langbook3.android.db.LangbookReadableDatabase.getMaxConcept;
import static sword.langbook3.android.db.LangbookReadableDatabase.getSymbolArray;

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
        private final InputHuffmanStream _ibs;

        CharReader(InputHuffmanStream ibs) {
            _ibs = ibs;
        }

        @Override
        public Character apply() throws IOException {
            return _ibs.readHuffmanSymbol(_table);
        }
    }

    private static class CharHuffmanSymbolDiffReader implements FunctionWithIOException<Character, Character> {

        private final InputHuffmanStream _ibs;
        private final NaturalNumberHuffmanTable _table;

        CharHuffmanSymbolDiffReader(InputHuffmanStream ibs, NaturalNumberHuffmanTable table) {
            _ibs = ibs;
            _table = table;
        }

        @Override
        public Character apply(Character previous) throws IOException {
            int value = _ibs.readHuffmanSymbol(_table);
            return (char) (value + previous + 1);
        }
    }

    private class IntReader implements IntSupplierWithIOException {

        private final InputHuffmanStream _ibs;

        IntReader(InputHuffmanStream ibs) {
            _ibs = ibs;
        }

        @Override
        public int apply() throws IOException {
            return _ibs.readHuffmanSymbol(naturalNumberTable);
        }
    }

    private static class IntHuffmanSymbolReader implements IntSupplierWithIOException {

        private final InputHuffmanStream _ibs;
        private final NaturalNumberHuffmanTable _table;

        IntHuffmanSymbolReader(InputHuffmanStream ibs, NaturalNumberHuffmanTable table) {
            _ibs = ibs;
            _table = table;
        }

        @Override
        public int apply() throws IOException {
            return _ibs.readHuffmanSymbol(_table);
        }
    }

    private static class IntHuffmanSymbolDiffReader implements IntToIntFunctionWithIOException {

        private final InputHuffmanStream _ibs;
        private final NaturalNumberHuffmanTable _table;

        IntHuffmanSymbolDiffReader(InputHuffmanStream ibs, NaturalNumberHuffmanTable table) {
            _ibs = ibs;
            _table = table;
        }

        @Override
        public int apply(int previous) throws IOException {
            return _ibs.readHuffmanSymbol(_table) + previous + 1;
        }
    }

    private static class IntValueDecoder implements IntSupplierWithIOException {

        private final InputHuffmanStream _ibs;
        private final IntHuffmanTable _table;

        IntValueDecoder(InputHuffmanStream ibs, IntHuffmanTable table) {
            if (ibs == null || table == null) {
                throw new IllegalArgumentException();
            }

            _ibs = ibs;
            _table = table;
        }

        @Override
        public int apply() throws IOException {
            return _ibs.readIntHuffmanSymbol(_table);
        }
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

    private static final class SymbolArrayReadResult {
        final int[] idMap;
        final int[] lengths;

        SymbolArrayReadResult(int[] idMap, int[] lengths) {
            this.idMap = idMap;
            this.lengths = lengths;
        }
    }

    private SymbolArrayReadResult readSymbolArrays(InputHuffmanStream ibs) throws IOException {
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
                builder.append(ibs.readHuffmanSymbol(charHuffmanTable));
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
        int minTargetAlphabet = maxValidAlphabet;
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
                result[i] = obtainCorrelation(_db, corr);
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

                final ImmutableIntList.Builder builder = new ImmutableIntList.Builder();
                for (int j = 0; j < arrayLength; j++) {
                    builder.add(correlationIdMap[ibs.readHuffmanSymbol(correlationTable)]);
                }
                result[i] = obtainCorrelationArray(_db, builder.build());
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

    private AgentReadResult readAgents(
            InputStreamWrapper ibs, ImmutableIntRange validConcepts, int[] correlationIdMap) throws IOException {

        final int agentsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final ImmutableIntKeyMap.Builder<AgentBunches> builder = new ImmutableIntKeyMap.Builder<>();
        final MutableIntSet agentsWithRule = MutableIntArraySet.empty();

        if (agentsLength > 0) {
            final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
            final IntHuffmanSymbolReader intHuffmanSymbolReader = new IntHuffmanSymbolReader(ibs, nat3Table);
            final IntHuffmanTable sourceSetLengthTable = ibs.readIntHuffmanTable(intHuffmanSymbolReader, null);

            int lastTarget = StreamedDatabaseConstants.nullBunchId;
            int minSource = validConcepts.min();
            int desiredSetId = getMaxBunchSetId(_db) + 1;
            final MutableIntValueHashMap<ImmutableIntSet> insertedBunchSets = MutableIntValueHashMap.empty();
            final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(validConcepts.min(), validConcepts.max());
            final RangedIntegerHuffmanTable correlationTable = new RangedIntegerHuffmanTable(0, correlationIdMap.length - 1);

            for (int i = 0; i < agentsLength; i++) {
                setProgress((0.99f - 0.8f) * i / ((float) agentsLength) + 0.8f, "Reading agent " + (i + 1) + "/" + agentsLength);
                final RangedIntegerHuffmanTable targetTable = new RangedIntegerHuffmanTable(lastTarget, validConcepts.max());
                final int targetBunch = ibs.readHuffmanSymbol(targetTable);

                if (targetBunch != lastTarget) {
                    minSource = validConcepts.min();
                }

                final ImmutableIntRange sourceRange = new ImmutableIntRange(minSource, validConcepts.max());
                final RangedIntSetDecoder sourceDecoder = new RangedIntSetDecoder(ibs, sourceSetLengthTable, sourceRange);
                final ImmutableIntSet sourceSet = ibs.readIntSet(sourceDecoder, sourceDecoder, sourceDecoder).toImmutable();

                final RangedIntSetDecoder diffDecoder = new RangedIntSetDecoder(ibs, sourceSetLengthTable, validConcepts);
                final ImmutableIntSet diffSet = ibs.readIntSet(diffDecoder, diffDecoder, diffDecoder).toImmutable();

                if (!sourceSet.isEmpty()) {
                    int min = Integer.MAX_VALUE;
                    for (int value : sourceSet) {
                        if (value < min) {
                            min = value;
                        }
                    }
                    minSource = min;
                }

                final int startMatcherId = correlationIdMap[ibs.readHuffmanSymbol(correlationTable)];
                final int startAdderId = correlationIdMap[ibs.readHuffmanSymbol(correlationTable)];
                final int endMatcherId = correlationIdMap[ibs.readHuffmanSymbol(correlationTable)];
                final int endAdderId = correlationIdMap[ibs.readHuffmanSymbol(correlationTable)];

                final boolean hasRule = startMatcherId != startAdderId || endMatcherId != endAdderId;
                final int rule = hasRule?
                        ibs.readHuffmanSymbol(conceptTable) :
                        StreamedDatabaseConstants.nullRuleId;

                final int noPresent = -1;
                final int reusedBunchSetId = insertedBunchSets.get(sourceSet, noPresent);
                final int sourceBunchSetId;
                if (reusedBunchSetId != noPresent) {
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

                final AgentRegister register = new AgentRegister(targetBunch, sourceBunchSetId, diffBunchSetId, startMatcherId, startAdderId, endMatcherId, endAdderId, rule);
                final int agentId = insertAgent(_db, register);

                builder.put(agentId, new AgentBunches(targetBunch, sourceSet, diffSet));

                if (hasRule) {
                    agentsWithRule.add(agentId);
                }
                lastTarget = targetBunch;
            }
        }

        return new AgentReadResult(builder.build(), agentsWithRule.toImmutable());
    }

    public static class AgentAcceptationPair {
        public final int agent;
        public final int acceptation;

        AgentAcceptationPair(int agent, int acceptation) {
            this.agent = agent;
            this.acceptation = acceptation;
        }
    }

    private AgentAcceptationPair[] readRelevantRuledAcceptations(InputHuffmanStream ibs, int[] accIdMap, ImmutableIntSet agentsWithRule) throws IOException {
        final int pairsCount = ibs.readHuffmanSymbol(naturalNumberTable);
        final AgentAcceptationPair[] pairs = new AgentAcceptationPair[pairsCount];
        if (pairsCount > 0) {
            final int maxMainAcc = accIdMap.length - 1;
            final int maxAgentsWithRule = agentsWithRule.size() - 1;
            int previousAgentWithRuleIndex = 0;
            int firstPossibleAcc = 0;
            for (int pairIndex = 0; pairIndex < pairsCount; pairIndex++) {
                final RangedIntegerHuffmanTable agentTable = new RangedIntegerHuffmanTable(previousAgentWithRuleIndex, maxAgentsWithRule);
                final int agentWithRuleIndex = ibs.readHuffmanSymbol(agentTable);
                final int agent = agentsWithRule.valueAt(agentWithRuleIndex);
                if (agentWithRuleIndex != previousAgentWithRuleIndex) {
                    firstPossibleAcc = 0;
                }

                final RangedIntegerHuffmanTable mainAcceptationTable = new RangedIntegerHuffmanTable(firstPossibleAcc, maxMainAcc);
                int mainAccIndex = ibs.readHuffmanSymbol(mainAcceptationTable);
                int mainAcc = accIdMap[mainAccIndex];

                previousAgentWithRuleIndex = agentWithRuleIndex;
                firstPossibleAcc = mainAccIndex + 1;

                pairs[pairIndex] = new AgentAcceptationPair(agent, mainAcc);
            }
        }

        return pairs;
    }

    public static final class SentenceSpan {
        public final int sentenceId;
        public final int symbolArray;
        public final int start;
        public final int length;
        public final int acceptationFileIndex;

        SentenceSpan(int sentenceId, int symbolArray, int start, int length, int acceptationFileIndex) {
            this.sentenceId = sentenceId;
            this.symbolArray = symbolArray;
            this.start = start;
            this.length = length;
            this.acceptationFileIndex = acceptationFileIndex;
        }

        @Override
        public int hashCode() {
            return symbolArray * 41 + start;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof SentenceSpan)) {
                return false;
            }

            final SentenceSpan that = (SentenceSpan) other;
            return symbolArray == that.symbolArray && start == that.start && length == that.length && acceptationFileIndex == that.acceptationFileIndex;
        }
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

    private void readSentenceMeanings(InputStreamWrapper ibs, int[] symbolArrayIdMap, SentenceSpan[] spans) throws IOException {
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
                }
            }
        }
    }

    public static final class Result {
        public final Conversion[] conversions;
        public final IntKeyMap<AgentBunches> agents;
        public final int[] accIdMap;
        public final AgentAcceptationPair[] agentAcceptationPairs;
        public final SentenceSpan[] spans;

        Result(Conversion[] conversions, IntKeyMap<AgentBunches> agents, int[] accIdMap, AgentAcceptationPair[] agentAcceptationPairs, SentenceSpan[] spans) {
            this.conversions = conversions;
            this.agents = agents;
            this.accIdMap = accIdMap;
            this.agentAcceptationPairs = agentAcceptationPairs;
            this.spans = spans;
        }
    }

    private static final class AgentReadResult {
        final ImmutableIntKeyMap<AgentBunches> agents;
        final ImmutableIntSet agentsWithRule;

        AgentReadResult(ImmutableIntKeyMap<AgentBunches> agents, ImmutableIntSet agentsWithRule) {
            this.agents = agents;
            this.agentsWithRule = agentsWithRule;
        }
    }

    private ImmutableIntRange readLanguagesAndAlphabets(InputStreamWrapper ibs) throws IOException {
        final int languageCount = ibs.readHuffmanSymbol(naturalNumberTable);
        final Language[] languages = new Language[languageCount];

        final NaturalNumberHuffmanTable nat2Table = new NaturalNumberHuffmanTable(2);
        final int minValidAlphabet = StreamedDatabaseConstants.minValidConcept + languageCount;
        int nextMinAlphabet = minValidAlphabet;

        final RangedIntegerHuffmanTable languageCodeSymbol = new RangedIntegerHuffmanTable('a', 'z');
        for (int languageIndex = 0; languageIndex < languageCount; languageIndex++) {
            final char firstChar = (char) ibs.readHuffmanSymbol(languageCodeSymbol).intValue();
            final char secondChar = (char) ibs.readHuffmanSymbol(languageCodeSymbol).intValue();
            final int alphabetCount = ibs.readHuffmanSymbol(nat2Table);

            final String code = "" + firstChar + secondChar;
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

    public Result read() throws IOException {
        try {
            setProgress(0, "Reading symbol arrays");
            final InputStreamWrapper ibs = new InputStreamWrapper(_is);
            final SymbolArrayReadResult symbolArraysReadResult = readSymbolArrays(ibs);
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

                return new Result(new Conversion[0], ImmutableIntKeyMap.empty(), new int[0], new AgentAcceptationPair[0], new SentenceSpan[0]);
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

                // Import bunchAcceptations
                setProgress(0.7f, "Reading bunch acceptations");
                readBunchAcceptations(ibs, validConcepts, acceptationIdMap);

                // Import agents
                setProgress(0.8f, "Reading agents");
                final AgentReadResult agentReadResult = readAgents(ibs, validConcepts, correlationIdMap);

                // Import relevant dynamic acceptations
                setProgress(0.9f, "Reading dynamic acceptations");
                final AgentAcceptationPair[] agentAcceptationPairs = readRelevantRuledAcceptations(ibs, acceptationIdMap,
                        agentReadResult.agentsWithRule);

                // Import sentence spans
                setProgress(0.93f, "Writing sentence spans");
                final SentenceSpan[] spans = readSentenceSpans(ibs,
                        acceptationIdMap.length + agentAcceptationPairs.length, symbolArraysIdMap,
                        symbolArraysReadResult.lengths);

                setProgress(0.98f, "Writing sentence meanings");
                readSentenceMeanings(ibs, symbolArraysIdMap, spans);

                return new Result(conversions, agentReadResult.agents, acceptationIdMap, agentAcceptationPairs, spans);
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
