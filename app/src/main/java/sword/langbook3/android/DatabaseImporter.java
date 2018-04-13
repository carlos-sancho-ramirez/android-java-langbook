package sword.langbook3.android;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;

import sword.langbook3.android.db.DbImporter;
import sword.langbook3.android.sdb.ProgressListener;

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
                is.skip(20);
                final DatabaseInflater reader = new DatabaseInflater(db, is, _listener);
                reader.read();
            }
        }
        catch (IOException e) {
            throw new UnableToImportException();
        }
    }
}
