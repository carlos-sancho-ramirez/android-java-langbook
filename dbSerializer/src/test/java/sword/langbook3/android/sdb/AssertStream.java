package sword.langbook3.android.sdb;

import java.io.InputStream;

import sword.collections.ImmutableIntList;

final class AssertStream extends InputStream {
    private final ImmutableIntList data;
    private final int dataSizeInBytes;
    private int byteIndex;

    AssertStream(ImmutableIntList data, int dataSizeInBytes) {
        if (((dataSizeInBytes + 3) >>> 2) != data.size()) {
            throw new IllegalArgumentException();
        }

        this.data = data;
        this.dataSizeInBytes = dataSizeInBytes;
    }

    @Override
    public int read() {
        if (byteIndex >= dataSizeInBytes) {
            throw new AssertionError("End of the stream already reached");
        }

        final int wordIndex = byteIndex >>> 2;
        final int wordByteIndex = byteIndex & 3;
        ++byteIndex;
        return (data.valueAt(wordIndex) >>> (wordByteIndex * 8)) & 0xFF;
    }

    boolean allBytesRead() {
        return byteIndex == dataSizeInBytes;
    }
}
