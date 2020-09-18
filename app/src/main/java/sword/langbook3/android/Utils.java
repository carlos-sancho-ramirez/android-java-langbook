package sword.langbook3.android;

import android.content.Context;
import android.net.Uri;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class Utils {

    private static void copyBetweenStreams(InputStream inStream, OutputStream outStream) throws IOException {
        final byte[] buffer = new byte[4096];
        int amount = 0;
        while (amount >= 0) {
            amount = inStream.read(buffer);
            if (amount > 0) {
                outStream.write(buffer, 0, amount);
            }
        }
    }

    private static void copyFile(InputStream inStream, String targetPath) throws IOException {
        try {
            try (FileOutputStream outStream = new FileOutputStream(targetPath)) {
                copyBetweenStreams(inStream, outStream);
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

    private static void copyFile(Context context, InputStream inStream, Uri targetUri) throws IOException {
        try (OutputStream outStream = context.getContentResolver().openOutputStream(targetUri)) {
            copyBetweenStreams(inStream, outStream);
        }
    }

    static void copyFile(Context context, String originPath, Uri targetUri) throws IOException {
        try (InputStream inStream = new FileInputStream(originPath)) {
            copyFile(context, inStream, targetUri);
        }
    }
}
