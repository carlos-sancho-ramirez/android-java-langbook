package sword.langbook3.android.sdb;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import sword.collections.ImmutableIntList;
import sword.langbook3.android.db.MemoryDatabase;

public final class StreamedDatabaseTest {

    private static final class AssertingOutputStream extends OutputStream {

        private final ImmutableIntList _values;
        private final int _maxBytes;
        private int _byteCount;

        AssertingOutputStream(ImmutableIntList values, int maxBytes) {
            _values = values;
            _maxBytes = maxBytes;
        }

        @Override
        public void write(int value) throws IOException {
            if (_byteCount < 0) {
                throw new IOException("Already closed");
            }

            if (_byteCount >= _maxBytes) {
                throw new AssertionError("End reached");
            }

            final int expectedValue = (_values.get(_byteCount >> 2) >>> (8 * (_byteCount & 3))) & 0xFF;
            if (value != expectedValue) {
                throw new AssertionError();
            }
            ++_byteCount;
        }

        @Override
        public void close() throws IOException {
            if (_byteCount < 0) {
                throw new IOException("Already closed");
            }

            if (_byteCount != _maxBytes) {
                throw new AssertionError();
            }
            _byteCount = -1;
        }

        public boolean matchExpectations() {
            return _byteCount == -1;
        }
    }

    private static final class TestInputStream extends InputStream {
        private final InputStream _is;
        private int _byteCount;
        private ImmutableIntList.Builder _builder = new ImmutableIntList.Builder();
        private ImmutableIntList _result;
        private int _accValue;

        TestInputStream(InputStream is) {
            _is = is;
        }

        @Override
        public int read() throws IOException {
            if (_builder == null) {
                throw new IOException("Already closed");
            }

            final int value = _is.read();
            if (value < 0 || value > 255) {
                throw new AssertionError();
            }

            final int offset = _byteCount & 3;
            if (offset == 0) {
                _accValue = value;
            }
            else {
                _accValue |= value << (offset * 8);
            }

            if (offset == 3) {
                _builder.add(_accValue);
            }
            ++_byteCount;

            return value;
        }

        @Override
        public void close() throws IOException {
            _is.close();

            final int offset = _byteCount & 3;
            if (offset != 0) {
                _builder.add(_accValue);
            }

            _result = _builder.build();
            _builder = null;
        }

        public AssertingOutputStream getAssertingOutputStream() {
            if (_result == null) {
                throw new AssertionError("Stream not close yet");
            }

            return new AssertingOutputStream(_result, _byteCount);
        }
    }

    @Test
    public void testReadWriteBasic() {
        final ProgressListener progressListener = (progress, message) -> {
            // Nothing to be done
        };

        final InputStream origIs = getClass().getClassLoader().getResourceAsStream("basic.sdb");
        final MemoryDatabase db = new MemoryDatabase();
        try {
            origIs.skip(20);
            final TestInputStream is = new TestInputStream(origIs);
            final StreamedDatabaseReader reader = new StreamedDatabaseReader(db, is, progressListener);
            reader.read();

            final AssertingOutputStream os = is.getAssertingOutputStream();
            final StreamedDatabaseWriter writer = new StreamedDatabaseWriter(db, os, progressListener);
            writer.write();
            os.matchExpectations();
        }
        catch (IOException e) {
            Assert.fail();
        }
        finally {
            try {
                origIs.close();
            } catch (IOException e) {
                // Nothing to be done
            }
        }
    }
}
