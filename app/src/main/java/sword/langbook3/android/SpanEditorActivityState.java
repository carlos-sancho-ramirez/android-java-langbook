package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.collections.ImmutableHashSet;
import sword.collections.ImmutableIntRange;
import sword.collections.ImmutableSet;
import sword.langbook3.android.LangbookReadableDatabase.SentenceSpan;

public final class SpanEditorActivityState implements Parcelable {

    private ImmutableSet<SentenceSpan> spans = ImmutableHashSet.empty();
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

    ImmutableSet<SentenceSpan> getSpans() {
        return spans;
    }

    void composeSpanWithCurrentSelection(int acceptation) {
        spans = spans.add(new SentenceSpan(selection, acceptation));

        if (spans.size() > 1) {
            spans = spans.sort(SpanEditorActivityState::sentenceSpanSortFunction);
        }
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

            state.spans = sentenceSpanSetFromParcel(in);
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

    private static ImmutableSet<SentenceSpan> sentenceSpanSetFromParcel(Parcel in) {
        final int size = in.readInt();
        final ImmutableHashSet.Builder<SentenceSpan> builder = new ImmutableHashSet.Builder<>();
        for (int i = 0; i < size; i++) {
            builder.add(SentenceSpan.fromParcel(in));
        }

        return builder.build().sort(SpanEditorActivityState::sentenceSpanSortFunction);
    }

    private void writeSentenceSpanSetToParcel(Parcel dest) {
        final int size = spans.size();
        dest.writeInt(size);
        for (int i = 0; i < size; i++) {
            spans.valueAt(i).writeToParcel(dest);
        }
    }

    private static boolean sentenceSpanSortFunction(SentenceSpan a, SentenceSpan b) {
        return b != null && (a == null || a.range.min() < b.range.min() ||
                a.range.min() == b.range.min() && (a.range.max() < b.range.max() ||
                        a.range.max() == b.range.max() && a.acceptation < b.acceptation));
    }
}
