package sword.langbook3.android.sdb;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import sword.bitstream.IntegerEncoder;
import sword.bitstream.NaturalEncoder;
import sword.bitstream.OutputBitStream;
import sword.bitstream.Procedure2WithIOException;
import sword.bitstream.ProcedureWithIOException;
import sword.bitstream.RangedIntegerSetEncoder;
import sword.bitstream.huffman.CharHuffmanTable;
import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;
import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.ImmutableIntValueHashMap;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.IntValueMap;
import sword.collections.List;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntPairMap;
import sword.collections.MutableIntSet;
import sword.collections.MutableIntValueHashMap;
import sword.collections.MutableIntValueSortedMap;
import sword.langbook3.android.LangbookDbSchema;
import sword.langbook3.android.db.DbExporter.Database;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbTable;
import sword.langbook3.android.db.DbValue;

import static sword.langbook3.android.sdb.StreamedDatabaseReader.naturalNumberTable;

public final class StreamedDatabaseWriter {

    private final Database _db;
    private final OutputBitStream _obs;
    private final ProgressListener _listener;

    /**
     * Reserved set id for the empty correlation.
     * This is expected not to be present in the Database.
     */
    private final int nullCorrelationSetId = 0;

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
            result.put(entry.key(), entry.value());
        }
        return result;
    }

    private java.util.Map<Integer, Integer> composeJavaMap(IntPairMap map) {
        final HashMap<Integer, Integer> result = new HashMap<>();
        for (IntPairMap.Entry entry : map.entries()) {
            result.put(entry.key(), entry.value());
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
        final ImmutableIntValueHashMap.Builder<String> langMapBuilder = new ImmutableIntValueHashMap.Builder<>();
        try {
            while (langResult.hasNext()) {
                final List<DbValue> row = langResult.next();
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
        final ImmutableIntPairMap lengths;

        SymbolArrayWriterResult(ImmutableIntPairMap idMap, ImmutableIntValueMap<String> langMap, ImmutableIntPairMap lengths) {
            this.idMap = idMap;
            this.langMap = langMap;
            this.lengths = lengths;
        }
    }

    private SymbolArrayWriterResult writeSymbolArrays(ImmutableIntSet exportable, ImmutableIntValueMap<String> languageCodes) throws IOException {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex(), table.getStrColumnIndex());

        final MutableIntValueHashMap<Character> charFrequency = MutableIntValueHashMap.empty();
        final MutableIntPairMap lengthFrequency = MutableIntPairMap.empty();
        int count = 0;
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                if (exportable.contains(row.get(0).toInt())) {
                    ++count;
                    final String str = row.get(1).toText();
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
        final ImmutableIntValueHashMap.Builder<String> langCodeSymbolArrayBuilder = new ImmutableIntValueHashMap.Builder<>();
        final ImmutableSet<String> languageKeys = languageCodes.keySet();
        final ImmutableIntPairMap.Builder idMapBuilder = new ImmutableIntPairMap.Builder();
        final ImmutableIntPairMap.Builder lengthMapBuilder = new ImmutableIntPairMap.Builder();

        count = 0;
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                List<DbValue> row = result.next();
                final int dbId = row.get(0).toInt();
                if (exportable.contains(dbId)) {
                    final String str = row.get(1).toText();
                    if (languageKeys.contains(str)) {
                        langCodeSymbolArrayBuilder.put(str, count);
                    }
                    idMapBuilder.put(dbId, count++);

                    final int strLength = str.length();
                    _obs.writeHuffmanSymbol(symbolArraysLengthTable, strLength);
                    lengthMapBuilder.put(dbId, strLength);

                    for (int i = 0; i < strLength; i++) {
                        _obs.writeHuffmanSymbol(charHuffmanTable, str.charAt(i));
                    }
                }
            }
        }

        return new SymbolArrayWriterResult(idMapBuilder.build(), langCodeSymbolArrayBuilder.build(), lengthMapBuilder.build());
    }

    private ImmutableIntRange writeLanguages(ImmutableIntPairMap symbolArraysIdMap, ImmutableIntValueMap<String> langMap) throws IOException {
        final LangbookDbSchema.AlphabetsTable alphabetsTable = LangbookDbSchema.Tables.alphabets;
        final DbResult alphabetResult = _db.select(new DbQuery.Builder(alphabetsTable).select(alphabetsTable.getIdColumnIndex(), alphabetsTable.getLanguageColumnIndex()));
        final MutableIntKeyMap<ImmutableIntSet> alphabetMap = MutableIntKeyMap.empty();
        final ImmutableIntSet emptySet = new ImmutableIntSetBuilder().build();
        int alphabetCount = 0;
        try {
            while (alphabetResult.hasNext()) {
                final List<DbValue> row = alphabetResult.next();
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
                final List<DbValue> row = result.next();
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
        final ImmutableHashMap.Builder<IntPair, ImmutableList<IntPair>> builder = new ImmutableHashMap.Builder<>();
        try {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
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
            final int sourceAlphabet = entry.key().source;
            _obs.writeHuffmanSymbol(sourceAlphabetTable, sourceAlphabet);

            if (minSourceAlphabet != sourceAlphabet) {
                minTargetAlphabet = validAlphabets.min();
                minSourceAlphabet = sourceAlphabet;
            }

            final RangedIntegerHuffmanTable targetAlphabetTable = new RangedIntegerHuffmanTable(minTargetAlphabet, validAlphabets.max());
            final int targetAlphabet = entry.key().target;
            _obs.writeHuffmanSymbol(targetAlphabetTable, targetAlphabet);
            minTargetAlphabet = targetAlphabet + 1;

            _obs.writeHuffmanSymbol(naturalNumberTable, entry.value().size());
            for (IntPair pair : entry.value()) {
                _obs.writeHuffmanSymbol(symbolArrayTable, symbolArraysIdMap.get(pair.source));
                _obs.writeHuffmanSymbol(symbolArrayTable, symbolArraysIdMap.get(pair.target));
            }
        }
    }

    private ImmutableIntRange getConceptRangeFromAcceptations() {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getConceptColumnIndex());
        final DbResult result = _db.select(query);
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        try {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int concept = row.get(0).toInt();
                if (concept < min) {
                    min = concept;
                }

                if (concept > max) {
                    max = concept;
                }
            }
        }
        finally {
            result.close();
        }

        return new ImmutableIntRange(min, max);
    }

    private ImmutableIntPairMap writeCorrelations(ImmutableIntSet exportable, ImmutableIntRange validAlphabets, ImmutableIntSet excludedAlphabets, ImmutableIntPairMap symbolArraysIdMap) throws IOException {
        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getCorrelationIdColumnIndex(),
                table.getAlphabetColumnIndex(),
                table.getSymbolArrayColumnIndex());

        final ImmutableIntKeyMap.Builder<ImmutableIntPairMap> builder = new ImmutableIntKeyMap.Builder<>();
        final ImmutableIntPairMap.Builder idMapBuilder = new ImmutableIntPairMap.Builder();
        final MutableIntPairMap lengthFrequencies = MutableIntPairMap.empty();
        int setCount = 0;

        final boolean shouldEmptyCorrelationBePresent = exportable.contains(nullCorrelationSetId);
        if (shouldEmptyCorrelationBePresent) {
            builder.put(nullCorrelationSetId, ImmutableIntPairMap.empty());
            idMapBuilder.put(nullCorrelationSetId, setCount++);
            lengthFrequencies.put(0, 1);
        }

        try (DbResult result = _db.select(query)) {
            if (result.hasNext()) {
                List<DbValue> row = result.next();
                ImmutableIntPairMap.Builder setBuilder = new ImmutableIntPairMap.Builder();
                int setId = row.get(0).toInt();
                if (setId == nullCorrelationSetId) {
                    throw new AssertionError("setId " + nullCorrelationSetId + " should be reserved for the empty one");
                }
                boolean isSetExportable = exportable.contains(setId);

                int alphabet = row.get(1).toInt();
                if (isSetExportable && !excludedAlphabets.contains(alphabet)) {
                    setBuilder.put(alphabet, symbolArraysIdMap.get(row.get(2).toInt()));
                }

                while (result.hasNext()) {
                    row = result.next();
                    int newSetId = row.get(0).toInt();
                    if (newSetId == nullCorrelationSetId) {
                        throw new AssertionError("setId " + nullCorrelationSetId + " should be reserved for the empty one");
                    }

                    if (newSetId != setId) {
                        if (isSetExportable) {
                            final ImmutableIntPairMap set = setBuilder.build();
                            final int setLength = set.size();
                            final int amount = lengthFrequencies.get(setLength, 0);
                            lengthFrequencies.put(setLength, amount + 1);

                            builder.put(setId, set);
                            idMapBuilder.put(setId, setCount++);
                        }

                        setId = newSetId;
                        isSetExportable = exportable.contains(setId);
                        setBuilder = isSetExportable? new ImmutableIntPairMap.Builder() : null;
                    }

                    alphabet = row.get(1).toInt();
                    if (isSetExportable && !excludedAlphabets.contains(alphabet)) {
                        setBuilder.put(alphabet, symbolArraysIdMap.get(row.get(2).toInt()));
                    }
                }

                if (isSetExportable) {
                    final ImmutableIntPairMap set = setBuilder.build();
                    final int setLength = set.size();
                    final int amount = lengthFrequencies.get(setLength, 0);
                    lengthFrequencies.put(setLength, amount + 1);

                    builder.put(setId, set);
                    idMapBuilder.put(setId, setCount++);
                }
            }
        }

        final DefinedHuffmanTable<Integer> lengthTable = DefinedHuffmanTable.withFrequencies(
                composeJavaMap(lengthFrequencies), new IntComparator());
        final RangedIntegerSetEncoder keyEncoder = new RangedIntegerSetEncoder(_obs,
                lengthTable, validAlphabets.min(), validAlphabets.max());
        final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(0, symbolArraysIdMap.size() - 1);
        final ValueEncoder<Integer> symbolArrayEncoder = new ValueEncoder<>(symbolArrayTable);

        final ImmutableIntKeyMap<ImmutableIntPairMap> correlations = builder.build();
        if (setCount != exportable.size()) {
            throw new AssertionError();
        }

        _obs.writeHuffmanSymbol(naturalNumberTable, setCount);

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

    private ImmutableIntPairMap writeCorrelationArrays(ImmutableIntSet exportable, ImmutableIntPairMap correlationIdMap) throws IOException {
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
                List<DbValue> row = result.next();
                int arrayId = row.get(0).toInt();
                boolean isExportable = exportable.contains(arrayId);
                ImmutableIntList.Builder arrayBuilder = isExportable? new ImmutableIntList.Builder() : null;
                if (isExportable) {
                    arrayBuilder.add(correlationIdMap.get(row.get(1).toInt()));
                }

                while (result.hasNext()) {
                    row = result.next();
                    int newArrayId = row.get(0).toInt();
                    if (newArrayId != arrayId) {
                        if (isExportable) {
                            final ImmutableIntList array = arrayBuilder.build();
                            final int length = array.size();
                            final int amount = lengthFrequencies.get(length, 0);
                            lengthFrequencies.put(length, amount + 1);

                            builder.add(array);
                            idMapBuilder.put(arrayId, index++);
                        }

                        arrayId = newArrayId;
                        isExportable = exportable.contains(arrayId);
                        arrayBuilder = isExportable? new ImmutableIntList.Builder() : null;
                    }

                    if (isExportable) {
                        arrayBuilder.add(correlationIdMap.get(row.get(1).toInt()));
                    }
                }

                if (isExportable) {
                    final ImmutableIntList array = arrayBuilder.build();
                    final int length = array.size();
                    final int amount = lengthFrequencies.get(length, 0);
                    lengthFrequencies.put(length, amount + 1);

                    builder.add(array);
                    idMapBuilder.put(arrayId, index++);
                }
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

    private ImmutableIntPairMap writeAcceptations(ImmutableIntSet exportable, ImmutableIntRange validConcepts, ImmutableIntPairMap correlationArrayIdMap) throws IOException {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final int length = exportable.size();
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
                    table.getConceptColumnIndex(),
                    table.getCorrelationArrayColumnIndex());
            final DbResult result = _db.select(query);

            int index = 0;
            try {
                final RangedIntegerHuffmanTable conceptTable =
                        new RangedIntegerHuffmanTable(validConcepts.min(), validConcepts.max());

                while (result.hasNext()) {
                    final List<DbValue> row = result.next();
                    final int accId = row.get(0).toInt();
                    if (exportable.contains(accId)) {
                        idMapBuilder.put(accId, index++);
                        _obs.writeHuffmanSymbol(conceptTable, row.get(1).toInt());

                        // Here the number of correlation arrays within the acceptation should be written.
                        // As length is always 1, it is expected that this will never include anything
                        // into the stream. So, it should not be required
                        final RangedIntegerHuffmanTable corrArrayTable = new RangedIntegerHuffmanTable(0,
                                correlationArrayIdMap.size() - 1);
                        _obs.writeHuffmanSymbol(corrArrayTable, correlationArrayIdMap.get(row.get(2).toInt()));
                    }
                }
            } finally {
                result.close();
            }

            if (index != length) {
                throw new AssertionError();
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
                final List<DbValue> row = result.next();
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
            final NaturalEncoder natEncoder = new NaturalEncoder(_obs);
            _obs.writeHuffmanTable(lengthTable, natEncoder, natEncoder);

            final RangedIntegerSetEncoder encoder = new RangedIntegerSetEncoder(_obs, lengthTable, validConcepts.min(), validConcepts.max());
            int remainingBunches = bunches.size();
            int minBunchConcept = validConcepts.min();
            for (IntKeyMap.Entry<MutableIntSet> entry : bunches.entries()) {
                final RangedIntegerHuffmanTable bunchTable = new RangedIntegerHuffmanTable(minBunchConcept, validConcepts.max() - remainingBunches + 1);
                _obs.writeHuffmanSymbol(bunchTable, entry.key());
                minBunchConcept = entry.key() + 1;
                --remainingBunches;

                writeRangedNumberSet(encoder, entry.value());
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
                final List<DbValue> row = result.next();
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

    private ImmutableIntSet writeAgents(int maxConcept, ImmutableIntPairMap correlationIdMap) throws IOException {
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
                final List<DbValue> row = result.next();
                final int sourceBunchSet = row.get(0).toInt();
                final int diffBunchSet = row.get(1).toInt();

                final ImmutableIntSet sourceSet = bunchSets.get(sourceBunchSet, null);
                final ImmutableIntSet diffSet = bunchSets.get(diffBunchSet, null);

                final int sourceBunchSetLength = (sourceSet != null)? sourceSet.size() : 0;
                final int diffBunchSetLength = (diffSet != null)? diffSet.size() : 0;

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

        final MutableIntSet presentRules = MutableIntSet.empty();
        if (agentCount != 0) {
            final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
            final IntWriter intWriter = new IntWriter(nat3Table);
            final DefinedHuffmanTable<Integer> sourceSetLengthTable = DefinedHuffmanTable.withFrequencies(composeJavaMap(bunchSetLengthFrequencyMap), new IntComparator());
            _obs.writeHuffmanTable(sourceSetLengthTable, intWriter, null);

            final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(
                    StreamedDatabaseConstants.minValidConcept, maxConcept);

            query = new DbQuery.Builder(table).select(
                    table.getTargetBunchColumnIndex(),
                    table.getSourceBunchSetColumnIndex(),
                    table.getStartMatcherColumnIndex(),
                    table.getStartAdderColumnIndex(),
                    table.getEndMatcherColumnIndex(),
                    table.getEndAdderColumnIndex(),
                    table.getRuleColumnIndex());
            result = _db.select(query);
            try {
                final RangedIntegerHuffmanTable correlationTable = new RangedIntegerHuffmanTable(0, correlationIdMap.size() - 1);
                int lastTarget = StreamedDatabaseConstants.nullBunchId;
                int minSource = StreamedDatabaseConstants.minValidConcept;
                while (result.hasNext()) {
                    final List<DbValue> row = result.next();
                    final int targetBunch = row.get(0).toInt();
                    final int sourceBunchSetId = row.get(1).toInt();
                    final int startMatcher = row.get(2).toInt();
                    final int startAdder = row.get(3).toInt();
                    final int endMatcher = row.get(4).toInt();
                    final int endAdder = row.get(5).toInt();
                    final int rule = row.get(6).toInt();

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

                    _obs.writeHuffmanSymbol(correlationTable, correlationIdMap.get(startMatcher));
                    _obs.writeHuffmanSymbol(correlationTable, correlationIdMap.get(startAdder));
                    _obs.writeHuffmanSymbol(correlationTable, correlationIdMap.get(endMatcher));
                    _obs.writeHuffmanSymbol(correlationTable, correlationIdMap.get(endAdder));

                    final boolean hasRule = startMatcher != startAdder || endMatcher != endAdder;
                    if (hasRule) {
                        _obs.writeHuffmanSymbol(conceptTable, rule);
                        presentRules.add(rule);
                    }

                    lastTarget = targetBunch;
                }
            }
            finally {
                result.close();
            }
        }

        return presentRules.toImmutable();
    }

    private ImmutableIntSet getCorrelationSetIds() {
        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getCorrelationIdColumnIndex());
        final DbResult result = _db.select(query);
        final ImmutableIntSetBuilder setIdBuilder = new ImmutableIntSetBuilder();
        try {
            while (result.hasNext()) {
                setIdBuilder.add(result.next().get(0).toInt());
            }
        }
        finally {
            result.close();
        }

        return setIdBuilder.build();
    }

    private void writeBunchAcceptations(ImmutableIntRange validConcepts, ImmutableIntSet agentSetIds, ImmutableIntPairMap accIdMap) throws IOException {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getBunchColumnIndex(),
                table.getAcceptationColumnIndex(),
                table.getAgentSetColumnIndex());
        final DbResult result = _db.select(query);
        final MutableIntKeyMap<MutableIntSet> bunches = MutableIntKeyMap.empty();
        try {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int agentSetId = row.get(2).toInt();
                if (!agentSetIds.contains(agentSetId)) {
                    if (agentSetId != LangbookDbSchema.Tables.agentSets.nullReference()) {
                        throw new AssertionError();
                    }

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
            final NaturalEncoder natEncoder = new NaturalEncoder(_obs);
            _obs.writeHuffmanTable(lengthTable, natEncoder, natEncoder);

            final RangedIntegerSetEncoder encoder = new RangedIntegerSetEncoder(_obs, lengthTable, 0, accIdMap.size() - 1);
            int maxBunchConcept = validConcepts.max() - bunches.size() + 1;
            int minBunchConcept = validConcepts.min();
            for (IntKeyMap.Entry<MutableIntSet> entry : bunches.entries()) {
                final RangedIntegerHuffmanTable bunchTable = new RangedIntegerHuffmanTable(minBunchConcept, maxBunchConcept);
                _obs.writeHuffmanSymbol(bunchTable, entry.key());
                minBunchConcept = entry.key() + 1;
                ++maxBunchConcept;

                writeRangedNumberSet(encoder, entry.value());
            }
        }
    }

    private void setProgress(float progress, String message) {
        if (_listener != null) {
            _listener.setProgress(progress, message);
        }
    }

    private ImmutableIntSet listRuledAcceptations() {
        final LangbookDbSchema.RuledAcceptationsTable table = LangbookDbSchema.Tables.ruledAcceptations;
        final DbQuery query = new DbQuery.Builder(table).select(table.getIdColumnIndex());
        return _db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private static class ExportableAcceptationsAndCorrelationArrays {
        final ImmutableIntSet acceptations;
        final ImmutableIntSet correlationArrays;

        ExportableAcceptationsAndCorrelationArrays(ImmutableIntSet acceptations, ImmutableIntSet correlationArrays) {
            this.acceptations = acceptations;
            this.correlationArrays = correlationArrays;
        }
    }

    private ExportableAcceptationsAndCorrelationArrays listExportableAcceptationsAndCorrelationArrays() {
        final ImmutableIntSet ruledAcceptations = listRuledAcceptations();

        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getIdColumnIndex(), table.getCorrelationArrayColumnIndex());
        final DbResult result = _db.select(query);

        final ImmutableIntSetBuilder acceptations = new ImmutableIntSetBuilder();
        final ImmutableIntSetBuilder correlationArrays = new ImmutableIntSetBuilder();
        try {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int accId = row.get(0).toInt();

                if (!ruledAcceptations.contains(accId)) {
                    acceptations.add(accId);
                    correlationArrays.add(row.get(1).toInt());
                }
            }
        }
        finally {
            result.close();
        }

        return new ExportableAcceptationsAndCorrelationArrays(acceptations.build(), correlationArrays.build());
    }

    private ImmutableIntSet listAgentCorrelations() {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(table).select(table.getStartMatcherColumnIndex(),
                table.getStartAdderColumnIndex(), table.getEndMatcherColumnIndex(), table.getEndAdderColumnIndex());
        final DbResult result = _db.select(query);

        final ImmutableIntSetBuilder correlations = new ImmutableIntSetBuilder();
        try {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                correlations.add(row.get(0).toInt());
                correlations.add(row.get(1).toInt());
                correlations.add(row.get(2).toInt());
                correlations.add(row.get(3).toInt());
            }
        }
        finally {
            result.close();
        }

        return correlations.build();
    }

    private ImmutableIntSet listExportableCorrelations(ImmutableIntSet correlationArrays) {
        final ImmutableIntSetBuilder correlations = new ImmutableIntSetBuilder();
        for (int corrId : listAgentCorrelations()) {
            correlations.add(corrId);
        }

        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table).select(table.getArrayIdColumnIndex(), table.getCorrelationColumnIndex());
        final DbResult result = _db.select(query);

        try {
            while(result.hasNext()) {
                final List<DbValue> row = result.next();
                if (correlationArrays.contains(row.get(0).toInt())) {
                    correlations.add(row.get(1).toInt());
                }
            }
        }
        finally {
            result.close();
        }

        return correlations.build();
    }

    private ImmutableIntSet listConversionSymbolArrays(MutableIntSet targetedAlphabets) {
        final LangbookDbSchema.ConversionsTable table = LangbookDbSchema.Tables.conversions;
        final DbQuery query = new DbQuery.Builder(table).select(table.getTargetAlphabetColumnIndex(), table.getSourceColumnIndex(), table.getTargetColumnIndex());
        final DbResult result = _db.select(query);

        final ImmutableIntSetBuilder symbolArrays = new ImmutableIntSetBuilder();
        try {
            while(result.hasNext()) {
                final List<DbValue> row = result.next();
                targetedAlphabets.add(row.get(0).toInt());
                symbolArrays.add(row.get(1).toInt());
                symbolArrays.add(row.get(2).toInt());
            }
        }
        finally {
            result.close();
        }

        return symbolArrays.build();
    }

    private static final class ExportableSymbolArraysResult {
        final ImmutableIntSet symbolArrays;
        final ImmutableIntSet excludedAlphabets;

        ExportableSymbolArraysResult(ImmutableIntSet symbolArrays, ImmutableIntSet excludedAlphabets) {
            this.symbolArrays = symbolArrays;
            this.excludedAlphabets = excludedAlphabets;
        }
    }

    private ImmutableIntSet listSymbolArraysForLanguageCodes() {
        final LangbookDbSchema.LanguagesTable langTable = LangbookDbSchema.Tables.languages;
        final LangbookDbSchema.SymbolArraysTable symbolArraysTable = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(langTable)
                .join(symbolArraysTable, langTable.getCodeColumnIndex(), symbolArraysTable.getStrColumnIndex())
                .select(langTable.columns().size() + symbolArraysTable.getIdColumnIndex());
        final DbResult result = _db.select(query);
        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }
        finally {
            result.close();
        }

        return builder.build();
    }

    private ImmutableIntSet listSymbolArraysForSentenceSpans() {
        final LangbookDbSchema.SpanTable table = LangbookDbSchema.Tables.spans;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getSymbolArray());
        final ImmutableIntSetBuilder builder = new ImmutableIntSetBuilder();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }
        return builder.build();
    }

    private ExportableSymbolArraysResult listExportableSymbolArrays(ImmutableIntSet correlations) {
        final MutableIntSet targetedAlphabets = MutableIntSet.empty();
        final ImmutableIntSetBuilder symbolArrays = new ImmutableIntSetBuilder();
        for (int arrayId : listConversionSymbolArrays(targetedAlphabets)) {
            symbolArrays.add(arrayId);
        }

        for (int arrayId : listSymbolArraysForLanguageCodes()) {
            symbolArrays.add(arrayId);
        }

        final LangbookDbSchema.CorrelationsTable table = LangbookDbSchema.Tables.correlations;
        final DbQuery query = new DbQuery.Builder(table).select(table.getCorrelationIdColumnIndex(), table.getAlphabetColumnIndex(), table.getSymbolArrayColumnIndex());

        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                if (correlations.contains(row.get(0).toInt()) && !targetedAlphabets.contains(row.get(1).toInt())) {
                    symbolArrays.add(row.get(2).toInt());
                }
            }
        }

        for (int arrayId : listSymbolArraysForSentenceSpans()) {
            symbolArrays.add(arrayId);
        }

        return new ExportableSymbolArraysResult(symbolArrays.build(), targetedAlphabets.toImmutable());
    }

    private ImmutableIntSet listAgentSets() {
        final LangbookDbSchema.AgentSetsTable table = LangbookDbSchema.Tables.agentSets;
        final DbQuery query = new DbQuery.Builder(table).select(table.getSetIdColumnIndex());
        final DbResult result = _db.select(query);

        final ImmutableIntSetBuilder setIds = new ImmutableIntSetBuilder();
        try {
            while (result.hasNext()) {
                setIds.add(result.next().get(0).toInt());
            }
        }
        finally {
            result.close();
        }

        return setIds.build();
    }

    private static class RuleAcceptationPair {
        final int rule;
        final int acceptation;

        RuleAcceptationPair(int rule, int acceptation) {
            this.rule = rule;
            this.acceptation = acceptation;
        }

        @Override
        public int hashCode() {
            return rule * 37 + acceptation;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof RuleAcceptationPair)) {
                return false;
            }

            RuleAcceptationPair that = (RuleAcceptationPair) other;
            return rule == that.rule && acceptation == that.acceptation;
        }

        static boolean lessThan(RuleAcceptationPair a, RuleAcceptationPair b) {
            return b != null && (a == null || a.rule < b.rule || a.rule == b.rule && a.acceptation < b.acceptation);
        }
    }

    private void writeRelevantRuledAcceptations(MutableIntPairMap accIdMap, ImmutableIntSet presentRules) throws IOException {
        final ImmutableIntSet originalAccKeys = accIdMap.keySet().toImmutable();
        if (accIdMap.max() + 1 != accIdMap.size()) {
            throw new AssertionError();
        }

        final LangbookDbSchema.SpanTable spans = LangbookDbSchema.Tables.spans;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final LangbookDbSchema.AgentsTable agents = LangbookDbSchema.Tables.agents;
        final int ruledAccOffset = spans.columns().size();
        final int agentOffset = ruledAccOffset + ruledAcceptations.columns().size();
        final DbQuery query = new DbQuery.Builder(spans)
                .join(ruledAcceptations, spans.getAcceptation(), ruledAcceptations.getIdColumnIndex())
                .join(agents, ruledAccOffset + ruledAcceptations.getAgentColumnIndex(), agents.getIdColumnIndex())
                .select(spans.getAcceptation(), ruledAccOffset + ruledAcceptations.getAcceptationColumnIndex(), agentOffset + agents.getRuleColumnIndex());

        final MutableIntValueSortedMap<RuleAcceptationPair> pairs = MutableIntValueSortedMap.empty(RuleAcceptationPair::lessThan);
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int dynamicAcc = row.get(0).toInt();
                final int mainAcc = row.get(1).toInt();
                final int ruleIndex = presentRules.indexOf(row.get(2).toInt());

                if (ruleIndex < 0 || ruleIndex >= presentRules.size()) {
                    throw new AssertionError();
                }
                if (originalAccKeys.contains(dynamicAcc)) {
                    throw new AssertionError();
                }
                if (!originalAccKeys.contains(mainAcc)) {
                    throw new AssertionError();
                }

                pairs.put(new RuleAcceptationPair(ruleIndex, accIdMap.get(mainAcc)), dynamicAcc);
            }
        }

        final int pairsCount = pairs.size();
        _obs.writeHuffmanSymbol(naturalNumberTable, pairsCount);
        if (pairsCount > 0) {
            final int mainAccCount = originalAccKeys.size();
            final int maxPresentRule = presentRules.size() - 1;
            int accIdMapSize = accIdMap.size();
            int previousRule = 0;
            int firstPossibleAcc = 0;
            for (int pairIndex = 0; pairIndex < pairsCount; pairIndex++) {
                final RuleAcceptationPair pair = pairs.keyAt(pairIndex);
                final RangedIntegerHuffmanTable rulesTable = new RangedIntegerHuffmanTable(previousRule, maxPresentRule);
                _obs.writeHuffmanSymbol(rulesTable, pair.rule);
                if (pair.rule != previousRule) {
                    firstPossibleAcc = 0;
                }

                final RangedIntegerHuffmanTable mainAcceptationTable = new RangedIntegerHuffmanTable(firstPossibleAcc, mainAccCount - 1);
                _obs.writeHuffmanSymbol(mainAcceptationTable, pair.acceptation);

                previousRule = pair.rule;
                firstPossibleAcc = pair.acceptation + 1;

                accIdMap.put(pairs.valueAt(pairIndex), accIdMapSize++);
            }
        }
    }

    private static final class SentenceSpan {
        final int symbolArray;
        final int start;
        final int length;
        final int acceptation;

        SentenceSpan(int symbolArray, int start, int length, int acceptation) {
            this.symbolArray = symbolArray;
            this.start = start;
            this.length = length;
            this.acceptation = acceptation;
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
            return symbolArray == that.symbolArray && start == that.start && length == that.length && acceptation == that.acceptation;
        }

        static boolean lessThan(SentenceSpan a, SentenceSpan b) {
            return b != null && (a == null || a.symbolArray < b.symbolArray || a.symbolArray == b.symbolArray && (
                    a.start < b.start || a.start == b.start && (a.length < b.length || a.length == b.length && a.acceptation < b.acceptation)));
        }
    }

    private void writeSentenceSpans(IntPairMap accIdMap, IntPairMap symbolArrayIdMap, IntPairMap symbolArrayLengths) throws IOException {
        final LangbookDbSchema.SpanTable table = LangbookDbSchema.Tables.spans;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getSymbolArray(), table.getStart(), table.getLength(), table.getAcceptation());

        final ImmutableSet.Builder<SentenceSpan> builder = new ImmutableHashSet.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int symbolArray = row.get(0).toInt();
                final int start = row.get(1).toInt();
                final int length = row.get(2).toInt();
                final int acc = row.get(3).toInt();
                builder.add(new SentenceSpan(symbolArray, start, length, acc));
            }
        }

        final int symbolArrayCount = symbolArrayLengths.size();
        final ImmutableSet<SentenceSpan> spans = builder.build().sort(SentenceSpan::lessThan);
        _obs.writeHuffmanSymbol(naturalNumberTable, spans.size());
        if (!spans.isEmpty()) {
            final RangedIntegerHuffmanTable accTable = new RangedIntegerHuffmanTable(0, accIdMap.size() - 1);
            int previousSymbolArray = 0;
            int previousStart = 0;

            for (SentenceSpan span : spans) {
                final int symbolArray = span.symbolArray;
                final int symbolArrayFileId = symbolArrayIdMap.get(symbolArray);
                final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(previousSymbolArray, symbolArrayCount - 1);
                _obs.writeHuffmanSymbol(symbolArrayTable, symbolArrayFileId);
                if (symbolArrayFileId != previousSymbolArray) {
                    previousStart = 0;
                }

                final int sentenceLength = symbolArrayLengths.get(symbolArray);
                final RangedIntegerHuffmanTable startTable = new RangedIntegerHuffmanTable(previousStart, sentenceLength - 1);
                _obs.writeHuffmanSymbol(startTable, span.start);

                final RangedIntegerHuffmanTable lengthTable = new RangedIntegerHuffmanTable(1, sentenceLength - span.start);
                _obs.writeHuffmanSymbol(lengthTable, span.length);

                _obs.writeHuffmanSymbol(accTable, accIdMap.get(span.acceptation));
                previousSymbolArray = symbolArrayFileId;
                previousStart = span.start;
            }
        }
    }

    public void write() throws IOException {
        setProgress(0.0f, "Filtering database");
        final ExportableAcceptationsAndCorrelationArrays exportable = listExportableAcceptationsAndCorrelationArrays();
        final ImmutableIntSet exportableCorrelations = listExportableCorrelations(exportable.correlationArrays);
        final ExportableSymbolArraysResult exportableSymbolArrays = listExportableSymbolArrays(exportableCorrelations);
        final ImmutableIntSet agentSetIds = listAgentSets();

        setProgress(0.1f, "Writing symbol arrays");
        final ImmutableIntValueMap<String> langCodes = readLanguageCodes();
        final SymbolArrayWriterResult symbolArrayWriterResult = writeSymbolArrays(exportableSymbolArrays.symbolArrays, langCodes);
        final ImmutableIntPairMap symbolArrayIdMap = symbolArrayWriterResult.idMap;
        final ImmutableIntRange validAlphabets = writeLanguages(symbolArrayIdMap, symbolArrayWriterResult.langMap);
        final ImmutableIntPairMap symbolArrayLengths = symbolArrayWriterResult.lengths;

        setProgress(0.2f, "Writing conversions");
        writeConversions(validAlphabets, symbolArrayIdMap);

        final ImmutableIntRange conceptRange = getConceptRangeFromAcceptations();
        // TODO: Languages, Alphabets, Bunches and Rules are concepts as well and they
        // should be considered into the count of the maximum concept
        _obs.writeHuffmanSymbol(naturalNumberTable, conceptRange.max() + 1);

        setProgress(0.25f, "Writing correlations");
        final ImmutableIntPairMap correlationIdMap = writeCorrelations(exportableCorrelations, validAlphabets, exportableSymbolArrays.excludedAlphabets, symbolArrayIdMap);

        setProgress(0.35f, "Writing correlation arrays");
        final ImmutableIntPairMap correlationArrayIdMap = writeCorrelationArrays(exportable.correlationArrays, correlationIdMap);

        // TODO: Check if this is already required and accRanges.concepts cannot be used instead
        final ImmutableIntRange validConcepts = new ImmutableIntRange(StreamedDatabaseConstants.minValidConcept, conceptRange.max());
        setProgress(0.5f, "Writing acceptations");
        final ImmutableIntPairMap accIdMap = writeAcceptations(exportable.acceptations, validConcepts, correlationArrayIdMap);

        setProgress(0.6f, "Writing bunch concepts");
        writeBunchConcepts(validConcepts);

        setProgress(0.7f, "Writing bunch acceptations");
        writeBunchAcceptations(validConcepts, agentSetIds, accIdMap);

        setProgress(0.8f, "Writing agents");
        final ImmutableIntSet presentRules = writeAgents(validConcepts.max(), correlationIdMap);

        setProgress(0.9f, "Writing dynamic acceptations");
        final MutableIntPairMap extendedAccIdMap = accIdMap.mutate();
        writeRelevantRuledAcceptations(extendedAccIdMap, presentRules);

        setProgress(0.93f, "Writing sentence spans");
        writeSentenceSpans(extendedAccIdMap, symbolArrayIdMap, symbolArrayLengths);

        _obs.close();
    }
}
