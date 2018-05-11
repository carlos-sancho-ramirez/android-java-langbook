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

import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableList;
import sword.collections.IntKeyMap;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldFlags;
import sword.langbook3.android.LangbookDbSchema.QuestionFieldSets;
import sword.langbook3.android.LangbookDbSchema.QuizDefinitionsTable;
import sword.langbook3.android.LangbookDbSchema.Tables;
import sword.langbook3.android.db.Database;
import sword.langbook3.android.db.DbColumn;
import sword.langbook3.android.db.DbExporter;
import sword.langbook3.android.db.DbImporter;
import sword.langbook3.android.db.DbIndex;
import sword.langbook3.android.db.DbInsertQuery;
import sword.langbook3.android.db.DbIntValue;
import sword.langbook3.android.db.DbQuery;
import sword.langbook3.android.db.DbResult;
import sword.langbook3.android.db.DbSchema;
import sword.langbook3.android.db.DbStringValue;
import sword.langbook3.android.db.DbTable;
import sword.langbook3.android.db.DbUpdateQuery;
import sword.langbook3.android.db.DbValue;
import sword.langbook3.android.sdb.ProgressListener;
import sword.langbook3.android.sqlite.SQLiteDbQuery;

import static sword.langbook3.android.sqlite.SqliteUtils.sqlType;

class DbManager extends SQLiteOpenHelper {

    private static final String DB_NAME = "Langbook";
    private static final int DB_VERSION = 5;

    private static DbManager _instance;

    private final Context _context;
    private Uri _uri;
    private ProgressListener _progressListener;
    private DatabaseImportTask _databaseImportTask;
    private DatabaseExportTask _databaseExportTask;
    private ProgressListener _externalProgressListener;
    private TaskProgress _lastProgress;

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
        return new SQLiteDbResult(query.columns(), db.rawQuery(new SQLiteDbQuery(query).toSql(), null));
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

    public Integer insert(DbInsertQuery query) {
        return insert(getWritableDatabase(), query);
    }

    private static boolean delete(SQLiteDatabase db, DbTable table, int id) {
        return db.delete(table.name(), table.columns().get(table.getIdColumnIndex()).name() + "=?", new String[] { Integer.toString(id) }) > 0;
    }

    private static boolean update(SQLiteDatabase db, DbUpdateQuery query) {
        final ImmutableIntKeyMap<DbValue> constraints = query.constraints();
        final ImmutableList.Builder<String> whereList = new ImmutableList.Builder<>(constraints.size());
        final String[] values = new String[constraints.size()];
        for (IntKeyMap.Entry<DbValue> entry : constraints.entries()) {
            whereList.add(query.table().columns().get(entry.key()).name() + "=?");

            final DbValue value = entry.value();
            values[entry.index()] = value.isText()? value.toText() : Integer.toString(value.toInt());
        }

        final String whereClause = whereList.build().reduce((a,b) -> a + " AND " + b);
        ContentValues cv = new ContentValues();
        for (IntKeyMap.Entry<DbValue> entry : query.values().entries()) {
            final String columnName = query.table().columns().get(entry.key()).name();
            final DbValue value = entry.value();
            if (value.isText()) {
                cv.put(columnName, value.toText());
            }
            else {
                cv.put(columnName, value.toInt());
            }
        }

        return db.update(query.table().name(), cv, whereClause, values) > 0;
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
            fieldsCv.put(table.columns().get(table.getSetIdColumnIndex()).name(), setId);
            fieldsCv.put(table.columns().get(table.getAlphabetColumnIndex()).name(), field.alphabet);
            fieldsCv.put(table.columns().get(table.getRuleColumnIndex()).name(), field.rule);
            fieldsCv.put(table.columns().get(table.getFlagsColumnIndex()).name(), field.flags);
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
        cv.put(table.columns().get(table.getBunchColumnIndex()).name(), bunch);
        cv.put(table.columns().get(table.getQuestionFieldsColumnIndex()).name(), setId);
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

            final String columns = table.columns()
                    .map(column -> column.name() + ' ' + sqlType(column))
                    .reduce((left, right) -> left + ", " + right);

            builder.append(columns).append(')');
            db.execSQL(builder.toString());
        }
    }

    private static void createIndexes(SQLiteDatabase db, DbSchema schema) {
        int i = 0;
        for (DbIndex index : schema.indexes()) {
            db.execSQL("CREATE INDEX I" + (i++) + " ON " + index.table.name() + " (" + index.table.columns().get(index.column).name() + ')');
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

    /**
     * Allow selecting a single row.
     *
     * This method is suitable for situations where a single row is expected.
     *
     * This method will ensure that one, and one one, row is returned and on
     * executing the query.
     *
     * @param query Query to be executed.
     * @return The result row on executing the query
     */
    public DbResult.Row selectSingleRow(DbQuery query) {
        DbResult result = attach(query).iterator();
        if (!result.hasNext()) {
            throw new AssertionError();
        }

        final DbResult.Row row = result.next();
        if (result.hasNext()) {
            throw new AssertionError();
        }
        return row;
    }

    static class SQLiteDbResult implements DbResult {
        private final ImmutableList<DbColumn> _columns;
        private final Cursor _cursor;
        private final int _rowCount;
        private int _nextRowIndex;

        SQLiteDbResult(ImmutableList<DbColumn> columns, Cursor cursor) {
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

            final int columnCount = _columns.size();
            final DbValue[] values = new DbValue[columnCount];
            for (int i = 0; i < columnCount; i++) {
                values[i] = _columns.get(i).isText()? new DbStringValue(_cursor.getString(i)) : new DbIntValue(_cursor.getInt(i));
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

    private static final class InitializerDatabase implements DbImporter.Database {

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

        DbImporter.Database initDb = new InitializerDatabase(db);
        final DatabaseImporter reader = new DatabaseImporter(_context, uri, _progressListener);
        try {
            reader.init(initDb);
        }
        catch (DbImporter.UnableToImportException e) {
            Toast.makeText(_context, "Error loading database", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // So far, version 5 is the only one expected. So this method should never be called
    }

    private static class TaskProgress {

        final float progress;
        final String message;

        TaskProgress(float progress, String message) {
            this.progress = progress;
            this.message = message;
        }
    }

    private class DatabaseImportTask extends AsyncTask<Uri, TaskProgress, Boolean> implements ProgressListener {

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
            publishProgress(new TaskProgress(progress, message));
        }

        @Override
        public void onProgressUpdate(TaskProgress... progresses) {
            final TaskProgress progress = progresses[0];
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
            final TaskProgress lastProgress = new TaskProgress(1f, "Completed");
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

    private class DatabaseExportTask extends AsyncTask<Uri, TaskProgress, Boolean> implements ProgressListener {

        @Override
        protected Boolean doInBackground(Uri... uris) {
            final DatabaseExporter writer = new DatabaseExporter(_context, uris[0], _progressListener);
            try {
                writer.save(new InitializerDatabase(getReadableDatabase()));
            }
            catch (DbExporter.UnableToExportException e) {
                return false;
            }

            return true;
        }

        @Override
        public void setProgress(float progress, String message) {
            publishProgress(new TaskProgress(progress, message));
        }

        @Override
        public void onProgressUpdate(TaskProgress... progresses) {
            final TaskProgress progress = progresses[0];
            final ProgressListener listener = _externalProgressListener;
            if (listener != null) {
                listener.setProgress(progress.progress, progress.message);
            }

            _lastProgress = progress;
        }

        @Override
        public void onPostExecute(Boolean result) {
            _progressListener = null;
            _databaseExportTask = null;

            final TaskProgress lastProgress = new TaskProgress(1f, "Completed");
            final ProgressListener listener = _externalProgressListener;
            if (listener != null) {
                listener.setProgress(lastProgress.progress, lastProgress.message);
            }
            _lastProgress = lastProgress;

            if (!result) {
                Toast.makeText(_context, "Error saving database", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void exportStreamedDatabase(Uri uri) {
        _databaseExportTask = new DatabaseExportTask();
        _progressListener = _databaseExportTask;
        _databaseExportTask.execute(uri);
    }

    boolean isProcessingDatabase() {
        return _databaseImportTask != null || _databaseExportTask != null;
    }

    void setProgressListener(ProgressListener listener) {
        _externalProgressListener = listener;
        final TaskProgress lastProgress = _lastProgress;
        if (listener != null && lastProgress != null) {
            listener.setProgress(lastProgress.progress, lastProgress.message);
        }
    }

    String getDatabasePath() {
        return _context.getDatabasePath(DB_NAME).toString();
    }

    private final class ManagerDatabase implements Database {

        @Override
        public DbResult select(DbQuery query) {
            return DbManager.select(getReadableDatabase(), query);
        }

        @Override
        public Integer insert(DbInsertQuery query) {
            return DbManager.insert(getWritableDatabase(), query);
        }

        @Override
        public boolean delete(DbTable table, int id) {
            return DbManager.delete(getWritableDatabase(), table, id);
        }

        @Override
        public boolean update(DbUpdateQuery query) {
            return DbManager.update(getWritableDatabase(), query);
        }
    }

    public Database getDatabase() {
        return new ManagerDatabase();
    }
}
