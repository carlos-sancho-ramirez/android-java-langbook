package sword.langbook3.android.sdb;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.HashMap;

import sword.bitstream.OutputBitStream;
import sword.bitstream.Procedure2WithIOException;
import sword.bitstream.ProcedureWithIOException;
import sword.bitstream.huffman.CharHuffmanTable;
import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;
import sword.collections.ImmutableIntPairMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableIntSetBuilder;
import sword.collections.ImmutableIntValueMap;
import sword.collections.ImmutableSet;
import sword.collections.IntPairMap;
import sword.collections.IntValueMap;
import sword.collections.MutableIntKeyMap;
import sword.collections.MutableIntPairMap;
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

        private final OutputBitStream _obs;

        IntWriter(OutputBitStream obs) {
            _obs = obs;
        }

        @Override
        public void apply(Integer value) throws IOException {
            _obs.writeHuffmanSymbol(naturalNumberTable, value);
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

    private static final class IntComparator implements Comparator<Integer> {

        @Override
        public int compare(Integer a, Integer b) {
            return a - b;
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
        _obs.writeHuffmanTable(symbolArraysLengthTable, new IntWriter(_obs), new IntHuffmanSymbolDiffWriter(_obs, nat3Table));

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

    private void writeLanguages(ImmutableIntPairMap symbolArraysIdMap, ImmutableIntValueMap<String> langMap) throws IOException {
        final LangbookDbSchema.AlphabetsTable alphabetsTable = LangbookDbSchema.Tables.alphabets;
        final DbResult alphabetResult = _db.select(new DbQuery.Builder(alphabetsTable).select(alphabetsTable.getIdColumnIndex(), alphabetsTable.getLanguageColumnIndex()));
        final MutableIntKeyMap<ImmutableIntSet> alphabetMap = MutableIntKeyMap.empty();
        final ImmutableIntSet emptySet = new ImmutableIntSetBuilder().build();
        try {
            while (alphabetResult.hasNext()) {
                final DbResult.Row row = alphabetResult.next();
                final int alphabetDbId = row.get(0).toInt();
                final int langDbId = row.get(1).toInt();
                final ImmutableIntSet set = alphabetMap.get(langDbId, emptySet);
                alphabetMap.put(langDbId, set.add(alphabetDbId));
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
    }

    public void write() throws IOException {
        final ImmutableIntValueMap<String> langCodes = readLanguageCodes();
        final SymbolArrayWriterResult symbolArrayWriterResult = writeSymbolArrays(langCodes);
        writeLanguages(symbolArrayWriterResult.idMap, symbolArrayWriterResult.langMap);
        _obs.close();
    }
}
