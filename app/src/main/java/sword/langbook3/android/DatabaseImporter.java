package sword.langbook3.android;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import sword.database.DbImporter;
import sword.langbook3.android.sdb.DatabaseInflater;
import sword.langbook3.android.sdb.ProgressListener;
import sword.langbook3.android.sdb.StreamedDatabase0Reader;
import sword.langbook3.android.sdb.StreamedDatabase1Reader;
import sword.langbook3.android.sdb.StreamedDatabaseReader;
import sword.langbook3.android.sdb.StreamedDatabaseReaderInterface;

public final class DatabaseImporter implements DbImporter {

    private final Context _context;
    private final Uri _uri;
    private final ProgressListener _listener;

    DatabaseImporter(Context context, Uri uri, ProgressListener listener) {
        _context = context;
        _uri = uri;
        _listener = listener;
    }

    @Override
    public void init(Database db) throws UnableToImportException {
        try {
            final InputStream is = _context.getContentResolver().openInputStream(_uri);
            if (is != null) {
                if (is.read() != 'S' || is.read() != 'D' || is.read() != 'B') {
                    throw new UnableToImportException();
                }

                final int version = is.read();
                final BufferedInputStream bis = new BufferedInputStream(is, 4096);
                final StreamedDatabaseReaderInterface dbReader;
                if (version == 0) {
                    dbReader = new StreamedDatabase0Reader(db, bis, (_listener != null)? new DatabaseInflater.Listener(_listener, 0.25f) : null);
                }
                else if (version == 1) {
                    dbReader = new StreamedDatabase1Reader(db, bis, (_listener != null)? new DatabaseInflater.Listener(_listener, 0.25f) : null);
                }
                else if (version == 2) {
                    dbReader = new StreamedDatabaseReader(db, bis, (_listener != null)? new DatabaseInflater.Listener(_listener, 0.25f) : null);
                }
                else {
                    throw new UnableToImportException();
                }

                final DatabaseInflater reader = new DatabaseInflater(db, dbReader, _listener);
                reader.read();
            }
        }
        catch (IOException e) {
            throw new UnableToImportException();
        }
    }
}
