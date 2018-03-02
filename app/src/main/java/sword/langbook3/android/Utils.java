package sword.langbook3.android;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

final class Utils {

    static void copyFile(String originPath, String targetPath) throws IOException {
        final FileInputStream inStream = new FileInputStream(originPath);
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
}
