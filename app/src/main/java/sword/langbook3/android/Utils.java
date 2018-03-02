package sword.langbook3.android;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

final class Utils {

    private static void copyFile(InputStream inStream, String targetPath) throws IOException {
        try {
            final FileOutputStream outStream = new FileOutputStream(targetPath);
            try {
                final byte[] buffer = new byte[4096];
                int amount = 0;
                while (amount >= 0) {
                    amount = inStream.read(buffer);
                    if (amount > 0) {
                        outStream.write(buffer, 0, amount);
                    }
                }
            }
            finally {
                outStream.close();
            }
        }
        finally {
            inStream.close();
        }
    }

    static void copyFile(String originPath, String targetPath) throws IOException {
        copyFile(new FileInputStream(originPath), targetPath);
    }

    static void copyFile(Context context, Uri originUri, String targetPath) throws IOException {
        copyFile(context.getContentResolver().openInputStream(originUri), targetPath);
    }
}
