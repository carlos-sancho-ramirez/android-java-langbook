package sword.langbook3.android.sdb;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import sword.bitstream.FunctionWithIOException;
import sword.bitstream.InputBitStream;
import sword.bitstream.IntegerDecoder;
import sword.bitstream.NaturalDecoder;
import sword.bitstream.RangedIntegerSetDecoder;
import sword.bitstream.SupplierWithIOException;
import sword.bitstream.huffman.CharHuffmanTable;
import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.IntKeyMap;
import sword.collections.IntSet;
import sword.collections.MutableHashMap;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntPairMap;
import sword.collections.MutableIntSet;
import sword.collections.MutableIntValueHashMap;
import sword.collections.MutableMap;
import sword.database.DbImporter.Database;
import sword.langbook3.android.models.Conversion;
import sword.langbook3.android.db.LangbookDbInserter;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.db.LangbookReadableDatabase.AgentRegister;

import static sword.langbook3.android.db.LangbookDatabase.obtainCorrelation;
import static sword.langbook3.android.db.LangbookDatabase.obtainCorrelationArray;
import static sword.langbook3.android.db.LangbookDatabase.obtainSymbolArray;
import static sword.langbook3.android.db.LangbookDbInserter.insertAcceptation;
import static sword.langbook3.android.db.LangbookDbInserter.insertAgent;
import static sword.langbook3.android.db.LangbookDbInserter.insertAlphabet;
import static sword.langbook3.android.db.LangbookDbInserter.insertBunchAcceptation;
import static sword.langbook3.android.db.LangbookDbInserter.insertBunchConcept;
import static sword.langbook3.android.db.LangbookDbInserter.insertConversion;
import static sword.langbook3.android.db.LangbookDbInserter.insertLanguage;
import static sword.langbook3.android.db.LangbookDbInserter.insertSentenceMeaning;
import static sword.langbook3.android.db.LangbookReadableDatabase.findBunchSet;
import static sword.langbook3.android.db.LangbookReadableDatabase.getMaxBunchSetId;
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
        final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
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

    private SymbolArrayReadResult readSymbolArrays(InputBitStream ibs) throws IOException {
        final int symbolArraysLength = ibs.readHuffmanSymbol(naturalNumberTable);
        if (symbolArraysLength == 0) {
            final int[] emptyArray = new int[0];
            return new SymbolArrayReadResult(emptyArray, emptyArray);
        }

        final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
        final NaturalNumberHuffmanTable nat4Table = new NaturalNumberHuffmanTable(4);

        final HuffmanTable<Character> charHuffmanTable =
                ibs.readHuffmanTable(new CharReader(ibs), new CharHuffmanSymbolDiffReader(ibs, nat4Table));

        final HuffmanTable<Integer> symbolArraysLengthTable =
                ibs.readHuffmanTable(new IntReader(ibs), new IntHuffmanSymbolDiffReader(ibs, nat3Table));

        final int[] idMap = new int[symbolArraysLength];
        final int[] lengths = new int[symbolArraysLength];
        for (int index = 0; index < symbolArraysLength; index++) {
            final int length = ibs.readHuffmanSymbol(symbolArraysLengthTable);
            final StringBuilder builder = new StringBuilder();
            for (int pos = 0; pos < length; pos++) {
                builder.append(ibs.readHuffmanSymbol(charHuffmanTable));
            }

            idMap[index] = obtainSymbolArray(_db, builder.toString());
            lengths[index] = length;
        }

        return new SymbolArrayReadResult(idMap, lengths);
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
                result[i] = obtainCorrelation(_db, corr);
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

                final ImmutableIntList.Builder builder = new ImmutableIntList.Builder();
                for (int j = 0; j < arrayLength; j++) {
                    builder.add(correlationIdMap[ibs.readHuffmanSymbol(correlationTable)]);
                }
                result[i] = obtainCorrelationArray(_db, builder.build());
            }
        }

        return result;
    }

    private int[] readAcceptations(InputBitStream ibs, ImmutableIntRange validConcepts, int[] correlationArrayIdMap) throws IOException {
        final int acceptationsLength = ibs.readHuffmanSymbol(naturalNumberTable);

        final int[] acceptationsIdMap = new int[acceptationsLength];
        if (acceptationsLength >= 0) {
            final IntegerDecoder intDecoder = new IntegerDecoder(ibs);
            final HuffmanTable<Integer> corrArraySetLengthTable = ibs.readHuffmanTable(intDecoder, intDecoder);
            final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(validConcepts.min(), validConcepts.max());
            for (int i = 0; i < acceptationsLength; i++) {
                final int concept = ibs.readHuffmanSymbol(conceptTable);
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

    private AgentReadResult readAgents(
            InputBitStream ibs, ImmutableIntRange validConcepts, int[] correlationIdMap) throws IOException {

        final int agentsLength = ibs.readHuffmanSymbol(naturalNumberTable);
        final ImmutableIntKeyMap.Builder<AgentBunches> builder = new ImmutableIntKeyMap.Builder<>();
        final MutableIntSet presentRules = MutableIntArraySet.empty();

        if (agentsLength > 0) {
            final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
            final IntHuffmanSymbolReader intHuffmanSymbolReader = new IntHuffmanSymbolReader(ibs, nat3Table);
            final HuffmanTable<Integer> sourceSetLengthTable = ibs.readHuffmanTable(intHuffmanSymbolReader, null);

            int lastTarget = StreamedDatabaseConstants.nullBunchId;
            int minSource = validConcepts.min();
            int desiredSetId = getMaxBunchSetId(_db) + 1;
            final MutableIntValueHashMap<ImmutableIntSet> insertedBunchSets = MutableIntValueHashMap.empty();
            final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(validConcepts.min(), validConcepts.max());
            final RangedIntegerHuffmanTable correlationTable = new RangedIntegerHuffmanTable(0, correlationIdMap.length - 1);

            for (int i = 0; i < agentsLength; i++) {
                setProgress((0.99f - 0.8f) * i / ((float) agentsLength) + 0.8f, "Reading agent " + (i + 1) + "/" + agentsLength);
                final RangedIntegerHuffmanTable targetTable = (lastTarget == 0)?
                        new RangedIntegerHuffmanTable(0, validConcepts.max()) :
                        new RangedIntegerHuffmanTable(lastTarget + 1, validConcepts.max());
                final int targetBunch = ibs.readHuffmanSymbol(targetTable);

                if (targetBunch != lastTarget) {
                    minSource = validConcepts.min();
                }

                final ImmutableIntSet sourceSet = readRangedNumberSet(ibs, sourceSetLengthTable, minSource, validConcepts.max());

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

                final ImmutableIntSet diffSet = new ImmutableIntSetCreator().build();
                builder.put(agentId, new AgentBunches(targetBunch, sourceSet, diffSet));

                if (hasRule) {
                    presentRules.add(rule);
                }
                lastTarget = targetBunch;
            }
        }

        return new AgentReadResult(builder.build(), presentRules.toImmutable());
    }

    public static class RuleAcceptationPair {
        public final int rule;
        public final int acceptation;

        RuleAcceptationPair(int rule, int acceptation) {
            this.rule = rule;
            this.acceptation = acceptation;
        }
    }

    private RuleAcceptationPair[] readRelevantRuledAcceptations(InputBitStream ibs, int[] accIdMap, ImmutableIntSet presentRules) throws IOException {
        final int pairsCount = ibs.readHuffmanSymbol(naturalNumberTable);
        final RuleAcceptationPair[] pairs = new RuleAcceptationPair[pairsCount];
        if (pairsCount > 0) {
            final int maxMainAcc = accIdMap.length - 1;
            final int maxPresentRule = presentRules.size() - 1;
            int previousRuleIndex = 0;
            int firstPossibleAcc = 0;
            for (int pairIndex = 0; pairIndex < pairsCount; pairIndex++) {
                final RangedIntegerHuffmanTable rulesTable = new RangedIntegerHuffmanTable(previousRuleIndex, maxPresentRule);
                final int ruleIndex = ibs.readHuffmanSymbol(rulesTable);
                final int rule = presentRules.valueAt(ruleIndex);
                if (ruleIndex != previousRuleIndex) {
                    firstPossibleAcc = 0;
                }

                final RangedIntegerHuffmanTable mainAcceptationTable = new RangedIntegerHuffmanTable(firstPossibleAcc, maxMainAcc);
                int mainAccIndex = ibs.readHuffmanSymbol(mainAcceptationTable);
                int mainAcc = accIdMap[mainAccIndex];

                previousRuleIndex = ruleIndex;
                firstPossibleAcc = mainAccIndex + 1;

                pairs[pairIndex] = new RuleAcceptationPair(rule, mainAcc);
            }
        }

        return pairs;
    }

    public static final class SentenceSpan {
        public final int symbolArray;
        public final int start;
        public final int length;
        public final int acceptationFileIndex;

        SentenceSpan(int symbolArray, int start, int length, int acceptationFileIndex) {
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

    private SentenceSpan[] readSentenceSpans(InputBitStream ibs, int extendedAccCount, int[] symbolArrayIdMap, int[] symbolArrayLengths) throws IOException {
        final int maxSymbolArray = symbolArrayLengths.length - 1;
        final int spanCount = ibs.readHuffmanSymbol(naturalNumberTable);
        final SentenceSpan[] spans = new SentenceSpan[spanCount];
        if (spanCount > 0) {
            final RangedIntegerHuffmanTable accTable = new RangedIntegerHuffmanTable(0, extendedAccCount - 1);
            int previousSymbolArray = 0;
            int previousStart = 0;

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

                spans[spanIndex] = new SentenceSpan(symbolArray, start, length, accIndex);
                previousSymbolArray = symbolArrayFileId;
                previousStart = start;
            }
        }

        return spans;
    }

    private void readSentenceMeanings(InputBitStream ibs, int[] symbolArrayIdMap) throws IOException {
        final int meaningCount = ibs.readHuffmanSymbol(naturalNumberTable);
        if (meaningCount > 0) {
            final IntegerDecoder intDecoder = new IntegerDecoder(ibs);
            DefinedHuffmanTable<Integer> lengthTable = ibs.readHuffmanTable(intDecoder, intDecoder);

            int previousMin = 0;
            for (int meaningIndex = 0; meaningIndex < meaningCount; meaningIndex++) {
                final ImmutableIntSet set = readRangedNumberSet(ibs, lengthTable, previousMin, symbolArrayIdMap.length - 1);
                previousMin = set.min();
                for (int symbolArrayFileId : set) {
                    insertSentenceMeaning(_db, symbolArrayIdMap[symbolArrayFileId], meaningIndex + 1);
                }
            }
        }
    }

    public static final class Result {
        public final Conversion[] conversions;
        public final IntKeyMap<AgentBunches> agents;
        public final int[] accIdMap;
        public final RuleAcceptationPair[] ruleAcceptationPairs;
        public final SentenceSpan[] spans;

        Result(Conversion[] conversions, IntKeyMap<AgentBunches> agents, int[] accIdMap, RuleAcceptationPair[] ruleAcceptationPairs, SentenceSpan[] spans) {
            this.conversions = conversions;
            this.agents = agents;
            this.accIdMap = accIdMap;
            this.ruleAcceptationPairs = ruleAcceptationPairs;
            this.spans = spans;
        }
    }

    private static final class AgentReadResult {
        final ImmutableIntKeyMap<AgentBunches> agents;
        final ImmutableIntSet presentRules;

        AgentReadResult(ImmutableIntKeyMap<AgentBunches> agents, ImmutableIntSet presentRules) {
            this.agents = agents;
            this.presentRules = presentRules;
        }
    }

    public Result read() throws IOException {
        try {
            setProgress(0, "Reading symbol arrays");
            final InputBitStream ibs = new InputBitStream(_is);
            final SymbolArrayReadResult symbolArraysReadResult = readSymbolArrays(ibs);
            final int[] symbolArraysIdMap = symbolArraysReadResult.idMap;
            if (symbolArraysIdMap.length == 0) {
                final int validConceptCount = ibs.readHuffmanSymbol(naturalNumberTable);
                // Writer does always write a 0 here, so it is expected by the reader.
                if (validConceptCount != 0) {
                    throw new IOException();
                }

                return new Result(new Conversion[0], ImmutableIntKeyMap.empty(), new int[0], new RuleAcceptationPair[0], new SentenceSpan[0]);
            }
            else {
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
                final int maxConcept =
                        ibs.readHuffmanSymbol(naturalNumberTable) + StreamedDatabaseConstants.minValidConcept - 1;

                // Import correlations
                setProgress(0.15f, "Reading correlations");
                int[] correlationIdMap = readCorrelations(ibs, StreamedDatabaseConstants.minValidAlphabet,
                        maxValidAlphabet, symbolArraysIdMap);

                // Import correlation arrays
                setProgress(0.30f, "Reading correlation arrays");
                int[] correlationArrayIdMap = readCorrelationArrays(ibs, correlationIdMap);

                // Import acceptations
                setProgress(0.5f, "Reading acceptations");
                final ImmutableIntRange validConcepts = new ImmutableIntRange(minValidConcept, maxConcept);
                int[] acceptationIdMap = readAcceptations(ibs, validConcepts, correlationArrayIdMap);

                // Import bunchConcepts
                setProgress(0.6f, "Reading bunch concepts");
                readBunchConcepts(ibs, validConcepts);

                // Import bunchAcceptations
                setProgress(0.7f, "Reading bunch acceptations");
                readBunchAcceptations(ibs, validConcepts, acceptationIdMap);

                // Import agents
                setProgress(0.8f, "Reading agents");
                final AgentReadResult agentReadResult = readAgents(ibs, validConcepts, correlationIdMap);

                // Import relevant dynamic acceptations
                setProgress(0.9f, "Writing dynamic acceptations");
                final RuleAcceptationPair[] ruleAcceptationPairs = readRelevantRuledAcceptations(ibs, acceptationIdMap,
                        agentReadResult.presentRules);

                // Import sentence spans
                setProgress(0.93f, "Writing sentence spans");
                final SentenceSpan[] spans = readSentenceSpans(ibs,
                        acceptationIdMap.length + ruleAcceptationPairs.length, symbolArraysIdMap,
                        symbolArraysReadResult.lengths);

                setProgress(0.98f, "Writing sentence meanings");
                readSentenceMeanings(ibs, symbolArraysIdMap);

                return new Result(conversions, agentReadResult.agents, acceptationIdMap, ruleAcceptationPairs, spans);
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
