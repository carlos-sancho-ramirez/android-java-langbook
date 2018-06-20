package sword.langbook3.android;

import android.os.Parcel;
import android.os.Parcelable;

import sword.collections.ImmutableIntSet;
import sword.collections.MutableIntSet;

public final class QuizSelectorActivityState implements Parcelable {

    public interface IntrinsicStates {
        int INIT = 0;
        int READY = 1;
        int SELECTING_ON_LIST = 2;
        int DELETING = 3;
    }

    private int _intrinsicState = IntrinsicStates.INIT;

    // Only relevant for SELECTING_ON_LIST and DELETING
    private MutableIntSet _selectedQuizzes = MutableIntSet.empty();

    public QuizSelectorActivityState() {
    }

    private QuizSelectorActivityState(Parcel in) {
        _intrinsicState = in.readInt();
        if (_intrinsicState != 0) {
            final int size = in.readInt();
            for (int i = 0; i < size; i++) {
                _selectedQuizzes.add(in.readInt());
            }
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(_intrinsicState);
        if (_intrinsicState != 0) {
            dest.writeInt(_selectedQuizzes.size());
            for (int quizId : _selectedQuizzes) {
                dest.writeInt(quizId);
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    private void assertState(int intrinsicState) {
        if (_intrinsicState != intrinsicState) {
            throw new IllegalStateException();
        }
    }

    public boolean firstActionExecuted() {
        return _intrinsicState != IntrinsicStates.INIT;
    }

    public void setReady() {
        if (_intrinsicState != IntrinsicStates.INIT && _intrinsicState != IntrinsicStates.READY) {
            throw new AssertionError();
        }

        _intrinsicState = IntrinsicStates.READY;
    }

    public ImmutableIntSet getListSelection() {
        return _selectedQuizzes.toImmutable();
    }

    public void changeListSelection(int position, boolean selected) {
        if (_intrinsicState == IntrinsicStates.INIT) {
            throw new AssertionError();
        }

        if (selected) {
            if (_intrinsicState == IntrinsicStates.READY && _selectedQuizzes.isEmpty() ||
                    _intrinsicState == IntrinsicStates.SELECTING_ON_LIST ||
                    _intrinsicState == IntrinsicStates.DELETING && _selectedQuizzes.contains(position)) {
                _selectedQuizzes.add(position);
            }
            else {
                throw new AssertionError();
            }

            if (_intrinsicState == IntrinsicStates.READY) {
                _intrinsicState = IntrinsicStates.SELECTING_ON_LIST;
            }
        }
        else {
            assertState(IntrinsicStates.SELECTING_ON_LIST);
            _selectedQuizzes.remove(position);
        }
    }

    public void clearQuizSelection() {
        assertState(IntrinsicStates.SELECTING_ON_LIST);
        _selectedQuizzes.clear();
        _intrinsicState = IntrinsicStates.READY;
    }

    public void setDeletingState() {
        assertState(IntrinsicStates.SELECTING_ON_LIST);
        _intrinsicState = IntrinsicStates.DELETING;
    }

    public void clearDeleteState() {
        assertState(IntrinsicStates.DELETING);
        _intrinsicState = IntrinsicStates.SELECTING_ON_LIST;
    }

    public void clearDeleteStateAndSelection() {
        assertState(IntrinsicStates.DELETING);
        _selectedQuizzes.clear();
        _intrinsicState = IntrinsicStates.READY;
    }

    public boolean shouldDisplayDeleteDialog() {
        return _intrinsicState == IntrinsicStates.DELETING;
    }

    public static final Creator<QuizSelectorActivityState> CREATOR = new Creator<QuizSelectorActivityState>() {
        @Override
        public QuizSelectorActivityState createFromParcel(Parcel in) {
            return new QuizSelectorActivityState(in);
        }

        @Override
        public QuizSelectorActivityState[] newArray(int size) {
            return new QuizSelectorActivityState[size];
        }
    };
}
