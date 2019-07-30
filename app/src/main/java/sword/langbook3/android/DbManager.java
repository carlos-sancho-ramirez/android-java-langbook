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
import sword.collections.IntFunction;
import sword.collections.IntKeyMap;
import sword.collections.List;
import sword.collections.Traversable;
import sword.database.Database;
import sword.database.DbColumn;
import sword.database.DbDeleteQuery;
import sword.database.DbExporter;
import sword.database.DbImporter;
import sword.database.DbIndex;
import sword.database.DbInsertQuery;
import sword.database.DbIntValue;
import sword.database.DbQuery;
import sword.database.DbResult;
import sword.database.DbSchema;
import sword.database.DbStringValue;
import sword.database.DbTable;
import sword.database.DbUpdateQuery;
import sword.database.DbValue;
import sword.langbook3.android.db.LangbookDbSchema;
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

    private static boolean delete(SQLiteDatabase db, DbDeleteQuery query) {
        final ImmutableIntKeyMap<DbValue> constraints = query.constraints();
        final ImmutableList.Builder<String> whereList = new ImmutableList.Builder<>(constraints.size());
        final String[] values = new String[constraints.size()];
        final ImmutableList<DbColumn> columns = query.table().columns();
        for (IntKeyMap.Entry<DbValue> entry : constraints.entries()) {
            whereList.add(columns.get(entry.key()).name() + "=?");

            final DbValue value = entry.value();
            values[entry.index()] = value.isText()? value.toText() : Integer.toString(value.toInt());
        }

        final String whereClause = whereList.build().reduce((a,b) -> a + " AND " + b);
        return db.delete(query.table().name(), whereClause, values) > 0;
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

        final ImmutableList<DbValue> row = result.next();
        if (result.hasNext()) {
            throw new AssertionError();
        }
        return row;
    }

    static class SQLiteDbResult extends AbstractTransformer<List<DbValue>> implements DbResult {
        private final ImmutableList<DbColumn> _columns;
        private final Cursor _cursor;
        private final int _rowCount;
        private int _nextRowIndex;

        private ImmutableIntSet _columnIndexes;
        private IntFunction<DbValue> _mapFunc;

        SQLiteDbResult(ImmutableList<DbColumn> columns, Cursor cursor) {
            _columns = columns;
            _columnIndexes = columns.indexes();
            _mapFunc = this::cursorToDbValue;
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

        private DbValue cursorToDbValue(int index) {
            return _columns.get(index).isText()? new DbStringValue(_cursor.getString(index)) : new DbIntValue(_cursor.getInt(index));
        }

        @Override
        public ImmutableList<DbValue> next() {
            if (!hasNext()) {
                throw new UnsupportedOperationException("End already reached");
            }

            final ImmutableList<DbValue> result = _columnIndexes.map(_mapFunc);
            if (!_cursor.moveToNext()) {
                close();
            }
            _nextRowIndex++;

            return result;
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
        public boolean delete(DbDeleteQuery query) {
            return DbManager.delete(getWritableDatabase(), query);
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
