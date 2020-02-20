package sword.langbook3.android.sdb;

public interface ProgressListener {

    /**
     * Notify the progress of a task
     * @param progress float value expected to be between 0 and 1.
     * @param message Text message describing the current subTask.
     */
    void setProgress(float progress, String message);
}
