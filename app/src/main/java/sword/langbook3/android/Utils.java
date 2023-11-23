package sword.langbook3.android;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import sword.langbook3.android.interf.ContextInterface;

public final class Utils {

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

    public static void copyFile(String originPath, String targetPath) throws IOException {
        copyFile(new FileInputStream(originPath), targetPath);
    }

    public static void copyFile(@NonNull ContextInterface context, Uri originUri, String targetPath) throws IOException {
        copyFile(context.getContentResolver().openInputStream(originUri), targetPath);
    }

    private static void copyFile(@NonNull ContextInterface context, InputStream inStream, Uri targetUri) throws IOException {
        try (OutputStream outStream = context.getContentResolver().openOutputStream(targetUri)) {
            copyBetweenStreams(inStream, outStream);
        }
    }

    public static void copyFile(@NonNull ContextInterface context, String originPath, Uri targetUri) throws IOException {
        try (InputStream inStream = new FileInputStream(originPath)) {
            copyFile(context, inStream, targetUri);
        }
    }
}
