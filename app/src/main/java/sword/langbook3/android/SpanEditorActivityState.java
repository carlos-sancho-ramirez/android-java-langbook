package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.collections.ImmutableIntRange;
import sword.collections.MutableIntValueMap;
import sword.langbook3.android.LangbookReadableDatabase.SentenceSpan;

public final class SpanEditorActivityState implements Parcelable {

    private final MutableIntValueMap<SentenceSpan> spans = MutableIntValueMap.empty();
    private ImmutableIntRange selection;

    ImmutableIntRange getSelection() {
        return selection;
    }

    void setSelection(ImmutableIntRange range) {
        if (range != null && range.min() < 0) {
            throw new IllegalArgumentException();
        }

        selection = range;
    }

    MutableIntValueMap<SentenceSpan> getSpans() {
        return spans;
    }

    public static final Creator<SpanEditorActivityState> CREATOR = new Creator<SpanEditorActivityState>() {
        @Override
        public SpanEditorActivityState createFromParcel(Parcel in) {
            final SpanEditorActivityState state = new SpanEditorActivityState();
            final int start = in.readInt();
            if (start >= 0) {
                final int end = in.readInt();
                state.setSelection(new ImmutableIntRange(start, end));
            }

            sentenceSpanSetFromParcel(state.getSpans(), in);
            return state;
        }

        @Override
        public SpanEditorActivityState[] newArray(int size) {
            return new SpanEditorActivityState[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (selection != null) {
            dest.writeInt(selection.min());
            dest.writeInt(selection.max());
        }
        else {
            dest.writeInt(-1);
        }

        writeSentenceSpanSetToParcel(dest);
    }

    private static void sentenceSpanSetFromParcel(MutableIntValueMap<SentenceSpan> builder, Parcel in) {
        final int size = in.readInt();
        for (int i = 0; i < size; i++) {
            builder.put(SentenceSpan.fromParcel(in), in.readInt());
        }
    }

    private void writeSentenceSpanSetToParcel(Parcel dest) {
        final int size = spans.size();
        dest.writeInt(size);
        for (int i = 0; i < size; i++) {
            spans.keyAt(i).writeToParcel(dest);
            dest.writeInt(spans.valueAt(i));
        }
    }

    private static boolean sentenceSpanSortFunction(SentenceSpan a, SentenceSpan b) {
        return b != null && (a == null || a.range.min() < b.range.min() ||
                a.range.min() == b.range.min() && (a.range.max() < b.range.max() ||
                        a.range.max() == b.range.max() && a.acceptation < b.acceptation));
    }
}
