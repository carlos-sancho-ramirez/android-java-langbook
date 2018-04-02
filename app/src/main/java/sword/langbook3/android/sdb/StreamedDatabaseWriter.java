package sword.langbook3.android.sdb;

import java.io.IOException;
import java.io.OutputStream;

import sword.langbook3.android.db.DbInitializer;
import sword.langbook3.android.db.DbInitializer.Database;

public final class StreamedDatabaseWriter {

    private final Database _db;
    private final OutputStream _os;
    private final ProgressListener _listener;

    public StreamedDatabaseWriter(Database db, OutputStream os, ProgressListener listener) {
        _db = db;
        _os = os;
        _listener = listener;
    }

    public void write() {
        try {
            _os.close();
        }
        catch (IOException e) {
            // Nothing to be done
        }
    }
}
