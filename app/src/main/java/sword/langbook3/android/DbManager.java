package sword.langbook3.android;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import sword.collections.AbstractTransformer;
import sword.collections.Function;
import sword.collections.ImmutableIntKeyMap;
import sword.collections.ImmutableIntSet;
import sword.collections.ImmutableList;
import sword.collections.IntKeyMap;
import sword.collections.List;
import sword.collections.MutableList;
import sword.collections.Traversable;
import sword.database.DbColumn;
import sword.database.DbDeleteQuery;
import sword.database.DbExporter;
import sword.database.DbImporter;
import sword.database.DbIndex;
import sword.database.DbInsertQuery;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbSchema;
import sword.database.DbTable;
import sword.database.DbUpdateQuery;
import sword.database.DbValue;
import sword.langbook3.android.db.AlphabetId;
import sword.langbook3.android.db.AlphabetIdManager;
import sword.langbook3.android.db.LangbookDatabaseManager;
import sword.langbook3.android.db.LangbookDbSchema;
import sword.langbook3.android.db.LangbookManager;
import sword.langbook3.android.db.LanguageId;
import sword.langbook3.android.db.LanguageIdManager;
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
    private ManagerDatabase _managerDatabase;
    private LangbookDatabaseManager<LanguageId, AlphabetId> _langbookManager;

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

    private static boolean delete(SQLiteDatabase db, DbDeleteQuery query) {
        final ImmutableIntKeyMap<DbValue> constraints = query.constraints();
        final ImmutableList<DbColumn> columns = query.table().columns();
        final String whereClause = constraints.keySet()
                .map(key -> columns.get(key).name() + "=?")
                .reduce((a, b) -> a + " AND " + b);

        final int constraintsSize = constraints.size();
        final String[] values = new String[constraintsSize];
        for (int i = 0; i < constraintsSize; i++) {
            final DbValue value = constraints.valueAt(i);
            values[i] = value.isText()? value.toText() : Integer.toString(value.toInt());
        }

        return db.delete(query.table().name(), whereClause, values) > 0;
    }

    private static boolean update(SQLiteDatabase db, DbUpdateQuery query) {
        final ImmutableIntKeyMap<DbValue> constraints = query.constraints();
        final ImmutableList<DbColumn> columns = query.table().columns();
        final String whereClause = constraints.keySet()
                .map(key -> columns.get(key).name() + "=?")
                .reduce((a, b) -> a + " AND " + b);
        final int constraintsSize = constraints.size();
        final String[] values = new String[constraintsSize];
        for (int i = 0; i < constraintsSize; i++) {
            final DbValue value = constraints.valueAt(i);
            values[i] = value.isText()? value.toText() : Integer.toString(value.toInt());
        }

        final ContentValues cv = new ContentValues();
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

    private static void createTables(SQLiteDatabase db, DbSchema schema) {
        for (DbTable table : schema.tables()) {
            StringBuilder builder = new StringBuilder();
            builder.append("CREATE TABLE ")
                    .append(table.name())
                    .append(" (");

            final Function<DbColumn, String> mapFunc = column -> column.name() + ' ' + sqlType(column);
            final String columns = table.columns().map(mapFunc)
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

    final class DbAttachedQuery implements Traversable<List<DbValue>> {

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
    public ImmutableList<DbValue> selectSingleRow(DbQuery query) {
        SQLiteDbResult result = select(getReadableDatabase(), query);
        if (!result.hasNext()) {
            throw new AssertionError();
        }

        final ImmutableList<DbValue> row = result.next().toImmutable();
        if (result.hasNext()) {
            throw new AssertionError();
        }
        return row;
    }

    private static final class SQLiteDbIntValue implements DbValue {

        int _value;

        @Override
        public boolean isText() {
            return false;
        }

        @Override
        public int toInt() throws UnsupportedOperationException {
            return _value;
        }

        @Override
        public String toText() {
            return Integer.toString(_value);
        }
    }

    private static final class SQLiteDbTextValue implements DbValue {

        String _value;

        @Override
        public boolean isText() {
            return true;
        }

        @Override
        public int toInt() throws UnsupportedOperationException {
            throw new UnsupportedOperationException("String column should not be converted to integer");
        }

        @Override
        public String toText() {
            return _value;
        }
    }

    static class SQLiteDbResult extends AbstractTransformer<List<DbValue>> implements DbResult {
        private final ImmutableList<DbColumn> _columns;
        private final Cursor _cursor;
        private final int _rowCount;
        private int _nextRowIndex;

        private final ImmutableIntSet _columnIndexes;
        private MutableList<DbValue> _rowHolder;

        SQLiteDbResult(ImmutableList<DbColumn> columns, Cursor cursor) {
            _columns = columns;
            _columnIndexes = columns.indexes();
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

        private DbValue cursorToDbValue(int index, DbValue recyclable) {
            if (_columns.get(index).isText()) {
                final SQLiteDbTextValue holder = (recyclable instanceof SQLiteDbTextValue)?
                    (SQLiteDbTextValue) recyclable : new SQLiteDbTextValue();
                holder._value = _cursor.getString(index);
                return holder;
            }
            else {
                final SQLiteDbIntValue holder = (recyclable instanceof SQLiteDbIntValue)?
                        (SQLiteDbIntValue) recyclable : new SQLiteDbIntValue();
                holder._value = _cursor.getInt(index);
                return holder;
            }
        }

        @Override
        public List<DbValue> next() {
            if (!hasNext()) {
                throw new UnsupportedOperationException("End already reached");
            }

            final int columnCount = _columnIndexes.size();
            if (_rowHolder == null) {
                _rowHolder = MutableList.empty((currentLength, newSize) -> columnCount);
                for (int i = 0; i < columnCount; i++) {
                    _rowHolder.append(cursorToDbValue(i, null));
                }
            }
            else {
                for (int i = 0; i < columnCount; i++) {
                    cursorToDbValue(i, _rowHolder.get(i));
                }
            }

            if (!_cursor.moveToNext()) {
                close();
            }
            _nextRowIndex++;

            return _rowHolder;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final LangbookDbSchema schema = LangbookDbSchema.getInstance();
        createTables(db, schema);
        createIndexes(db, schema);

        if (_uri != null) {
            final Uri uri = _uri;
            _uri = null;

            importDatabase(db, uri);
        }
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
        DbImporter.Database initDb = new InitializerDatabase(db);
        final DatabaseImporter reader = new DatabaseImporter(_context, uri, _progressListener);
        try {
            reader.init(initDb);
        }
        catch (DbImporter.UnableToImportException e) {
            e.printStackTrace();
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
                Toast.makeText(_context, R.string.saveDatabaseError, Toast.LENGTH_SHORT).show();
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

        private int _dbWriteVersion = 1;

        @Override
        public DbResult select(DbQuery query) {
            return DbManager.select(getReadableDatabase(), query);
        }

        @Override
        public Integer insert(DbInsertQuery query) {
            ++_dbWriteVersion;
            return DbManager.insert(getWritableDatabase(), query);
        }

        @Override
        public boolean delete(DbDeleteQuery query) {
            ++_dbWriteVersion;
            return DbManager.delete(getWritableDatabase(), query);
        }

        @Override
        public boolean update(DbUpdateQuery query) {
            ++_dbWriteVersion;
            return DbManager.update(getWritableDatabase(), query);
        }

        @Override
        public int getWriteVersion() {
            return _dbWriteVersion;
        }
    }

    public Database getDatabase() {
        if (_managerDatabase == null) {
            _managerDatabase = new ManagerDatabase();
        }
        return _managerDatabase;
    }

    public LangbookManager<LanguageId, AlphabetId> getManager() {
        if (_langbookManager == null) {
            _langbookManager = new LangbookDatabaseManager<>(getDatabase(), new LanguageIdManager(), new AlphabetIdManager());
        }

        return _langbookManager;
    }

    public interface Database extends sword.database.Database {
        /**
         * Returns an integer that its incremented every time that an insert, update or delete
         * operation is performed in this database.
         *
         * The idea behind this method is to call this method just after updating the UI,
         * and keep its value in a field within that class. This method can be called later again.
         * If the value is still the same as it was before, that no changes were performed into
         * the database, and then the data in the UI should still be valid. But if the value is
         * different, the data in that screen may had changed, and then a refresh may be required.
         *
         * Do not save the returned within the Android task (bundles or intents), as this class
         * can disappear if the process is finished.
         *
         * @return A numeric value reflecting the current version of this database.
         */
        int getWriteVersion();
    }
}
