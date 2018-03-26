package sword.langbook3.android;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sword.langbook3.android.LangbookDbSchema.QuestionFieldFlags;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldSets;
import sword.langbook3.android.LangbookDbSchema.QuizDefinitionsTable;
import sword.langbook3.android.LangbookDbSchema.Tables;
import sword.langbook3.android.db.DbColumn;
import sword.langbook3.android.db.DbInitializer;
import sword.langbook3.android.db.DbInsertQuery;
import sword.langbook3.android.db.DbIntValue;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbSchema;
import sword.langbook3.android.db.DbStringValue;
import sword.langbook3.android.db.DbTable;
import sword.langbook3.android.db.DbValue;
import sword.langbook3.android.sdb.ProgressListener;

class DbManager extends SQLiteOpenHelper {

    private static final String DB_NAME = "Langbook";
    private static final int DB_VERSION = 5;

    private static DbManager _instance;

    private final Context _context;
    private Uri _uri;
    private ProgressListener _progressListener;
    private DatabaseImportTask _databaseImportTask;
    private ProgressListener _externalProgressListener;
    private DatabaseImportProgress _lastProgress;

    static DbManager getInstance() {
        return _instance;
    }

    static void createInstance(Context context) {
        _instance = new DbManager(context);
    }

    private DbManager(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        _context = context;
    }

    private static SQLiteDbResult select(SQLiteDatabase db, DbQuery query) {
        return new SQLiteDbResult(query.getSelectedColumns(), db.rawQuery(new SQLiteDbQuery(query).toSql(), null));
    }

    private static Integer insert(SQLiteDatabase db, DbInsertQuery query) {
        final int count = query.getColumnCount();
        ContentValues cv = new ContentValues();
        for (int i = 0; i < count; i++) {
            final String name = query.getColumn(i).name();
            final DbValue value = query.getValue(i);
            if (value.isText()) {
                cv.put(name, value.toText());
            }
            else {
                cv.put(name, value.toInt());
            }
        }

        final long returnId = db.insert(query.getTable().name(), null, cv);
        return (returnId >= 0)? (int) returnId : null;
    }

    private static int getColumnMax(SQLiteDatabase db, DbTable table, int columnIndex) {
        final DbQuery query = new DbQuery.Builder(table)
                .select(DbQuery.max(columnIndex));
        final SQLiteDbResult result = select(db, query);
        try {
            return result.next().get(0).toInt();
        }
        finally {
            result.close();
        }
    }

    static final class QuestionField {

        final int alphabet;
        final int rule;
        final int flags;

        QuestionField(int alphabet, int rule, int flags) {
            this.alphabet = alphabet;
            this.rule = rule;
            this.flags = flags;
        }

        int getType() {
            return flags & QuestionFieldFlags.TYPE_MASK;
        }

        int getTypeStringResId() {
            switch (getType()) {
                case QuestionFieldFlags.TYPE_SAME_ACC:
                    return R.string.questionTypeSameAcceptation;

                case QuestionFieldFlags.TYPE_SAME_CONCEPT:
                    return R.string.questionTypeSameConcept;

                case QuestionFieldFlags.TYPE_APPLY_RULE:
                    return R.string.questionTypeAppliedRule;
            }

            return 0;
        }

        boolean isAnswer() {
            return (flags & QuestionFieldFlags.IS_ANSWER) != 0;
        }

        @Override
        public int hashCode() {
            return (alphabet * 31 + rule) * 31 + flags;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof QuestionField)) {
                return false;
            }

            QuestionField that = (QuestionField) other;
            return alphabet == that.alphabet && rule == that.rule && flags == that.flags;
        }
    }

    static Integer findQuestionFieldSet(SQLiteDatabase db, Collection<QuestionField> collection) {
        if (collection == null || collection.size() == 0) {
            return null;
        }

        final Set<QuestionField> set = new HashSet<>(collection);

        final QuestionField firstField = set.iterator().next();
        final QuestionFieldSets fieldSets = Tables.questionFieldSets;
        final int columnCount = fieldSets.columns().size();
        final DbQuery query = new DbQuery.Builder(fieldSets)
                .join(fieldSets, fieldSets.getSetIdColumnIndex(), fieldSets.getSetIdColumnIndex())
                .where(fieldSets.getAlphabetColumnIndex(), firstField.alphabet)
                .where(fieldSets.getRuleColumnIndex(), firstField.rule)
                .where(fieldSets.getFlagsColumnIndex(), firstField.flags)
                .select(
                        fieldSets.getSetIdColumnIndex(),
                        columnCount + fieldSets.getAlphabetColumnIndex(),
                        columnCount + fieldSets.getRuleColumnIndex(),
                        columnCount + fieldSets.getFlagsColumnIndex());

        final DbResult result = select(db, query);

        try {
            if (result.hasNext()) {
                DbResult.Row row = result.next();
                int setId = row.get(0).toInt();
                final Set<QuestionField> foundSet = new HashSet<>();
                foundSet.add(new QuestionField(row.get(1).toInt(), row.get(2).toInt(), row.get(3).toInt()));

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

                    foundSet.add(new QuestionField(row.get(1).toInt(), row.get(2).toInt(), row.get(3).toInt()));
                }

                if (foundSet.equals(set)) {
                    return setId;
                }
            }
        }
        finally {
            result.close();
        }

        return null;
    }

    private static int getMaxQuestionFieldSetId(SQLiteDatabase db) {
        QuestionFieldSets table = Tables.questionFieldSets;
        return getColumnMax(db, table, table.getSetIdColumnIndex());
    }

    static int insertQuestionFieldSet(SQLiteDatabase db, List<QuestionField> fields) {
        final QuestionFieldSets table = Tables.questionFieldSets;
        ContentValues fieldsCv = new ContentValues();
        final int setId = getMaxQuestionFieldSetId(db) + 1;

        for (QuestionField field : fields) {
            fieldsCv.put(table.getColumnName(table.getSetIdColumnIndex()), setId);
            fieldsCv.put(table.getColumnName(table.getAlphabetColumnIndex()), field.alphabet);
            fieldsCv.put(table.getColumnName(table.getRuleColumnIndex()), field.rule);
            fieldsCv.put(table.getColumnName(table.getFlagsColumnIndex()), field.flags);
            db.insert(table.name(), null, fieldsCv);
        }

        return setId;
    }

    static Integer findQuizDefinition(SQLiteDatabase db, int bunch, int setId) {
        final QuizDefinitionsTable table = Tables.quizDefinitions;
        final DbResult result = getInstance().attach(new DbQuery.Builder(table)
                .where(table.getBunchColumnIndex(), bunch)
                .where(table.getQuestionFieldsColumnIndex(), setId)
                .select(table.getIdColumnIndex())).iterator();
        final Integer value = result.hasNext()? result.next().get(0).toInt() : null;
        if (result.hasNext()) {
            throw new AssertionError("Only one quiz definition expected");
        }

        return value;
    }

    static int insertQuizDefinition(SQLiteDatabase db, int bunch, int setId) {
        final QuizDefinitionsTable table = Tables.quizDefinitions;
        ContentValues cv = new ContentValues();
        cv.put(table.getColumnName(table.getBunchColumnIndex()), bunch);
        cv.put(table.getColumnName(table.getQuestionFieldsColumnIndex()), setId);
        final long returnId = db.insert(table.name(), null, cv);
        if (returnId < 0) {
            throw new AssertionError("insert returned a negative id");
        }
        return (int) returnId;
    }

    private static void createTables(SQLiteDatabase db, DbSchema schema) {
        for (DbTable table : schema.tables()) {
            StringBuilder builder = new StringBuilder();
            builder.append("CREATE TABLE ")
                    .append(table.name())
                    .append(" (");

            final int columnCount = table.columns().size();
            for (int j = 0; j < columnCount; j++) {
                final String columnName = table.getColumnName(j);
                if (j != 0) {
                    builder.append(", ");
                }

                builder.append(columnName).append(' ')
                        .append(table.getColumnType(j));
            }
            builder.append(')');

            db.execSQL(builder.toString());
        }
    }

    private static void createIndexes(SQLiteDatabase db, DbSchema schema) {
        int i = 0;
        for (DbSchema.DbIndex index : schema.indexes()) {
            db.execSQL("CREATE INDEX I" + (i++) + " ON " + index.table.name() + " (" + index.table.getColumnName(index.column) + ')');
        }
    }

    final class DbAttachedQuery implements Iterable<DbResult.Row> {

        private final DbQuery _query;

        private DbAttachedQuery(DbQuery query) {
            _query = query;
        }

        @Override
        public SQLiteDbResult iterator() {
            return select(getReadableDatabase(), _query);
        }
    }

    /**
     * Attach the given query to this manager instance.
     * This targets a databses where to execute the query.
     */
    DbAttachedQuery attach(DbQuery query) {
        return new DbAttachedQuery(query);
    }

    static class SQLiteDbResult implements DbResult {
        private final DbColumn[] _columns;
        private final Cursor _cursor;
        private final int _rowCount;
        private int _nextRowIndex;

        SQLiteDbResult(DbColumn[] columns, Cursor cursor) {
            _columns = columns;
            _cursor = cursor;
            _rowCount = cursor.getCount();

            if (!cursor.moveToFirst()) {
                close();
            }
        }

        @Override
        public void close() {
            _nextRowIndex = _rowCount;
            _cursor.close();
        }

        @Override
        public int getRemainingRows() {
            return _rowCount - _nextRowIndex;
        }

        @Override
        public boolean hasNext() {
            return getRemainingRows() > 0;
        }

        @Override
        public Row next() {
            if (!hasNext()) {
                throw new UnsupportedOperationException("End already reached");
            }

            final DbValue[] values = new DbValue[_columns.length];
            for (int i = 0; i < _columns.length; i++) {
                values[i] = _columns[i].isText()? new DbStringValue(_cursor.getString(i)) : new DbIntValue(_cursor.getInt(i));
            }
            Row row = new Row(values);

            if (!_cursor.moveToNext()) {
                close();
            }
            _nextRowIndex++;

            return row;
        }

        static class Row implements DbResult.Row {

            private final DbValue[] _values;

            Row(DbValue[] values) {
                _values = values;
            }

            @Override
            public DbValue get(int index) {
                return _values[index];
            }
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final Uri uri = (_uri != null)? _uri : Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + _context.getPackageName() + '/' + R.raw.basic);

        _uri = null;

        importDatabase(db, uri);
    }

    private static final class InitializerDatabase implements DbInitializer.Database {

        private final SQLiteDatabase _db;

        InitializerDatabase(SQLiteDatabase db) {
            _db = db;
        }

        @Override
        public Integer insert(DbInsertQuery query) {
            return DbManager.insert(_db, query);
        }

        @Override
        public DbResult select(DbQuery query) {
            return DbManager.select(_db, query);
        }
    }

    private void importDatabase(SQLiteDatabase db, Uri uri) {
        final LangbookDbSchema schema = LangbookDbSchema.getInstance();
        createTables(db, schema);
        createIndexes(db, schema);

        DbInitializer.Database initDb = new InitializerDatabase(db);
        final DatabaseImporter reader = new DatabaseImporter(_context, uri, _progressListener);
        try {
            reader.init(initDb);
        }
        catch (DbInitializer.UnableToInitializeException e) {
            Toast.makeText(_context, "Error loading database", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // So far, version 5 is the only one expected. So this method should never be called
    }

    private static class DatabaseImportProgress {

        final float progress;
        final String message;

        DatabaseImportProgress(float progress, String message) {
            this.progress = progress;
            this.message = message;
        }
    }

    private class DatabaseImportTask extends AsyncTask<Uri, DatabaseImportProgress, Boolean> implements ProgressListener {

        @Override
        protected Boolean doInBackground(Uri... uris) {
            getWritableDatabase().close();
            _context.deleteDatabase(DB_NAME);
            _uri = uris[0];
            getWritableDatabase().close();
            return true;
        }

        @Override
        public void setProgress(float progress, String message) {
            publishProgress(new DatabaseImportProgress(progress, message));
        }

        @Override
        public void onProgressUpdate(DatabaseImportProgress... progresses) {
            final DatabaseImportProgress progress = progresses[0];
            final ProgressListener listener = _externalProgressListener;
            if (listener != null) {
                listener.setProgress(progress.progress, progress.message);
            }

            _lastProgress = progress;
        }

        @Override
        public void onPostExecute(Boolean result) {
            _progressListener = null;
            _databaseImportTask = null;

            final ProgressListener listener = _externalProgressListener;
            final DatabaseImportProgress lastProgress = new DatabaseImportProgress(1f, "Completed");
            if (listener != null) {
                listener.setProgress(lastProgress.progress, lastProgress.message);
            }
            _lastProgress = lastProgress;
        }
    }

    void importDatabase(Uri uri) {
        _databaseImportTask = new DatabaseImportTask();
        _progressListener = _databaseImportTask;
        _databaseImportTask.execute(uri);
    }

    boolean isImportingDatabase() {
        return _databaseImportTask != null;
    }

    void setProgressListener(ProgressListener listener) {
        _externalProgressListener = listener;
        final DatabaseImportProgress lastProgress = _lastProgress;
        if (listener != null && lastProgress != null) {
            listener.setProgress(lastProgress.progress, lastProgress.message);
        }
    }

    String getDatabasePath() {
        return _context.getDatabasePath(DB_NAME).toString();
    }
}
