package sword.langbook3.android.sdb;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import sword.bitstream.IntegerEncoder;
import sword.bitstream.OutputBitStream;
import sword.bitstream.Procedure2WithIOException;
import sword.bitstream.ProcedureWithIOException;
import sword.bitstream.RangedIntegerSetEncoder;
import sword.bitstream.huffman.CharHuffmanTable;
import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.IntValueMap;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntPairMap;
import sword.collections.MutableIntSet;
import sword.collections.MutableIntValueMap;
import sword.langbook3.android.LangbookDbSchema;
import sword.langbook3.android.db.DbInitializer.Database;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbTable;

import static sword.langbook3.android.sdb.StreamedDatabaseReader.naturalNumberTable;

public final class StreamedDatabaseWriter {

    private final Database _db;
    private final OutputBitStream _obs;
    private final ProgressListener _listener;

    private static class CharWriter implements ProcedureWithIOException<Character> {

        private final CharHuffmanTable _table = new CharHuffmanTable(8);
        private final OutputBitStream _obs;

        CharWriter(OutputBitStream obs) {
            _obs = obs;
        }

        @Override
        public void apply(Character ch) throws IOException {
            _obs.writeHuffmanSymbol(_table, ch);
        }
    }

    private static class CharHuffmanSymbolDiffWriter implements Procedure2WithIOException<Character> {

        private final OutputBitStream _obs;
        private final NaturalNumberHuffmanTable _table;

        CharHuffmanSymbolDiffWriter(OutputBitStream obs, NaturalNumberHuffmanTable table) {
            _obs = obs;
            _table = table;
        }

        @Override
        public void apply(Character previous, Character element) throws IOException {
            _obs.writeHuffmanSymbol(_table, element - previous - 1);
        }
    }

    private static final class CharComparator implements Comparator<Character> {

        @Override
        public int compare(Character a, Character b) {
            return a - b;
        }
    }

    private class IntWriter implements ProcedureWithIOException<Integer> {

        private final NaturalNumberHuffmanTable _table;

        IntWriter() {
            _table = naturalNumberTable;
        }

        IntWriter(NaturalNumberHuffmanTable table) {
            _table = table;
        }

        @Override
        public void apply(Integer value) throws IOException {
            _obs.writeHuffmanSymbol(_table, value);
        }
    }

    private static class IntHuffmanSymbolDiffWriter implements Procedure2WithIOException<Integer> {

        private final OutputBitStream _obs;
        private final NaturalNumberHuffmanTable _table;

        IntHuffmanSymbolDiffWriter(OutputBitStream obs, NaturalNumberHuffmanTable table) {
            _obs = obs;
            _table = table;
        }

        @Override
        public void apply(Integer previous, Integer element) throws IOException {
            _obs.writeHuffmanSymbol(_table, element - previous - 1);
        }
    }

    private class IntDiffWriter implements Procedure2WithIOException<Integer> {

        @Override
        public void apply(Integer previous, Integer element) throws IOException {
            _obs.writeHuffmanSymbol(naturalNumberTable, element - previous - 1);
        }
    }

    private static final class IntComparator implements Comparator<Integer> {

        @Override
        public int compare(Integer a, Integer b) {
            return a - b;
        }
    }

    private class ValueEncoder<E> implements ProcedureWithIOException<E> {

        private final HuffmanTable<E> _table;

        ValueEncoder(HuffmanTable<E> table) {
            if (table == null) {
                throw new IllegalArgumentException();
            }

            _table = table;
        }

        @Override
        public void apply(E element) throws IOException {
            _obs.writeHuffmanSymbol(_table, element);
        }
    }

    public StreamedDatabaseWriter(Database db, OutputStream os, ProgressListener listener) {
        _db = db;
        _obs = new OutputBitStream(os);
        _listener = listener;
    }

    private int getTableLength(DbTable table) {
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex());

        final DbResult result = _db.select(query);
        int count = 0;
        try {
            while (result.hasNext()) {
                ++count;
                result.next();
            }
        }
        finally {
            result.close();
        }

        return count;
    }

    private <E> java.util.Map<E, Integer> composeJavaMap(IntValueMap<E> map) {
        final HashMap<E, Integer> result = new HashMap<>();
        for (IntValueMap.Entry<E> entry : map.entries()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private java.util.Map<Integer, Integer> composeJavaMap(IntPairMap map) {
        final HashMap<Integer, Integer> result = new HashMap<>();
        for (IntPairMap.Entry entry : map.entries()) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private Set<Integer> composeJavaSet(IntSet set) {
        final HashSet<Integer> javaSet = new HashSet<>();
        for (int value : set) {
            javaSet.add(value);
        }

        return javaSet;
    }

    private void writeRangedNumberSet(RangedIntegerSetEncoder encoder, IntSet set) throws IOException {
        _obs.writeSet(encoder, encoder, encoder, encoder, composeJavaSet(set));
    }

    private void writeRangedNumberSet(HuffmanTable<Integer> lengthTable, ImmutableIntRange range, IntSet set) throws IOException {
        final RangedIntegerSetEncoder encoder = new RangedIntegerSetEncoder(_obs, lengthTable, range.min(), range.max());
        writeRangedNumberSet(encoder, set);
    }

    private ImmutableIntValueMap<String> readLanguageCodes() {
        final LangbookDbSchema.LanguagesTable langTable = LangbookDbSchema.Tables.languages;
        final DbResult langResult = _db.select(new DbQuery.Builder(langTable).select(langTable.getIdColumnIndex(), langTable.getCodeColumnIndex()));
        final ImmutableIntValueMap.Builder<String> langMapBuilder = new ImmutableIntValueMap.Builder<>();
        try {
            while (langResult.hasNext()) {
                final DbResult.Row row = langResult.next();
                final int id = row.get(0).toInt();
                final String code = row.get(1).toText();
                langMapBuilder.put(code, id);
            }
        }
        finally {
            langResult.close();
        }

        return langMapBuilder.build();
    }

    private static final class SymbolArrayWriterResult {
        final ImmutableIntPairMap idMap;
        final ImmutableIntValueMap<String> langMap;

        SymbolArrayWriterResult(ImmutableIntPairMap idMap, ImmutableIntValueMap<String> langMap) {
            this.idMap = idMap;
            this.langMap = langMap;
        }
    }

    private SymbolArrayWriterResult writeSymbolArrays(ImmutableIntValueMap<String> languageCodes) throws IOException {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        DbQuery query = new DbQuery.Builder(table)
                .select(table.getStrColumnIndex());

        DbResult result = _db.select(query);
        final MutableIntValueMap<Character> charFrequency = MutableIntValueMap.empty();
        final MutableIntPairMap lengthFrequency = MutableIntPairMap.empty();
        int count = 0;
        try {
            while (result.hasNext()) {
                ++count;
                final DbResult.Row row = result.next();
                final String str = row.get(0).toText();
                final int strLength = str.length();
                for (int i = 0; i < strLength; i++) {
                    final Character ch = str.charAt(i);
                    final int amount = charFrequency.get(ch, 0);
                    charFrequency.put(ch, amount + 1);
                }

                final int amount = lengthFrequency.get(strLength, 0);
                lengthFrequency.put(strLength, amount + 1);
            }
        }
        finally {
            result.close();
        }
        final int length = count;

        _obs.writeHuffmanSymbol(naturalNumberTable, length);

        final DefinedHuffmanTable<Character> charHuffmanTable = DefinedHuffmanTable.withFrequencies(composeJavaMap(charFrequency), new CharComparator());
        final DefinedHuffmanTable<Integer> symbolArraysLengthTable = DefinedHuffmanTable.withFrequencies(composeJavaMap(lengthFrequency), new IntComparator());

        final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
        final NaturalNumberHuffmanTable nat4Table = new NaturalNumberHuffmanTable(4);

        _obs.writeHuffmanTable(charHuffmanTable, new CharWriter(_obs), new CharHuffmanSymbolDiffWriter(_obs, nat4Table));
        _obs.writeHuffmanTable(symbolArraysLengthTable, new IntWriter(), new IntHuffmanSymbolDiffWriter(_obs, nat3Table));

        query = new DbQuery.Builder(table).select(table.getIdColumnIndex(), table.getStrColumnIndex());
        result = _db.select(query);
        final ImmutableIntValueMap.Builder<String> langCodeSymbolArrayBuilder = new ImmutableIntValueMap.Builder<>();
        final ImmutableSet<String> languageKeys = languageCodes.keySet();
        final ImmutableIntPairMap.Builder idMapBuilder = new ImmutableIntPairMap.Builder();
        count = 0;
        try {
            while (result.hasNext()) {
                DbResult.Row row = result.next();
                final int dbId = row.get(0).toInt();

                final String str = row.get(1).toText();
                if (languageKeys.contains(str)) {
                    langCodeSymbolArrayBuilder.put(str, count);
                }
                idMapBuilder.put(dbId, count++);

                final int strLength = str.length();
                _obs.writeHuffmanSymbol(symbolArraysLengthTable, strLength);

                for (int i = 0; i < strLength; i++) {
                    _obs.writeHuffmanSymbol(charHuffmanTable, str.charAt(i));
                }
            }
        }
        finally {
            result.close();
        }

        return new SymbolArrayWriterResult(idMapBuilder.build(), langCodeSymbolArrayBuilder.build());
    }

    private ImmutableIntRange writeLanguages(ImmutableIntPairMap symbolArraysIdMap, ImmutableIntValueMap<String> langMap) throws IOException {
        final LangbookDbSchema.AlphabetsTable alphabetsTable = LangbookDbSchema.Tables.alphabets;
        final DbResult alphabetResult = _db.select(new DbQuery.Builder(alphabetsTable).select(alphabetsTable.getIdColumnIndex(), alphabetsTable.getLanguageColumnIndex()));
        final MutableIntKeyMap<ImmutableIntSet> alphabetMap = MutableIntKeyMap.empty();
        final ImmutableIntSet emptySet = new ImmutableIntSetBuilder().build();
        int alphabetCount = 0;
        try {
            while (alphabetResult.hasNext()) {
                final DbResult.Row row = alphabetResult.next();
                final int alphabetDbId = row.get(0).toInt();
                final int langDbId = row.get(1).toInt();
                final ImmutableIntSet set = alphabetMap.get(langDbId, emptySet);
                alphabetMap.put(langDbId, set.add(alphabetDbId));
                ++alphabetCount;
            }
        }
        finally {
            alphabetResult.close();
        }

        final LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        final int langCount = getTableLength(table);
        _obs.writeHuffmanSymbol(naturalNumberTable, langCount);

        final int minSymbolArrayIndex = 0;
        final int maxSymbolArrayIndex = symbolArraysIdMap.size() - 1;
        final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(minSymbolArrayIndex,
                maxSymbolArrayIndex);

        final NaturalNumberHuffmanTable nat2Table = new NaturalNumberHuffmanTable(2);
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex(), table.getCodeColumnIndex());
        final DbResult result = _db.select(query);
        try {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                final int langDbId = row.get(0).toInt();
                final String code = row.get(1).toText();
                _obs.writeHuffmanSymbol(symbolArrayTable, langMap.get(code));
                _obs.writeHuffmanSymbol(nat2Table, alphabetMap.get(langDbId).size());
            }
        }
        finally {
            result.close();
        }

        final int minValidAlphabet = StreamedDatabaseConstants.minValidAlphabet;
        return new ImmutableIntRange(minValidAlphabet, minValidAlphabet + alphabetCount - 1);
    }

    private static final class IntPair {
        final int source;
        final int target;

        IntPair(int source, int target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public int hashCode() {
            return source * 37 + target;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof IntPair)) {
                return false;
            }

            IntPair that = (IntPair) other;
            return source == that.source && target == that.target;
        }
    }

    private void writeConversions(ImmutableIntRange validAlphabets, ImmutableIntPairMap symbolArraysIdMap) throws IOException {
        final LangbookDbSchema.ConversionsTable table = LangbookDbSchema.Tables.conversions;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getSourceAlphabetColumnIndex(), table.getTargetAlphabetColumnIndex(), table.getSourceColumnIndex(), table.getTargetColumnIndex());
        final DbResult result = _db.select(query);
        final ImmutableMap.Builder<IntPair, ImmutableList<IntPair>> builder = new ImmutableMap.Builder<>();
        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                IntPair alphabetPair = new IntPair(row.get(0).toInt(), row.get(1).toInt());
                ImmutableList.Builder<IntPair> arrayPairs = new ImmutableList.Builder<>();
                arrayPairs.add(new IntPair(row.get(2).toInt(), row.get(3).toInt()));

                while (result.hasNext()) {
                    row = result.next();
                    final int alphabetSource = row.get(0).toInt();
                    final int alphabetTarget = row.get(1).toInt();
                    final int source = row.get(2).toInt();
                    final int target = row.get(3).toInt();

                    if (alphabetPair.source != alphabetSource || alphabetPair.target != alphabetTarget) {
                        builder.put(alphabetPair, arrayPairs.build());

                        alphabetPair = new IntPair(alphabetSource, alphabetTarget);
                        arrayPairs = new ImmutableList.Builder<>();
                    }
                    arrayPairs.add(new IntPair(source, target));
                }

                builder.put(alphabetPair, arrayPairs.build());
            }
        }
        finally {
            result.close();
        }
        final ImmutableMap<IntPair, ImmutableList<IntPair>> conversions = builder.build();

        final ImmutableIntRange validSymbolArrays = new ImmutableIntRange(0, symbolArraysIdMap.size() - 1);
        final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(validSymbolArrays.min(), validSymbolArrays.max());
        _obs.writeHuffmanSymbol(naturalNumberTable, conversions.size());

        int minSourceAlphabet = validAlphabets.min();
        int minTargetAlphabet = minSourceAlphabet;
        for (ImmutableMap.Entry<IntPair, ImmutableList<IntPair>> entry : conversions.entries()) {
            final RangedIntegerHuffmanTable sourceAlphabetTable = new RangedIntegerHuffmanTable(minSourceAlphabet, validAlphabets.max());
            final int sourceAlphabet = entry.getKey().source;
            _obs.writeHuffmanSymbol(sourceAlphabetTable, sourceAlphabet);

            if (minSourceAlphabet != sourceAlphabet) {
                minTargetAlphabet = validAlphabets.min();
                minSourceAlphabet = sourceAlphabet;
            }

            final RangedIntegerHuffmanTable targetAlphabetTable = new RangedIntegerHuffmanTable(minTargetAlphabet, validAlphabets.max());
            final int targetAlphabet = entry.getKey().target;
            _obs.writeHuffmanSymbol(targetAlphabetTable, targetAlphabet);
            minTargetAlphabet = targetAlphabet + 1;

            _obs.writeHuffmanSymbol(naturalNumberTable, entry.getValue().size());
            for (IntPair pair : entry.getValue()) {
                _obs.writeHuffmanSymbol(symbolArrayTable, symbolArraysIdMap.get(pair.source));
                _obs.writeHuffmanSymbol(symbolArrayTable, symbolArraysIdMap.get(pair.target));
            }
        }
    }

    private static final class AcceptationWordConceptRanges {
        final ImmutableIntRange words;
        final ImmutableIntRange concepts;

        AcceptationWordConceptRanges(ImmutableIntRange words, ImmutableIntRange concepts) {
            this.words = words;
            this.concepts = concepts;
        }
    }

    private AcceptationWordConceptRanges getRangesFromAcceptations() {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getWordColumnIndex(), table.getConceptColumnIndex());
        final DbResult result = _db.select(query);
        int minWord = Integer.MAX_VALUE;
        int maxWord = Integer.MIN_VALUE;
        int minConcept = Integer.MAX_VALUE;
        int maxConcept = Integer.MIN_VALUE;
        try {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                final int word = row.get(0).toInt();
                final int concept = row.get(1).toInt();
                if (word < minWord) {
                    minWord = word;
                }

                if (word > maxWord) {
                    maxWord = word;
                }

                if (concept < minConcept) {
                    minConcept = concept;
                }

                if (concept > maxConcept) {
                    maxConcept = concept;
                }
            }
        }
        finally {
            result.close();
        }

        return new AcceptationWordConceptRanges(
                new ImmutableIntRange(minWord, maxWord),
                new ImmutableIntRange(minConcept, maxConcept));
    }

    private ImmutableIntPairMap writeCorrelations(ImmutableIntRange validAlphabets, ImmutableIntPairMap symbolArraysIdMap) throws IOException {
        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getCorrelationIdColumnIndex(),
                table.getAlphabetColumnIndex(),
                table.getSymbolArrayColumnIndex());
        final DbResult result = _db.select(query);
        final ImmutableIntKeyMap.Builder<ImmutableIntPairMap> builder = new ImmutableIntKeyMap.Builder<>();
        final ImmutableIntPairMap.Builder idMapBuilder = new ImmutableIntPairMap.Builder();
        final MutableIntPairMap lengthFrequencies = MutableIntPairMap.empty();
        int setCount = 0;
        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                ImmutableIntPairMap.Builder setBuilder = new ImmutableIntPairMap.Builder();
                int setId = row.get(0).toInt();
                setBuilder.put(row.get(1).toInt(), symbolArraysIdMap.get(row.get(2).toInt()));

                while (result.hasNext()) {
                    row = result.next();
                    int newSetId = row.get(0).toInt();
                    if (newSetId != setId) {
                        final ImmutableIntPairMap set = setBuilder.build();
                        final int setLength = set.size();
                        final int amount = lengthFrequencies.get(setLength, 0);
                        lengthFrequencies.put(setLength, amount + 1);

                        builder.put(setId, set);
                        idMapBuilder.put(setId, setCount++);
                        setBuilder = new ImmutableIntPairMap.Builder();
                        setId = newSetId;
                    }

                    setBuilder.put(row.get(1).toInt(), symbolArraysIdMap.get(row.get(2).toInt()));
                }

                final ImmutableIntPairMap set = setBuilder.build();
                final int setLength = set.size();
                final int amount = lengthFrequencies.get(setLength, 0);
                lengthFrequencies.put(setLength, amount + 1);

                builder.put(setId, set);
                idMapBuilder.put(setId, setCount);
            }
        }
        finally {
            result.close();
        }

        final DefinedHuffmanTable<Integer> lengthTable = DefinedHuffmanTable.withFrequencies(
                composeJavaMap(lengthFrequencies), new IntComparator());
        final RangedIntegerSetEncoder keyEncoder = new RangedIntegerSetEncoder(_obs,
                lengthTable, validAlphabets.min(), validAlphabets.max());
        final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(0, symbolArraysIdMap.size() - 1);
        final ValueEncoder<Integer> symbolArrayEncoder = new ValueEncoder<>(symbolArrayTable);

        final ImmutableIntKeyMap<ImmutableIntPairMap> correlations = builder.build();
        _obs.writeHuffmanSymbol(naturalNumberTable, correlations.size());

        boolean tableWritten = false;
        for (ImmutableIntPairMap corr : correlations) {
            if (!tableWritten) {
                final IntegerEncoder intEncoder = new IntegerEncoder(_obs);
                _obs.writeHuffmanTable(lengthTable, intEncoder, intEncoder);
                tableWritten = true;
            }

            _obs.writeMap(keyEncoder, keyEncoder, keyEncoder, keyEncoder, symbolArrayEncoder, composeJavaMap(corr));
        }

        return idMapBuilder.build();
    }

    private ImmutableIntPairMap writeCorrelationArrays(ImmutableIntPairMap correlationIdMap) throws IOException {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getArrayIdColumnIndex(),
                table.getCorrelationColumnIndex());
        final DbResult result = _db.select(query);
        final ImmutableList.Builder<ImmutableIntList> builder = new ImmutableList.Builder<>();
        final MutableIntPairMap lengthFrequencies = MutableIntPairMap.empty();
        final ImmutableIntPairMap.Builder idMapBuilder = new ImmutableIntPairMap.Builder();
        int index = 0;
        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                int arrayId = row.get(0).toInt();
                ImmutableIntList.Builder arrayBuilder = new ImmutableIntList.Builder();
                arrayBuilder.add(correlationIdMap.get(row.get(1).toInt()));

                while (result.hasNext()) {
                    row = result.next();
                    int newArrayId = row.get(0).toInt();
                    if (newArrayId != arrayId) {
                        final ImmutableIntList array = arrayBuilder.build();
                        final int length = array.size();
                        final int amount = lengthFrequencies.get(length, 0);
                        lengthFrequencies.put(length, amount + 1);

                        builder.add(array);
                        idMapBuilder.put(arrayId, index++);

                        arrayId = newArrayId;
                        arrayBuilder = new ImmutableIntList.Builder();
                    }

                    arrayBuilder.add(correlationIdMap.get(row.get(1).toInt()));
                }

                final ImmutableIntList array = arrayBuilder.build();
                final int length = array.size();
                final int amount = lengthFrequencies.get(length, 0);
                lengthFrequencies.put(length, amount + 1);

                builder.add(array);
                idMapBuilder.put(arrayId, index++);
            }
        }
        finally {
            result.close();
        }
        final ImmutableList<ImmutableIntList> corrArrays = builder.build();
        _obs.writeHuffmanSymbol(naturalNumberTable, index);

        if (!corrArrays.isEmpty()) {
            final DefinedHuffmanTable<Integer> lengthTable = DefinedHuffmanTable.withFrequencies(
                    composeJavaMap(lengthFrequencies), new IntComparator());
            final RangedIntegerHuffmanTable correlationTable =
                    new RangedIntegerHuffmanTable(0, correlationIdMap.size() - 1);

            final IntegerEncoder intEncoder = new IntegerEncoder(_obs);
            _obs.writeHuffmanTable(lengthTable, intEncoder, intEncoder);

            for (ImmutableIntList array : corrArrays) {
                _obs.writeHuffmanSymbol(lengthTable, array.size());
                for (int value : array) {
                    _obs.writeHuffmanSymbol(correlationTable, value);
                }
            }
        }

        return idMapBuilder.build();
    }

    private ImmutableIntPairMap writeAcceptations(ImmutableIntRange validWords, ImmutableIntRange validConcepts, ImmutableIntPairMap correlationArrayIdMap) throws IOException {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final int length = getTableLength(table);
        _obs.writeHuffmanSymbol(naturalNumberTable, length);

        final ImmutableIntPairMap.Builder idMapBuilder = new ImmutableIntPairMap.Builder();
        if (length > 0) {
            // The streamed database file allow having more than one representation for acceptation.
            // However, the database schema only allows 1 per each acceptation.
            // Because of that, the length table will have a single element, as all will be have length 1.
            final Map<Integer, Integer> dummyFrequencyMap =
                    composeJavaMap(new ImmutableIntPairMap.Builder().put(1, 50).build());
            final DefinedHuffmanTable<Integer> lengthTable =
                    DefinedHuffmanTable.withFrequencies(dummyFrequencyMap, new IntComparator());
            final IntegerEncoder intEncoder = new IntegerEncoder(_obs);
            _obs.writeHuffmanTable(lengthTable, intEncoder, intEncoder);

            final DbQuery query = new DbQuery.Builder(table).select(
                    table.getIdColumnIndex(),
                    table.getWordColumnIndex(),
                    table.getConceptColumnIndex(),
                    table.getCorrelationArrayColumnIndex());
            final DbResult result = _db.select(query);

            int index = 0;
            try {
                final RangedIntegerHuffmanTable wordTable =
                        new RangedIntegerHuffmanTable(validWords.min(), validWords.max());
                final RangedIntegerHuffmanTable conceptTable =
                        new RangedIntegerHuffmanTable(validConcepts.min(), validConcepts.max());

                while (result.hasNext()) {
                    final DbResult.Row row = result.next();
                    idMapBuilder.put(row.get(0).toInt(), index++);
                    _obs.writeHuffmanSymbol(wordTable, row.get(1).toInt());
                    _obs.writeHuffmanSymbol(conceptTable, row.get(2).toInt());

                    // Here the number of correlation arrays within the acceptation should be written.
                    // As length is always 1, it is expected that this will never include anything
                    // into the stream. So, it should not be required
                    final RangedIntegerHuffmanTable corrArrayTable = new RangedIntegerHuffmanTable(0, correlationArrayIdMap.size() - 1);
                    _obs.writeHuffmanSymbol(corrArrayTable, correlationArrayIdMap.get(row.get(3).toInt()));
                }
            } finally {
                result.close();
            }
        }

        return idMapBuilder.build();
    }

    private void writeBunchConcepts(ImmutableIntRange validConcepts) throws IOException {
        final LangbookDbSchema.BunchConceptsTable table = LangbookDbSchema.Tables.bunchConcepts;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getBunchColumnIndex(),
                table.getConceptColumnIndex());
        final DbResult result = _db.select(query);
        final MutableIntKeyMap<MutableIntSet> bunches = MutableIntKeyMap.empty();
        try {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                final int bunchId = row.get(0).toInt();
                final MutableIntSet set;
                if (bunches.keySet().contains(bunchId)) {
                    set = bunches.get(bunchId);
                }
                else {
                    set = MutableIntSet.empty();
                    bunches.put(bunchId, set);
                }
                set.add(row.get(1).toInt());
            }
        }
        finally {
            result.close();
        }
        _obs.writeHuffmanSymbol(naturalNumberTable, bunches.size());

        if (!bunches.isEmpty()) {
            final MutableIntPairMap lengthFrequencies = MutableIntPairMap.empty();
            for (IntSet set : bunches) {
                final int length = set.size();
                final int amount = lengthFrequencies.get(length, 0);
                lengthFrequencies.put(length, amount + 1);
            }

            final DefinedHuffmanTable<Integer> lengthTable = DefinedHuffmanTable.withFrequencies(composeJavaMap(lengthFrequencies), new IntComparator());
            _obs.writeHuffmanTable(lengthTable, new IntWriter(), new IntDiffWriter());

            final RangedIntegerSetEncoder encoder = new RangedIntegerSetEncoder(_obs, lengthTable, validConcepts.min(), validConcepts.max());
            int remainingBunches = bunches.size();
            int minBunchConcept = validConcepts.min();
            for (IntKeyMap.Entry<MutableIntSet> entry : bunches.entries()) {
                final RangedIntegerHuffmanTable bunchTable = new RangedIntegerHuffmanTable(minBunchConcept, validConcepts.max() - remainingBunches + 1);
                _obs.writeHuffmanSymbol(bunchTable, entry.getKey());
                minBunchConcept = entry.getKey() + 1;
                --remainingBunches;

                writeRangedNumberSet(encoder, entry.getValue());
            }
        }
    }

    private ImmutableIntKeyMap<ImmutableIntSet> getBunchSets() {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getSetIdColumnIndex(),
                table.getBunchColumnIndex());
        final DbResult result = _db.select(query);
        final MutableIntKeyMap<ImmutableIntSet> bunchSets = MutableIntKeyMap.empty();
        try {
            final ImmutableIntSet emptySet = new ImmutableIntSetBuilder().build();
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                final int setId = row.get(0).toInt();
                final int bunch = row.get(1).toInt();

                final ImmutableIntSet set = bunchSets.get(setId, emptySet);
                bunchSets.put(setId, set.add(bunch));
            }
        }
        finally {
            result.close();
        }

        return bunchSets.toImmutable();
    }

    private void writeAgents(int maxConcept, ImmutableIntPairMap correlationIdMap) throws IOException {
        final ImmutableIntKeyMap<ImmutableIntSet> bunchSets = getBunchSets();

        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        DbQuery query = new DbQuery.Builder(table).select(
                table.getSourceBunchSetColumnIndex(),
                table.getDiffBunchSetColumnIndex());
        DbResult result = _db.select(query);
        final MutableIntPairMap bunchSetLengthFrequencyMap = MutableIntPairMap.empty();
        int count = 0;
        try {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                final int sourceBunchSet = row.get(0).toInt();
                final int diffBunchSet = row.get(1).toInt();

                final int sourceBunchSetLength = bunchSets.get(sourceBunchSet).size();
                final int diffBunchSetLength = bunchSets.get(diffBunchSet).size();

                int amount = bunchSetLengthFrequencyMap.get(sourceBunchSetLength, 0);
                bunchSetLengthFrequencyMap.put(sourceBunchSetLength, amount + 1);

                amount = bunchSetLengthFrequencyMap.get(diffBunchSetLength, 0);
                bunchSetLengthFrequencyMap.put(diffBunchSetLength, amount + 1);
                count++;
            }
        }
        finally {
            result.close();
        }

        final int agentCount = count;
        _obs.writeHuffmanSymbol(naturalNumberTable, agentCount);

        if (agentCount != 0) {
            final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
            final IntWriter intWriter = new IntWriter(nat3Table);
            final DefinedHuffmanTable<Integer> sourceSetLengthTable = DefinedHuffmanTable.withFrequencies(composeJavaMap(bunchSetLengthFrequencyMap), new IntComparator());
            _obs.writeHuffmanTable(sourceSetLengthTable, intWriter, null);

            query = new DbQuery.Builder(table).select(
                    table.getTargetBunchColumnIndex(),
                    table.getSourceBunchSetColumnIndex(),
                    table.getMatcherColumnIndex(),
                    table.getAdderColumnIndex(),
                    table.getRuleColumnIndex(),
                    table.getFlagsColumnIndex());
            result = _db.select(query);
            try {
                final RangedIntegerHuffmanTable correlationTable = new RangedIntegerHuffmanTable(0, correlationIdMap.size() - 1);
                int lastTarget = StreamedDatabaseConstants.nullBunchId;
                int minSource = StreamedDatabaseConstants.minValidConcept;
                while (result.hasNext()) {
                    final DbResult.Row row = result.next();
                    final int targetBunch = row.get(0).toInt();
                    final int sourceBunchSetId = row.get(1).toInt();
                    final int matcher = row.get(2).toInt();
                    final int adder = row.get(3).toInt();
                    final int rule = row.get(4).toInt();
                    final int flags = row.get(5).toInt();

                    final RangedIntegerHuffmanTable targetBunchTable = new RangedIntegerHuffmanTable(lastTarget, maxConcept);
                    _obs.writeHuffmanSymbol(targetBunchTable, targetBunch);

                    if (targetBunch != lastTarget) {
                        minSource = StreamedDatabaseConstants.minValidConcept;
                    }

                    final RangedIntegerSetEncoder encoder = new RangedIntegerSetEncoder(_obs, sourceSetLengthTable, minSource, maxConcept);
                    final IntSet sourceBunchSet = bunchSets.get(sourceBunchSetId);
                    writeRangedNumberSet(encoder, sourceBunchSet);

                    if (!sourceBunchSet.isEmpty()) {
                        minSource = sourceBunchSet.min();
                    }

                    _obs.writeHuffmanSymbol(correlationTable, correlationIdMap.get(matcher));
                    _obs.writeHuffmanSymbol(correlationTable, correlationIdMap.get(adder));

                    // Rule and flags are still missing. However there must be an error here as the adder
                    // could match the empty correlation. But the empty correlation has not been included
                    // within the file in writeCorrelations method.
                    // TODO: Ensure that the empty correlation is present and implement the rest of the logic to write agents
                }
            }
            finally {
                result.close();
            }
        }
    }

    private void writeBunchAcceptations(ImmutableIntRange validConcepts, ImmutableIntPairMap accIdMap) throws IOException {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getBunchColumnIndex(),
                table.getAcceptationColumnIndex());
        final DbResult result = _db.select(query);
        final MutableIntKeyMap<MutableIntSet> bunches = MutableIntKeyMap.empty();
        try {
            while (result.hasNext()) {
                final DbResult.Row row = result.next();
                final int bunchId = row.get(0).toInt();
                final MutableIntSet set;
                if (bunches.keySet().contains(bunchId)) {
                    set = bunches.get(bunchId);
                }
                else {
                    set = MutableIntSet.empty();
                    bunches.put(bunchId, set);
                }
                set.add(accIdMap.get(row.get(1).toInt()));
            }
        }
        finally {
            result.close();
        }
        _obs.writeHuffmanSymbol(naturalNumberTable, bunches.size());

        if (!bunches.isEmpty()) {
            final MutableIntPairMap lengthFrequencies = MutableIntPairMap.empty();
            for (IntSet set : bunches) {
                final int length = set.size();
                final int amount = lengthFrequencies.get(length, 0);
                lengthFrequencies.put(length, amount + 1);
            }

            final DefinedHuffmanTable<Integer> lengthTable = DefinedHuffmanTable.withFrequencies(composeJavaMap(lengthFrequencies), new IntComparator());
            _obs.writeHuffmanTable(lengthTable, new IntWriter(), new IntDiffWriter());

            final RangedIntegerSetEncoder encoder = new RangedIntegerSetEncoder(_obs, lengthTable, 0, accIdMap.size() - 1);
            int maxBunchConcept = validConcepts.max() - bunches.size() + 1;
            int minBunchConcept = validConcepts.min();
            for (IntKeyMap.Entry<MutableIntSet> entry : bunches.entries()) {
                final RangedIntegerHuffmanTable bunchTable = new RangedIntegerHuffmanTable(minBunchConcept, maxBunchConcept);
                _obs.writeHuffmanSymbol(bunchTable, entry.getKey());
                minBunchConcept = entry.getKey() + 1;
                ++maxBunchConcept;

                writeRangedNumberSet(encoder, entry.getValue());
            }
        }
    }

    public void write() throws IOException {
        final ImmutableIntValueMap<String> langCodes = readLanguageCodes();
        final SymbolArrayWriterResult symbolArrayWriterResult = writeSymbolArrays(langCodes);
        final ImmutableIntPairMap symbolArrayIdMap = symbolArrayWriterResult.idMap;
        final ImmutableIntRange validAlphabets = writeLanguages(symbolArrayIdMap, symbolArrayWriterResult.langMap);
        writeConversions(validAlphabets, symbolArrayIdMap);

        final AcceptationWordConceptRanges accRanges = getRangesFromAcceptations();
        _obs.writeHuffmanSymbol(naturalNumberTable, accRanges.words.max() + 1);

        // TODO: Languages, Alphabets, Bunches and Rules are concepts as well and they
        // should be considered into the count of the maximum concept
        _obs.writeHuffmanSymbol(naturalNumberTable, accRanges.concepts.max() + 1);

        final ImmutableIntPairMap correlationIdMap = writeCorrelations(validAlphabets, symbolArrayIdMap);
        final ImmutableIntPairMap correlationArrayIdMap = writeCorrelationArrays(correlationIdMap);

        // TODO: Check if this is already required and accRanges.words cannot be used instead
        final ImmutableIntRange validWords = new ImmutableIntRange(StreamedDatabaseConstants.minValidWord, accRanges.words.max());

        // TODO: Check if this is already required and accRanges.concepts cannot be used instead
        final ImmutableIntRange validConcepts = new ImmutableIntRange(StreamedDatabaseConstants.minValidConcept, accRanges.concepts.max());
        final ImmutableIntPairMap accIdMap = writeAcceptations(validWords, validConcepts, correlationArrayIdMap);
        writeBunchConcepts(validConcepts);
        writeBunchAcceptations(validConcepts, accIdMap);
        writeAgents(validConcepts.max(), correlationIdMap);
        _obs.close();
    }
}
