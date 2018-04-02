package sword.langbook3.android.sdb;

import android.os.Environment;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import sword.langbook3.android.db.MemoryDatabase;

public final class StreamedDatabaseTest {

    @Test
    public void testReadWriteBasic() {
        final ProgressListener progressListener = (progress, message) -> {
            // Nothing to be done
        };

        final InputStream is = getClass().getClassLoader().getResourceAsStream("basic.sdb");
        final MemoryDatabase db = new MemoryDatabase();
        try {
            is.skip(20);
            final StreamedDatabaseReader reader = new StreamedDatabaseReader(db, is, progressListener);
            reader.read();
        }
        catch (IOException e) {
            Assert.fail();
        }
        finally {
            try {
                is.close();
            } catch (IOException e) {
                // Nothing to be done
            }
        }
    }
}
