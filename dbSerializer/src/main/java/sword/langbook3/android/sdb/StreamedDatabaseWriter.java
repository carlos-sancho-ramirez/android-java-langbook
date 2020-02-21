package sword.langbook3.android.sdb;

import java.io.IOException;
import java.io.OutputStream;

import sword.bitstream.IntEncoder;
import sword.bitstream.IntProcedure2WithIOException;
import sword.bitstream.IntProcedureWithIOException;
import sword.bitstream.NatEncoder;
import sword.bitstream.OutputHuffmanStream;
import sword.bitstream.OutputStreamWrapper;
import sword.bitstream.Procedure2WithIOException;
import sword.bitstream.ProcedureWithIOException;
import sword.bitstream.RangedIntSetEncoder;
import sword.bitstream.huffman.CharHuffmanTable;
import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.DefinedIntHuffmanTable;
import sword.bitstream.huffman.IntHuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;
import sword.bitstream.huffman.RangedIntHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;
import sword.collections.ImmutableHashMap;
import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntArraySet;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntList;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetCreator;
import sword.collections.ImmutableList;
import sword.collections.ImmutableMap;
import sword.collections.ImmutableSet;
import sword.collections.ImmutableSortedSet;
import sword.collections.IntKeyMap;
import sword.collections.IntPairMap;
import sword.collections.IntSet;
import sword.collections.List;
import sword.collections.MutableHashSet;
import sword.collections.MutableIntArraySet;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntList;
import sword.collections.MutableIntPairMap;
import sword.collections.MutableIntSet;
import sword.collections.MutableIntValueHashMap;
import sword.collections.MutableIntValueSortedMap;
import sword.collections.MutableSet;
import sword.collections.MutableSortedSet;
import sword.collections.SortFunction;
import sword.database.DbExporter;
import sword.database.DbExporter.Database;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbTable;
import sword.database.DbValue;
import sword.langbook3.android.LanguageCodeRules;
import sword.langbook3.android.collections.ImmutableIntPair;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.models.AgentRegister;

import static sword.langbook3.android.sdb.StreamedDatabaseReader.naturalNumberTable;

public final class StreamedDatabaseWriter {

    private final Database _db;
    private final OutputStreamWrapper _obs;
    private final ProgressListener _listener;

    /**
     * Reserved set id for the empty correlation.
     * This is expected not to be present in the Database.
     */
    private final int nullCorrelationSetId = 0;

    private static class CharWriter implements ProcedureWithIOException<Character> {

        private final CharHuffmanTable _table = new CharHuffmanTable(8);
        private final OutputHuffmanStream _obs;

        CharWriter(OutputHuffmanStream obs) {
            _obs = obs;
        }

        @Override
        public void apply(Character ch) throws IOException {
            _obs.writeHuffmanSymbol(_table, ch);
        }
    }

    private static class CharHuffmanSymbolDiffWriter implements Procedure2WithIOException<Character> {

        private final OutputHuffmanStream _obs;
        private final NaturalNumberHuffmanTable _table;

        CharHuffmanSymbolDiffWriter(OutputHuffmanStream obs, NaturalNumberHuffmanTable table) {
            _obs = obs;
            _table = table;
        }

        @Override
        public void apply(Character previous, Character element) throws IOException {
            _obs.writeHuffmanSymbol(_table, element - previous - 1);
        }
    }

    private class IntWriter implements IntProcedureWithIOException {

        private final NaturalNumberHuffmanTable _table;

        IntWriter() {
            _table = naturalNumberTable;
        }

        IntWriter(NaturalNumberHuffmanTable table) {
            _table = table;
        }

        @Override
        public void apply(int value) throws IOException {
            _obs.writeHuffmanSymbol(_table, value);
        }
    }

    private static class IntHuffmanSymbolDiffWriter implements IntProcedure2WithIOException {

        private final OutputHuffmanStream _obs;
        private final NaturalNumberHuffmanTable _table;

        IntHuffmanSymbolDiffWriter(OutputHuffmanStream obs, NaturalNumberHuffmanTable table) {
            _obs = obs;
            _table = table;
        }

        @Override
        public void apply(int previous, int element) throws IOException {
            _obs.writeHuffmanSymbol(_table, element - previous - 1);
        }
    }

    private class IntValueEncoder implements IntProcedureWithIOException {

        private final IntHuffmanTable _table;

        IntValueEncoder(IntHuffmanTable table) {
            if (table == null) {
                throw new IllegalArgumentException();
            }

            _table = table;
        }

        @Override
        public void apply(int element) throws IOException {
            _obs.writeIntHuffmanSymbol(_table, element);
        }
    }

    public StreamedDatabaseWriter(Database db, OutputStream os, ProgressListener listener) {
        _db = db;
        _obs = new OutputStreamWrapper(os);
        _listener = listener;
    }

    private int getTableLength(DbTable table) {
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex());

        int count = 0;
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                ++count;
                result.next();
            }
        }

        return count;
    }

    private static final class SymbolArrayWriterResult {
        final ImmutableIntPairMap idMap;
        final ImmutableIntPairMap lengths;

        SymbolArrayWriterResult(ImmutableIntPairMap idMap, ImmutableIntPairMap lengths) {
            if (!idMap.keySet().equalSet(lengths.keySet())) {
                throw new IllegalArgumentException();
            }

            this.idMap = idMap;
            this.lengths = lengths;
        }
    }

    private SymbolArrayWriterResult writeSymbolArrays(ImmutableIntSet exportable) throws IOException {
        final LangbookDbSchema.SymbolArraysTable table = LangbookDbSchema.Tables.symbolArrays;
        final DbQuery query = new DbQuery.Builder(table)
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
        if (length == 0) {
            return new SymbolArrayWriterResult(ImmutableIntPairMap.empty(), ImmutableIntPairMap.empty());
        }
        else {
            final DefinedHuffmanTable<Character> charHuffmanTable = DefinedHuffmanTable.withFrequencies(charFrequency, (a, b) -> a < b);
            final DefinedIntHuffmanTable symbolArraysLengthTable = DefinedIntHuffmanTable.withFrequencies(lengthFrequency);

            final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
            final NaturalNumberHuffmanTable nat4Table = new NaturalNumberHuffmanTable(4);

            _obs.writeHuffmanTable(charHuffmanTable, new CharWriter(_obs), new CharHuffmanSymbolDiffWriter(_obs, nat4Table));
            _obs.writeIntHuffmanTable(symbolArraysLengthTable, new IntWriter(), new IntHuffmanSymbolDiffWriter(_obs, nat3Table));

            final ImmutableIntPairMap.Builder idMapBuilder = new ImmutableIntPairMap.Builder();
            final ImmutableIntPairMap.Builder lengthMapBuilder = new ImmutableIntPairMap.Builder();

            count = 0;
            try (DbResult result = _db.select(query)) {
                while (result.hasNext()) {
                    List<DbValue> row = result.next();
                    final int dbId = row.get(0).toInt();
                    if (exportable.contains(dbId)) {
                        final String str = row.get(1).toText();
                        idMapBuilder.put(dbId, count++);

                        final int strLength = str.length();
                        _obs.writeIntHuffmanSymbol(symbolArraysLengthTable, strLength);
                        lengthMapBuilder.put(dbId, strLength);

                        for (int i = 0; i < strLength; i++) {
                            _obs.writeHuffmanSymbol(charHuffmanTable, str.charAt(i));
                        }
                    }
                }
            }

            return new SymbolArrayWriterResult(idMapBuilder.build(), lengthMapBuilder.build());
        }
    }

    private static final class LanguageAndAlphabetsMapping {
        final ImmutableIntPairMap languages;
        final ImmutableIntPairMap alphabets;

        LanguageAndAlphabetsMapping(ImmutableIntPairMap languages, ImmutableIntPairMap alphabets) {
            this.languages = languages;
            this.alphabets = alphabets;
        }

        ImmutableIntPairMap composeConceptMap(ImmutableIntSet usedConcepts) {
            final MutableIntPairMap map = MutableIntPairMap.empty();
            for (IntPairMap.Entry entry : languages.entries()) {
                map.put(entry.key(), entry.value());
            }

            for (IntPairMap.Entry entry : alphabets.entries()) {
                map.put(entry.key(), entry.value());
            }

            int conceptIndex = StreamedDatabaseConstants.minValidConcept + languages.size() + alphabets.size();
            for (int concept : usedConcepts) {
                if (map.get(concept, 0) == 0) {
                    map.put(concept, conceptIndex++);
                }
            }

            return map.toImmutable();
        }
    }

    private LanguageAndAlphabetsMapping writeLanguages() throws IOException {
        final LangbookDbSchema.AlphabetsTable alphabetsTable = LangbookDbSchema.Tables.alphabets;
        DbQuery query = new DbQuery.Builder(alphabetsTable)
                .select(alphabetsTable.getIdColumnIndex(), alphabetsTable.getLanguageColumnIndex());

        final MutableIntKeyMap<ImmutableIntSet> alphabetMap = MutableIntKeyMap.empty();
        final ImmutableIntSet emptySet = new ImmutableIntSetCreator().build();
        try (DbResult alphabetResult = _db.select(query)) {
            while (alphabetResult.hasNext()) {
                final List<DbValue> row = alphabetResult.next();
                final int alphabetDbId = row.get(0).toInt();
                final int langDbId = row.get(1).toInt();
                final ImmutableIntSet set = alphabetMap.get(langDbId, emptySet);
                alphabetMap.put(langDbId, set.add(alphabetDbId));
            }
        }

        final LangbookDbSchema.LanguagesTable table = LangbookDbSchema.Tables.languages;
        final int langCount = getTableLength(table);
        _obs.writeHuffmanSymbol(naturalNumberTable, langCount);

        final NaturalNumberHuffmanTable nat2Table = new NaturalNumberHuffmanTable(2);
        query = new DbQuery.Builder(table)
                .select(table.getIdColumnIndex(), table.getCodeColumnIndex());

        try (DbResult result = _db.select(query)) {
            final RangedIntegerHuffmanTable codeSymbolTable = new RangedIntegerHuffmanTable('a', 'z');
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int langDbId = row.get(0).toInt();
                final String code = row.get(1).toText();

                if (code.length() != LanguageCodeRules.LENGTH) {
                    throw new AssertionError();
                }

                for (int i = 0; i < LanguageCodeRules.LENGTH; i++) {
                    _obs.writeHuffmanSymbol(codeSymbolTable, (int) code.charAt(i));
                }

                _obs.writeHuffmanSymbol(nat2Table, alphabetMap.get(langDbId).size());
            }
        }

        final ImmutableIntPairMap.Builder languagesBuilder = new ImmutableIntPairMap.Builder();
        final ImmutableIntPairMap.Builder alphabetsBuilder = new ImmutableIntPairMap.Builder();
        final int languageCount = alphabetMap.size();
        int alphabetIndex = StreamedDatabaseConstants.minValidConcept + languageCount;
        for (int i = 0; i < languageCount; i++) {
            languagesBuilder.put(alphabetMap.keyAt(i), StreamedDatabaseConstants.minValidConcept + i);
            final ImmutableIntSet langAlphabets = alphabetMap.valueAt(i);
            for (int j = 0; j < langAlphabets.size(); j++) {
                alphabetsBuilder.put(langAlphabets.valueAt(j), alphabetIndex++);
            }
        }

        return new LanguageAndAlphabetsMapping(languagesBuilder.build(), alphabetsBuilder.build());
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
            if (!(other instanceof IntPair)) {
                return false;
            }

            IntPair that = (IntPair) other;
            return source == that.source && target == that.target;
        }
    }

    private void writeConversions(ImmutableIntPairMap alphabetIdMap, ImmutableIntPairMap symbolArraysIdMap) throws IOException {
        final LangbookDbSchema.ConversionsTable table = LangbookDbSchema.Tables.conversions;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getSourceAlphabetColumnIndex(),
                table.getTargetAlphabetColumnIndex(),
                table.getSourceColumnIndex(),
                table.getTargetColumnIndex());

        final ImmutableHashMap.Builder<IntPair, ImmutableList<IntPair>> builder = new ImmutableHashMap.Builder<>();
        try (DbResult result = _db.select(query)) {
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

        final ImmutableMap<IntPair, ImmutableList<IntPair>> conversions = builder.build();

        final ImmutableIntRange validSymbolArrays = new ImmutableIntRange(0, symbolArraysIdMap.size() - 1);
        final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(validSymbolArrays.min(), validSymbolArrays.max());
        _obs.writeHuffmanSymbol(naturalNumberTable, conversions.size());

        int minSourceAlphabetIndex = alphabetIdMap.min();
        int minTargetAlphabetIndex = minSourceAlphabetIndex;
        for (ImmutableMap.Entry<IntPair, ImmutableList<IntPair>> entry : conversions.entries()) {
            final RangedIntegerHuffmanTable sourceAlphabetTable = new RangedIntegerHuffmanTable(minSourceAlphabetIndex, alphabetIdMap.max());
            final int sourceAlphabetIndex = alphabetIdMap.get(entry.key().source);
            _obs.writeHuffmanSymbol(sourceAlphabetTable, sourceAlphabetIndex);

            if (minSourceAlphabetIndex != sourceAlphabetIndex) {
                minTargetAlphabetIndex = alphabetIdMap.min();
                minSourceAlphabetIndex = sourceAlphabetIndex;
            }

            final RangedIntegerHuffmanTable targetAlphabetTable = new RangedIntegerHuffmanTable(minTargetAlphabetIndex, alphabetIdMap.max());
            final int targetAlphabetIndex = alphabetIdMap.get(entry.key().target);
            _obs.writeHuffmanSymbol(targetAlphabetTable, targetAlphabetIndex);
            minTargetAlphabetIndex = targetAlphabetIndex + 1;

            _obs.writeHuffmanSymbol(naturalNumberTable, entry.value().size());
            for (IntPair pair : entry.value()) {
                _obs.writeHuffmanSymbol(symbolArrayTable, symbolArraysIdMap.get(pair.source));
                _obs.writeHuffmanSymbol(symbolArrayTable, symbolArraysIdMap.get(pair.target));
            }
        }
    }

    private ImmutableIntPairMap writeCorrelations(ImmutableIntSet exportable, ImmutableIntPairMap alphabetIdMap, ImmutableIntSet excludedAlphabets, ImmutableIntPairMap symbolArraysIdMap) throws IOException {
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

        if (setCount != exportable.size()) {
            throw new AssertionError();
        }

        _obs.writeHuffmanSymbol(naturalNumberTable, setCount);
        if (setCount > 0) {
            final DefinedIntHuffmanTable lengthTable = DefinedIntHuffmanTable.withFrequencies(lengthFrequencies);

            final IntEncoder intEncoder = new IntEncoder(_obs);
            _obs.writeIntHuffmanTable(lengthTable, intEncoder, intEncoder);

            final ImmutableIntRange keyRange = new ImmutableIntRange(alphabetIdMap.min(), alphabetIdMap.max());
            final RangedIntSetEncoder keyEncoder = new RangedIntSetEncoder(_obs, lengthTable, keyRange);

            final ImmutableIntRange symbolArraysIdRange = new ImmutableIntRange(0, symbolArraysIdMap.size() - 1);
            final RangedIntHuffmanTable symbolArrayTable = new RangedIntHuffmanTable(symbolArraysIdRange);
            final IntValueEncoder symbolArrayEncoder = new IntValueEncoder(symbolArrayTable);

            final ImmutableIntKeyMap<ImmutableIntPairMap> correlations = builder.build();

            for (ImmutableIntPairMap corr : correlations) {
                final MutableIntPairMap mutableMap = MutableIntPairMap.empty();
                for (IntPairMap.Entry entry : corr.entries()) {
                    mutableMap.put(alphabetIdMap.get(entry.key()), entry.value());
                }

                _obs.writeIntPairMap(keyEncoder, keyEncoder, keyEncoder, symbolArrayEncoder,
                        mutableMap);
            }
        }

        return idMapBuilder.build();
    }

    private ImmutableIntPairMap writeCorrelationArrays(ImmutableIntSet exportable, ImmutableIntPairMap correlationIdMap) throws IOException {
        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getArrayIdColumnIndex(),
                table.getCorrelationColumnIndex());

        final ImmutableList.Builder<ImmutableIntList> builder = new ImmutableList.Builder<>();
        final MutableIntPairMap lengthFrequencies = MutableIntPairMap.empty();
        final ImmutableIntPairMap.Builder idMapBuilder = new ImmutableIntPairMap.Builder();
        int index = 0;
        try (DbResult result = _db.select(query)) {
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

        final ImmutableList<ImmutableIntList> corrArrays = builder.build();
        _obs.writeHuffmanSymbol(naturalNumberTable, index);

        if (!corrArrays.isEmpty()) {
            final DefinedIntHuffmanTable lengthTable = DefinedIntHuffmanTable.withFrequencies(lengthFrequencies);
            final ImmutableIntRange range = new ImmutableIntRange(0, correlationIdMap.size() - 1);
            final RangedIntHuffmanTable correlationTable = new RangedIntHuffmanTable(range);

            final IntEncoder intEncoder = new IntEncoder(_obs);
            _obs.writeIntHuffmanTable(lengthTable, intEncoder, intEncoder);

            for (ImmutableIntList array : corrArrays) {
                _obs.writeIntHuffmanSymbol(lengthTable, array.size());
                for (int value : array) {
                    _obs.writeIntHuffmanSymbol(correlationTable, value);
                }
            }
        }

        return idMapBuilder.build();
    }

    private ImmutableIntPairMap writeAcceptations(ImmutableIntSet exportable, ImmutableIntPairMap conceptIdMap, ImmutableIntPairMap correlationArrayIdMap) throws IOException {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final int length = exportable.size();
        _obs.writeHuffmanSymbol(naturalNumberTable, length);

        final ImmutableIntPairMap.Builder idMapBuilder = new ImmutableIntPairMap.Builder();
        if (length > 0) {
            // The streamed database file allow having more than one representation for acceptation.
            // However, the database schema only allows 1 per each acceptation.
            // Because of that, the length table will have a single element, as all will be have length 1.
            final IntPairMap dummyFrequencyMap = new ImmutableIntPairMap.Builder().put(1, 50).build();
            final DefinedIntHuffmanTable lengthTable = DefinedIntHuffmanTable.withFrequencies(dummyFrequencyMap);
            final IntEncoder intEncoder = new IntEncoder(_obs);
            _obs.writeIntHuffmanTable(lengthTable, intEncoder, intEncoder);

            final DbQuery query = new DbQuery.Builder(table).select(
                    table.getIdColumnIndex(),
                    table.getConceptColumnIndex(),
                    table.getCorrelationArrayColumnIndex());

            int index = 0;
            try (DbResult result = _db.select(query)) {
                final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(1, conceptIdMap.size());

                while (result.hasNext()) {
                    final List<DbValue> row = result.next();
                    final int accId = row.get(0).toInt();
                    if (exportable.contains(accId)) {
                        idMapBuilder.put(accId, index++);
                        _obs.writeHuffmanSymbol(conceptTable, conceptIdMap.get(row.get(1).toInt()));

                        // Here the number of correlation arrays within the acceptation should be written.
                        // As length is always 1, it is expected that this will never include anything
                        // into the stream. So, it should not be required
                        final RangedIntegerHuffmanTable corrArrayTable = new RangedIntegerHuffmanTable(0,
                                correlationArrayIdMap.size() - 1);
                        _obs.writeHuffmanSymbol(corrArrayTable, correlationArrayIdMap.get(row.get(2).toInt()));
                    }
                }
            }

            if (index != length) {
                throw new AssertionError();
            }
        }

        return idMapBuilder.build();
    }

    private void writeComplementedConcepts(ImmutableIntPairMap conceptIdMap) throws IOException {
        final LangbookDbSchema.ComplementedConceptsTable table = LangbookDbSchema.Tables.complementedConcepts;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getBaseColumnIndex(),
                table.getIdColumnIndex(),
                table.getComplementColumnIndex());

        final MutableIntKeyMap<MutableSet<ImmutableIntPair>> bases = MutableIntKeyMap.empty();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int baseId = conceptIdMap.get(row.get(0).toInt());
                final MutableSet<ImmutableIntPair> set;
                if (bases.keySet().contains(baseId)) {
                    set = bases.get(baseId);
                }
                else {
                    set = MutableHashSet.empty();
                    bases.put(baseId, set);
                }

                final int complementedConcept = conceptIdMap.get(row.get(1).toInt());
                final int compositionId = row.get(2).toInt();
                set.add(new ImmutableIntPair(complementedConcept, compositionId));
            }
        }

        _obs.writeHuffmanSymbol(naturalNumberTable, bases.size());

        if (!bases.isEmpty()) {
            final MutableIntPairMap lengthFrequencies = MutableIntPairMap.empty();
            for (MutableSet<ImmutableIntPair> set : bases) {
                final int length = set.size();
                final int amount = lengthFrequencies.get(length, 0);
                lengthFrequencies.put(length, amount + 1);
            }

            final DefinedIntHuffmanTable lengthTable = DefinedIntHuffmanTable.withFrequencies(lengthFrequencies);
            final NatEncoder natEncoder = new NatEncoder(_obs);
            _obs.writeIntHuffmanTable(lengthTable, natEncoder, natEncoder);

            final ImmutableIntRange range = new ImmutableIntRange(StreamedDatabaseConstants.minValidConcept, StreamedDatabaseConstants.minValidConcept + conceptIdMap.size() - 1);
            final RangedIntSetEncoder encoder = new RangedIntSetEncoder(_obs, lengthTable, range);
            int remainingBunches = bases.size();
            int minBunchConcept = StreamedDatabaseConstants.minValidConcept;
            for (IntKeyMap.Entry<MutableSet<ImmutableIntPair>> entry : bases.entries()) {
                final RangedIntegerHuffmanTable bunchTable = new RangedIntegerHuffmanTable(minBunchConcept, StreamedDatabaseConstants.minValidConcept + conceptIdMap.size() - remainingBunches);
                _obs.writeHuffmanSymbol(bunchTable, entry.key());
                minBunchConcept = entry.key() + 1;
                --remainingBunches;

                final MutableIntKeyMap<ImmutableIntSet> innerMap = MutableIntKeyMap.empty();
                for (ImmutableIntPair pair : entry.value()) {
                    final ImmutableIntSet itemConcepts;
                    if (pair.right == 0) {
                        itemConcepts = ImmutableIntArraySet.empty();
                    }
                    else {
                        final LangbookDbSchema.ConceptCompositionsTable conceptCompositions = LangbookDbSchema.Tables.conceptCompositions;
                        final DbQuery compositionQuery = new DbQuery.Builder(conceptCompositions)
                                .where(conceptCompositions.getComposedColumnIndex(), pair.right)
                                .select(conceptCompositions.getItemColumnIndex());

                        final ImmutableIntSetCreator itemConceptsBuilder = new ImmutableIntSetCreator();
                        boolean hasItems = false;
                        try (DbResult dbResult = _db.select(compositionQuery)) {
                            while (dbResult.hasNext()) {
                                hasItems = true;
                                itemConceptsBuilder.add(conceptIdMap.get(dbResult.next().get(0).toInt()));
                            }
                        }

                        itemConcepts = hasItems? itemConceptsBuilder.build() : itemConceptsBuilder.add(conceptIdMap.get(pair.right)).build();
                    }

                    innerMap.put(pair.left, itemConcepts);
                }

                _obs.writeIntKeyMap(encoder, encoder, encoder, complements -> {
                    int minValidComplement = StreamedDatabaseConstants.minValidConcept;
                    final int maxValidComplement = StreamedDatabaseConstants.minValidConcept + conceptIdMap.size() - 1;
                    for (int complement : complements) {
                        _obs.writeBoolean(true);
                        final RangedIntegerHuffmanTable rangedTable = new RangedIntegerHuffmanTable(minValidComplement, maxValidComplement);
                        _obs.writeHuffmanSymbol(rangedTable, complement);
                        minValidComplement = complement + 1;
                    }

                    if (minValidComplement <= maxValidComplement) {
                        _obs.writeBoolean(false);
                    }
                }, innerMap);
            }
        }
    }

    private ImmutableIntKeyMap<ImmutableIntSet> getBunchSets(ImmutableIntPairMap conceptIdMap) {
        final LangbookDbSchema.BunchSetsTable table = LangbookDbSchema.Tables.bunchSets;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getSetIdColumnIndex(),
                table.getBunchColumnIndex());

        final MutableIntKeyMap<ImmutableIntSet> bunchSets = MutableIntKeyMap.empty();
        try (DbResult result = _db.select(query)) {
            final ImmutableIntSet emptySet = new ImmutableIntSetCreator().build();
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int setId = row.get(0).toInt();
                final int bunch = conceptIdMap.get(row.get(1).toInt());

                final ImmutableIntSet set = bunchSets.get(setId, emptySet);
                bunchSets.put(setId, set.add(bunch));
            }
        }

        return bunchSets.toImmutable();
    }

    private static ImmutableIntSet getConceptsInAcceptations(DbExporter.Database db) {
        final LangbookDbSchema.AcceptationsTable table = LangbookDbSchema.Tables.acceptations;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getConceptColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private static ImmutableIntSet getConceptsInRuledConcepts(DbExporter.Database db) {
        final LangbookDbSchema.RuledConceptsTable table = LangbookDbSchema.Tables.ruledConcepts;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getIdColumnIndex(),
                table.getRuleColumnIndex(),
                table.getConceptColumnIndex());

        final ImmutableIntSetCreator builder = new ImmutableIntSetCreator();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                builder.add(row.get(0).toInt());
                builder.add(row.get(1).toInt());
                builder.add(row.get(2).toInt());
            }
        }

        return builder.build();
    }

    private static ImmutableIntSet getConceptsFromAlphabets(DbExporter.Database db) {
        final LangbookDbSchema.AlphabetsTable table = LangbookDbSchema.Tables.alphabets;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getIdColumnIndex(),
                table.getLanguageColumnIndex());

        final ImmutableIntSetCreator builder = new ImmutableIntSetCreator();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                builder.add(row.get(0).toInt());
                builder.add(row.get(1).toInt());
            }
        }

        return builder.build();
    }

    private static ImmutableIntSet getConceptsInComplementedConcepts(DbExporter.Database db) {
        final LangbookDbSchema.ComplementedConceptsTable table = LangbookDbSchema.Tables.complementedConcepts;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getIdColumnIndex(),
                table.getBaseColumnIndex(),
                table.getComplementColumnIndex());

        final ImmutableIntSetCreator builder = new ImmutableIntSetCreator();
        try (DbResult dbResult = db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                builder.add(row.get(0).toInt());
                builder.add(row.get(1).toInt());

                final int complement = row.get(2).toInt();
                if (complement != 0) {
                    builder.add(complement);
                }
            }
        }

        return builder.build();
    }

    private static ImmutableIntSet getConceptsInConceptCompositions(DbExporter.Database db) {
        final LangbookDbSchema.ConceptCompositionsTable table = LangbookDbSchema.Tables.conceptCompositions;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getItemColumnIndex());
        return db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private static ImmutableIntSet getUsedConcepts(DbExporter.Database db) {
        final ImmutableIntSet set = getConceptsInAcceptations(db)
                .addAll(getConceptsInRuledConcepts(db))
                .addAll(getConceptsFromAlphabets(db))
                .addAll(getConceptsInComplementedConcepts(db))
                .addAll(getConceptsInConceptCompositions(db));

        if (set.contains(0)) {
            throw new AssertionError();
        }

        return set;
    }

    private ImmutableIntList writeAgents(ImmutableIntPairMap conceptIdMap, ImmutableIntPairMap correlationIdMap) throws IOException {
        final ImmutableIntKeyMap<ImmutableIntSet> bunchSets = getBunchSets(conceptIdMap);
        final ImmutableIntSet emptySet = ImmutableIntArraySet.empty();

        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(table).select(
                        table.getIdColumnIndex(),
                        table.getTargetBunchColumnIndex(),
                        table.getSourceBunchSetColumnIndex(),
                        table.getDiffBunchSetColumnIndex(),
                        table.getStartMatcherColumnIndex(),
                        table.getStartAdderColumnIndex(),
                        table.getEndMatcherColumnIndex(),
                        table.getEndAdderColumnIndex(),
                        table.getRuleColumnIndex());

        final SortFunction<AgentRegister> sortFunc = (a, b) -> a.targetBunch < b.targetBunch || a.targetBunch == b.targetBunch &&
                b.sourceBunchSetId != 0 && (a.sourceBunchSetId == 0 || bunchSets.get(a.sourceBunchSetId).min() < bunchSets.get(b.sourceBunchSetId).min());

        final ImmutableHashSet.Builder<AgentRegister> agentsBuilder = new ImmutableHashSet.Builder<>();
        final MutableIntValueHashMap<AgentRegister> agentIds = MutableIntValueHashMap.empty();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int agentId = row.get(0).toInt();
                final int rawTargetBunch = row.get(1).toInt();
                final int targetBunch = (rawTargetBunch == 0)? 0 : conceptIdMap.get(rawTargetBunch);
                final int sourceBunchSetId = row.get(2).toInt();
                final int diffBunchSetId = row.get(3).toInt();
                final int startMatcherId = row.get(4).toInt();
                final int startAdderId = row.get(5).toInt();
                final int endMatcherId = row.get(6).toInt();
                final int endAdderId = row.get(7).toInt();
                final int rule = row.get(8).toInt();
                final AgentRegister register = new AgentRegister(targetBunch, sourceBunchSetId, diffBunchSetId, startMatcherId, startAdderId, endMatcherId, endAdderId, rule);
                agentsBuilder.add(register);
                agentIds.put(register, agentId);
            }
        }
        final ImmutableSortedSet<AgentRegister> agents = agentsBuilder.build().sort(sortFunc);

        final ImmutableIntPairMap bunchSetLengthFrequencyMap = agents
                .mapToInt(agent -> bunchSets.get(agent.sourceBunchSetId, emptySet).size())
                .appendAll(agents.mapToInt(agent -> bunchSets.get(agent.diffBunchSetId, emptySet).size()))
                .count();

        final int agentCount = agents.size();
        _obs.writeHuffmanSymbol(naturalNumberTable, agentCount);

        final MutableIntList agentsWithRule = MutableIntList.empty();
        if (!agents.isEmpty()) {
            final NaturalNumberHuffmanTable nat3Table = new NaturalNumberHuffmanTable(3);
            final IntWriter intWriter = new IntWriter(nat3Table);
            final DefinedIntHuffmanTable sourceSetLengthTable = DefinedIntHuffmanTable.withFrequencies(bunchSetLengthFrequencyMap);
            _obs.writeIntHuffmanTable(sourceSetLengthTable, intWriter, null);

            int minSource = StreamedDatabaseConstants.minValidConcept;
            final int maxSource = StreamedDatabaseConstants.minValidConcept + conceptIdMap.size() - 1;
            final RangedIntegerHuffmanTable conceptTable = new RangedIntegerHuffmanTable(minSource, maxSource);

            final RangedIntegerHuffmanTable correlationTable = new RangedIntegerHuffmanTable(0, correlationIdMap.size() - 1);
            int lastTarget = 0;
            for (AgentRegister agent : agents) {
                final RangedIntegerHuffmanTable targetBunchTable = new RangedIntegerHuffmanTable(lastTarget, conceptIdMap.size());
                _obs.writeHuffmanSymbol(targetBunchTable, agent.targetBunch);

                if (agent.targetBunch != lastTarget) {
                    minSource = StreamedDatabaseConstants.minValidConcept;
                }

                final ImmutableIntRange sourceRange = new ImmutableIntRange(minSource, maxSource);
                final RangedIntSetEncoder sourceEncoder = new RangedIntSetEncoder(_obs, sourceSetLengthTable, sourceRange);
                final IntSet sourceBunchSet = (agent.sourceBunchSetId != 0)? bunchSets.get(agent.sourceBunchSetId) : emptySet;
                _obs.writeIntSet(sourceEncoder, sourceEncoder, sourceEncoder, sourceBunchSet);

                final ImmutableIntRange diffRange = new ImmutableIntRange(StreamedDatabaseConstants.minValidConcept, maxSource);
                final RangedIntSetEncoder diffEncoder = new RangedIntSetEncoder(_obs, sourceSetLengthTable, diffRange);
                final IntSet diffBunchSet = (agent.diffBunchSetId != 0)? bunchSets.get(agent.diffBunchSetId) : emptySet;
                _obs.writeIntSet(diffEncoder, diffEncoder, diffEncoder, diffBunchSet);

                if (!sourceBunchSet.isEmpty()) {
                    minSource = sourceBunchSet.min();
                }

                _obs.writeHuffmanSymbol(correlationTable, correlationIdMap.get(agent.startMatcherId));
                _obs.writeHuffmanSymbol(correlationTable, correlationIdMap.get(agent.startAdderId));
                _obs.writeHuffmanSymbol(correlationTable, correlationIdMap.get(agent.endMatcherId));
                _obs.writeHuffmanSymbol(correlationTable, correlationIdMap.get(agent.endAdderId));

                final boolean hasRule = agent.startMatcherId != agent.startAdderId || agent.endMatcherId != agent.endAdderId;
                if (hasRule) {
                    final int rule = conceptIdMap.get(agent.rule);
                    _obs.writeHuffmanSymbol(conceptTable, rule);
                    agentsWithRule.append(agentIds.get(agent));
                }

                lastTarget = agent.targetBunch;
            }
        }

        return agentsWithRule.toImmutable();
    }

    private void writeBunchAcceptations(ImmutableIntPairMap conceptIdMap, ImmutableIntPairMap accIdMap) throws IOException {
        final LangbookDbSchema.BunchAcceptationsTable table = LangbookDbSchema.Tables.bunchAcceptations;
        final DbQuery query = new DbQuery.Builder(table).select(
                table.getBunchColumnIndex(),
                table.getAcceptationColumnIndex(),
                table.getAgentColumnIndex());

        final MutableIntKeyMap<MutableIntSet> bunches = MutableIntKeyMap.empty();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int agentId = row.get(2).toInt();
                if (agentId == 0) {
                    final int bunchId = conceptIdMap.get(row.get(0).toInt());
                    final MutableIntSet set;
                    if (bunches.keySet().contains(bunchId)) {
                        set = bunches.get(bunchId);
                    }
                    else {
                        set = MutableIntArraySet.empty();
                        bunches.put(bunchId, set);
                    }

                    set.add(accIdMap.get(row.get(1).toInt()));
                }
            }
        }

        _obs.writeHuffmanSymbol(naturalNumberTable, bunches.size());

        if (!bunches.isEmpty()) {
            final MutableIntPairMap lengthFrequencies = MutableIntPairMap.empty();
            for (IntSet set : bunches) {
                final int length = set.size();
                final int amount = lengthFrequencies.get(length, 0);
                lengthFrequencies.put(length, amount + 1);
            }

            final DefinedIntHuffmanTable lengthTable = DefinedIntHuffmanTable.withFrequencies(lengthFrequencies);
            final NatEncoder natEncoder = new NatEncoder(_obs);
            _obs.writeIntHuffmanTable(lengthTable, natEncoder, natEncoder);

            final ImmutableIntRange range = new ImmutableIntRange(0, accIdMap.size() - 1);
            final RangedIntSetEncoder encoder = new RangedIntSetEncoder(_obs, lengthTable, range);
            int maxBunchConcept = StreamedDatabaseConstants.minValidConcept + conceptIdMap.size() - bunches.size();
            int minBunchConcept = StreamedDatabaseConstants.minValidConcept;
            for (IntKeyMap.Entry<MutableIntSet> entry : bunches.entries()) {
                final RangedIntegerHuffmanTable bunchTable = new RangedIntegerHuffmanTable(minBunchConcept, maxBunchConcept);
                _obs.writeHuffmanSymbol(bunchTable, entry.key());
                minBunchConcept = entry.key() + 1;
                ++maxBunchConcept;

                _obs.writeIntSet(encoder, encoder, encoder, entry.value());
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

        final ImmutableIntSet.Builder acceptations = new ImmutableIntSetCreator();
        final ImmutableIntSet.Builder correlationArrays = new ImmutableIntSetCreator();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                final int accId = row.get(0).toInt();

                if (!ruledAcceptations.contains(accId)) {
                    acceptations.add(accId);
                    correlationArrays.add(row.get(1).toInt());
                }
            }
        }

        return new ExportableAcceptationsAndCorrelationArrays(acceptations.build(), correlationArrays.build());
    }

    private ImmutableIntSet listAgentCorrelations() {
        final LangbookDbSchema.AgentsTable table = LangbookDbSchema.Tables.agents;
        final DbQuery query = new DbQuery.Builder(table).select(table.getStartMatcherColumnIndex(),
                table.getStartAdderColumnIndex(), table.getEndMatcherColumnIndex(), table.getEndAdderColumnIndex());

        final ImmutableIntSet.Builder correlations = new ImmutableIntSetCreator();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                correlations.add(row.get(0).toInt());
                correlations.add(row.get(1).toInt());
                correlations.add(row.get(2).toInt());
                correlations.add(row.get(3).toInt());
            }
        }

        return correlations.build();
    }

    private ImmutableIntSet listExportableCorrelations(ImmutableIntSet correlationArrays) {
        final ImmutableIntSet.Builder correlations = new ImmutableIntSetCreator();
        for (int corrId : listAgentCorrelations()) {
            correlations.add(corrId);
        }

        final LangbookDbSchema.CorrelationArraysTable table = LangbookDbSchema.Tables.correlationArrays;
        final DbQuery query = new DbQuery.Builder(table).select(table.getArrayIdColumnIndex(), table.getCorrelationColumnIndex());

        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                if (correlationArrays.contains(row.get(0).toInt())) {
                    correlations.add(row.get(1).toInt());
                }
            }
        }

        return correlations.build();
    }

    private ImmutableIntSet listConversionSymbolArrays(MutableIntSet targetedAlphabets) {
        final LangbookDbSchema.ConversionsTable table = LangbookDbSchema.Tables.conversions;
        final DbQuery query = new DbQuery.Builder(table).select(table.getTargetAlphabetColumnIndex(), table.getSourceColumnIndex(), table.getTargetColumnIndex());

        final ImmutableIntSet.Builder symbolArrays = new ImmutableIntSetCreator();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                final List<DbValue> row = result.next();
                targetedAlphabets.add(row.get(0).toInt());
                symbolArrays.add(row.get(1).toInt());
                symbolArrays.add(row.get(2).toInt());
            }
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

        final ImmutableIntSet.Builder builder = new ImmutableIntSetCreator();
        try (DbResult result = _db.select(query)) {
            while (result.hasNext()) {
                builder.add(result.next().get(0).toInt());
            }
        }

        return builder.build();
    }

    private ImmutableIntSet listSymbolArraysFromSentences() {
        final LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getSymbolArrayColumnIndex());

        return _db.select(query).mapToInt(row -> row.get(0).toInt()).toSet().toImmutable();
    }

    private ExportableSymbolArraysResult listExportableSymbolArrays(ImmutableIntSet correlations) {
        final MutableIntSet targetedAlphabets = MutableIntArraySet.empty();
        final ImmutableIntSet.Builder symbolArrays = new ImmutableIntSetCreator();
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

        for (int arrayId : listSymbolArraysFromSentences()) {
            symbolArrays.add(arrayId);
        }

        return new ExportableSymbolArraysResult(symbolArrays.build(), targetedAlphabets.toImmutable());
    }

    private static class AgentAcceptationPair {
        final int agentWithRuleIndex;
        final int acceptation;

        AgentAcceptationPair(int agentWithRuleIndex, int acceptation) {
            this.agentWithRuleIndex = agentWithRuleIndex;
            this.acceptation = acceptation;
        }

        @Override
        public int hashCode() {
            return agentWithRuleIndex * 37 + acceptation;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof AgentAcceptationPair)) {
                return false;
            }

            AgentAcceptationPair that = (AgentAcceptationPair) other;
            return agentWithRuleIndex == that.agentWithRuleIndex && acceptation == that.acceptation;
        }

        static boolean lessThan(AgentAcceptationPair a, AgentAcceptationPair b) {
            return b != null && (a == null || a.agentWithRuleIndex < b.agentWithRuleIndex ||
                    a.agentWithRuleIndex == b.agentWithRuleIndex && a.acceptation < b.acceptation);
        }
    }

    private void writeRelevantRuledAcceptations(MutableIntPairMap accIdMap, ImmutableIntList agentsWithRule) throws IOException {
        final ImmutableIntSet originalAccKeys = accIdMap.keySet().toImmutable();
        if (!accIdMap.isEmpty() && accIdMap.max() + 1 != accIdMap.size()) {
            throw new AssertionError();
        }

        final LangbookDbSchema.SpanTable spans = LangbookDbSchema.Tables.spans;
        final LangbookDbSchema.RuledAcceptationsTable ruledAcceptations = LangbookDbSchema.Tables.ruledAcceptations;
        final int ruledAccOffset = spans.columns().size();
        final DbQuery query = new DbQuery.Builder(spans)
                .join(ruledAcceptations, spans.getDynamicAcceptationColumnIndex(), ruledAcceptations.getIdColumnIndex())
                .select(spans.getDynamicAcceptationColumnIndex(), ruledAccOffset + ruledAcceptations.getAcceptationColumnIndex(), ruledAccOffset + ruledAcceptations.getAgentColumnIndex());

        final MutableIntValueSortedMap<AgentAcceptationPair> pairs = MutableIntValueSortedMap.empty(AgentAcceptationPair::lessThan);
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int dynamicAcc = row.get(0).toInt();
                final int baseAcc = row.get(1).toInt();
                final int agentWithRuleIndex = agentsWithRule.indexOf(row.get(2).toInt());

                if (agentWithRuleIndex < 0) {
                    throw new AssertionError();
                }
                if (originalAccKeys.contains(dynamicAcc)) {
                    throw new AssertionError();
                }
                if (!originalAccKeys.contains(baseAcc)) {
                    throw new AssertionError();
                }

                pairs.put(new AgentAcceptationPair(agentWithRuleIndex, accIdMap.get(baseAcc)), dynamicAcc);
            }
        }

        final int pairsCount = pairs.size();
        _obs.writeHuffmanSymbol(naturalNumberTable, pairsCount);
        if (pairsCount > 0) {
            final int mainAccCount = originalAccKeys.size();
            final int maxAgentWithRule = agentsWithRule.size() - 1;
            int accIdMapSize = accIdMap.size();
            int previousAgentWithRuleIndex = 0;
            int firstPossibleAcc = 0;
            for (int pairIndex = 0; pairIndex < pairsCount; pairIndex++) {
                final AgentAcceptationPair pair = pairs.keyAt(pairIndex);
                final RangedIntegerHuffmanTable agentTable = new RangedIntegerHuffmanTable(previousAgentWithRuleIndex, maxAgentWithRule);
                _obs.writeHuffmanSymbol(agentTable, pair.agentWithRuleIndex);
                if (pair.agentWithRuleIndex != previousAgentWithRuleIndex) {
                    firstPossibleAcc = 0;
                }

                final RangedIntegerHuffmanTable mainAcceptationTable = new RangedIntegerHuffmanTable(firstPossibleAcc, mainAccCount - 1);
                _obs.writeHuffmanSymbol(mainAcceptationTable, pair.acceptation);

                previousAgentWithRuleIndex = pair.agentWithRuleIndex;
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

    private void writeSentenceSpans(IntPairMap accIdMap, SymbolArrayWriterResult symbolArrays) throws IOException {
        final LangbookDbSchema.SpanTable table = LangbookDbSchema.Tables.spans;
        final LangbookDbSchema.SentencesTable sentences = LangbookDbSchema.Tables.sentences;
        final DbQuery query = new DbQuery.Builder(table)
                .join(sentences, table.getSentenceIdColumnIndex(), sentences.getIdColumnIndex())
                .select(
                        table.columns().size() + sentences.getSymbolArrayColumnIndex(),
                        table.getStartColumnIndex(),
                        table.getLengthColumnIndex(),
                        table.getDynamicAcceptationColumnIndex());

        final ImmutableSet.Builder<SentenceSpan> builder = new ImmutableHashSet.Builder<>();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int sentenceId = row.get(0).toInt();
                final int start = row.get(1).toInt();
                final int length = row.get(2).toInt();
                final int acc = row.get(3).toInt();
                builder.add(new SentenceSpan(sentenceId, start, length, acc));
            }
        }

        final int symbolArrayCount = symbolArrays.idMap.size();
        final ImmutableSet<SentenceSpan> spans = builder.build().sort(SentenceSpan::lessThan);
        _obs.writeHuffmanSymbol(naturalNumberTable, spans.size());
        if (!spans.isEmpty()) {
            final RangedIntegerHuffmanTable accTable = new RangedIntegerHuffmanTable(0, accIdMap.size() - 1);
            int previousSymbolArrayFileId = 0;
            int previousStart = 0;

            for (SentenceSpan span : spans) {
                final int symbolArray = span.symbolArray;
                final int symbolArrayFileId = symbolArrays.idMap.get(symbolArray);
                final RangedIntegerHuffmanTable symbolArrayTable = new RangedIntegerHuffmanTable(previousSymbolArrayFileId, symbolArrayCount - 1);
                _obs.writeHuffmanSymbol(symbolArrayTable, symbolArrayFileId);
                if (symbolArrayFileId != previousSymbolArrayFileId) {
                    previousStart = 0;
                }

                final int sentenceLength = symbolArrays.lengths.get(symbolArray);
                final RangedIntegerHuffmanTable startTable = new RangedIntegerHuffmanTable(previousStart, sentenceLength - 1);
                _obs.writeHuffmanSymbol(startTable, span.start);

                final RangedIntegerHuffmanTable lengthTable = new RangedIntegerHuffmanTable(1, sentenceLength - span.start);
                _obs.writeHuffmanSymbol(lengthTable, span.length);

                _obs.writeHuffmanSymbol(accTable, accIdMap.get(span.acceptation));
                previousSymbolArrayFileId = symbolArrayFileId;
                previousStart = span.start;
            }
        }
    }

    private static boolean sentenceSetLessThan(ImmutableIntSet a, ImmutableIntSet b) {
        if (b == null) {
            return false;
        }

        if (a == null) {
            return true;
        }

        final int aLength = a.size();
        final int bLength = b.size();

        for (int index = 0; index < aLength && index < bLength; index++) {
            final int aValue = a.valueAt(index);
            final int bValue = b.valueAt(index);
            if (aValue < bValue) {
                return true;
            }

            if (bValue < aValue) {
                return false;
            }
        }

        return aLength < bLength;
    }

    private void writeSentenceMeanings(IntPairMap symbolArrayIdMap) throws IOException {
        final LangbookDbSchema.SentencesTable table = LangbookDbSchema.Tables.sentences;
        final DbQuery query = new DbQuery.Builder(table)
                .select(table.getSymbolArrayColumnIndex(), table.getConceptColumnIndex());

        final MutableIntKeyMap<ImmutableIntSet> map = MutableIntKeyMap.empty();
        final ImmutableIntSet emptySet = new ImmutableIntSetCreator().build();
        try (DbResult dbResult = _db.select(query)) {
            while (dbResult.hasNext()) {
                final List<DbValue> row = dbResult.next();
                final int symbolArray = row.get(0).toInt();
                final int concept = row.get(1).toInt();

                final ImmutableIntSet current = map.get(concept, emptySet);
                map.put(concept, current.add(symbolArrayIdMap.get(symbolArray)));
            }
        }

        final MutableSet<ImmutableIntSet> meaningBuilder = MutableSortedSet.empty(StreamedDatabaseWriter::sentenceSetLessThan);
        for (ImmutableIntSet set : map.toImmutable().filter(set -> set.size() >= 2)) {
            meaningBuilder.add(set);
        }

        final ImmutableSet<ImmutableIntSet> meanings = meaningBuilder.toImmutable();
        final int meaningCount = meanings.size();
        _obs.writeHuffmanSymbol(naturalNumberTable, meaningCount);
        if (meaningCount > 0) {
            final IntEncoder intEncoder = new IntEncoder(_obs);
            DefinedIntHuffmanTable lengthTable = DefinedIntHuffmanTable.from(meanings.mapToInt(ImmutableIntSet::size));
            _obs.writeIntHuffmanTable(lengthTable, intEncoder, intEncoder);

            int previousMin = 0;
            for (ImmutableIntSet set : meanings) {
                final ImmutableIntRange range = new ImmutableIntRange(previousMin, symbolArrayIdMap.size() - 1);
                final RangedIntSetEncoder encoder = new RangedIntSetEncoder(_obs, lengthTable, range);
                _obs.writeIntSet(encoder, encoder, encoder, set);
                previousMin = set.min();
            }
        }
    }

    public void write() throws IOException {
        setProgress(0.0f, "Filtering database");
        final ExportableAcceptationsAndCorrelationArrays exportable = listExportableAcceptationsAndCorrelationArrays();
        final ImmutableIntSet exportableCorrelations = listExportableCorrelations(exportable.correlationArrays);
        final ExportableSymbolArraysResult exportableSymbolArrays = listExportableSymbolArrays(exportableCorrelations);

        setProgress(0.1f, "Writing symbol arrays");
        final SymbolArrayWriterResult symbolArrayWriterResult = writeSymbolArrays(exportableSymbolArrays.symbolArrays);
        final ImmutableIntPairMap symbolArrayIdMap = symbolArrayWriterResult.idMap;
        final LanguageAndAlphabetsMapping langAlphabetMappings = writeLanguages();
        if (symbolArrayIdMap.isEmpty()) {
            // Without symbolArrays, it is possible to have languages and alphabets as they do not depend on them,
            // but not conversions, correlations, acceptations, sentences...
            //
            // The only thing that can exist without symbol arrays are concepts (languages and alphabets are concepts),
            // then all semantics can be present, even agents without matchers and adders.
            // However, even if possible, the app could not reference any of those without a single text to display to
            // the user. So, all that information is useless from app perspective, but not from database perspective.
            //
            // Because of that, for simplification, this writer will write an extra leading 0 to express that no valid
            // concepts are present. If later, those concepts are included, it will be possible without incompatibilities.

            final int validConcepts = 0;
            _obs.writeHuffmanSymbol(naturalNumberTable, validConcepts);
        }
        else {
            final ImmutableIntPairMap conceptIdMap = langAlphabetMappings.composeConceptMap(getUsedConcepts(_db));

            setProgress(0.2f, "Writing conversions");
            writeConversions(langAlphabetMappings.alphabets, symbolArrayIdMap);

            if (conceptIdMap.keySet().min() <= 0 || conceptIdMap.min() < StreamedDatabaseConstants.minValidConcept ||
                    conceptIdMap.max() > StreamedDatabaseConstants.minValidConcept + conceptIdMap.size() - 1) {
                throw new AssertionError();
            }

            _obs.writeHuffmanSymbol(naturalNumberTable, conceptIdMap.size());

            setProgress(0.25f, "Writing correlations");
            final ImmutableIntPairMap correlationIdMap = writeCorrelations(exportableCorrelations, langAlphabetMappings.alphabets, exportableSymbolArrays.excludedAlphabets, symbolArrayIdMap);

            setProgress(0.35f, "Writing correlation arrays");
            final ImmutableIntPairMap correlationArrayIdMap = writeCorrelationArrays(exportable.correlationArrays, correlationIdMap);

            // TODO: Check if this is already required and accRanges.concepts cannot be used instead
            setProgress(0.5f, "Writing acceptations");
            final ImmutableIntPairMap accIdMap = writeAcceptations(exportable.acceptations, conceptIdMap, correlationArrayIdMap);

            setProgress(0.6f, "Writing complemented concepts");
            writeComplementedConcepts(conceptIdMap);

            setProgress(0.7f, "Writing bunch acceptations");
            writeBunchAcceptations(conceptIdMap, accIdMap);

            setProgress(0.8f, "Writing agents");
            final ImmutableIntList agentsWithRule = writeAgents(conceptIdMap, correlationIdMap);

            setProgress(0.9f, "Writing dynamic acceptations");
            final MutableIntPairMap extendedAccIdMap = accIdMap.mutate();
            writeRelevantRuledAcceptations(extendedAccIdMap, agentsWithRule);

            setProgress(0.93f, "Writing sentence spans");
            writeSentenceSpans(extendedAccIdMap, symbolArrayWriterResult);

            setProgress(0.98f, "Writing sentence meanings");
            writeSentenceMeanings(symbolArrayIdMap);
        }

        _obs.close();
    }
}
