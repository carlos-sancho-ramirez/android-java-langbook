package sword.langbook3.android.sdb;

import java.io.OutputStream;

import sword.collections.ImmutableIntList;
import sword.collections.MutableIntList;

final class TestStream extends OutputStream {
    private final MutableIntList data = MutableIntList.empty();
    private int currentWord;
    private int byteCount;

    @Override
    public void write(int b) {
        final int wordByte = byteCount & 3;
        currentWord |= (b & 0xFF) << (wordByte * 8);

        ++byteCount;
        if ((byteCount & 3) == 0) {
            data.append(currentWord);
            currentWord = 0;
        }
    }

    AssertStream toInputStream() {
        final ImmutableIntList inData = ((byteCount & 3) == 0)? data.toImmutable() : data.toImmutable().append(currentWord);
        return new AssertStream(inData, byteCount);
    }
}
