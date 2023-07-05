package sword.langbook3.android;

import android.content.Context;
import android.net.Uri;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import sword.database.DbExporter;
import sword.langbook3.android.sdb.ProgressListener;
import sword.langbook3.android.sdb.StreamedDatabaseWriter;

public final class DatabaseExporter implements DbExporter {

    private final Context _context;
    private final Uri _uri;
    private final ProgressListener _listener;

    DatabaseExporter(Context context, Uri uri, ProgressListener listener) {
        _context = context;
        _uri = uri;
        _listener = listener;
    }

    @Override
    public void save(Database db) throws UnableToExportException {
        try {
            final OutputStream os = _context.getContentResolver().openOutputStream(_uri);
            if (os != null) {
                try {
                    os.write('S');
                    os.write('D');
                    os.write('B');
                    os.write(2);

                    final BufferedOutputStream bos = new BufferedOutputStream(os, 4096);
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    final DigestOutputStream dos = new DigestOutputStream(bos, md);
                    final StreamedDatabaseWriter writer = new StreamedDatabaseWriter(db, dos, _listener);
                    writer.write();
                    dos.flush();
                    bos.flush();

                    // Adding the MD5 hash
                    os.write(md.digest());
                }
                finally {
                    os.close();
                }
            }
        }
        catch (IOException | NoSuchAlgorithmException e) {
            throw new UnableToExportException();
        }
    }
}
