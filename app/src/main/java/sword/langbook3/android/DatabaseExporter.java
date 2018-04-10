package sword.langbook3.android;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import sword.langbook3.android.db.DbInitializer;
import sword.langbook3.android.sdb.ProgressListener;
import sword.langbook3.android.sdb.StreamedDatabaseWriter;

public final class DatabaseExporter {

    private final Context _context;
    private final Uri _uri;
    private final ProgressListener _listener;

    DatabaseExporter(Context context, Uri uri, ProgressListener listener) {
        _context = context;
        _uri = uri;
        _listener = listener;
    }

    public void save(DbInitializer.Database db) throws DbInitializer.UnableToInitializeException {
        try {
            final OutputStream os = _context.getContentResolver().openOutputStream(_uri);
            if (os != null) {
                for (int i = 0; i < 20; i++) {
                    os.write(0);
                }

                MessageDigest md = MessageDigest.getInstance("MD5");
                final DigestOutputStream dos = new DigestOutputStream(os, md);
                final StreamedDatabaseWriter writer = new StreamedDatabaseWriter(db, dos, _listener);
                writer.write();

                // Adding the MD5 hash
                final RandomAccessFile raf = new RandomAccessFile(new File(_uri.getPath()), "rw");
                raf.write('S');
                raf.write('D');
                raf.write('B');
                raf.write(0);
                raf.write(md.digest());
                raf.close();
            }
        }
        catch (IOException | NoSuchAlgorithmException e) {
            throw new DbInitializer.UnableToInitializeException();
        }
    }
}
