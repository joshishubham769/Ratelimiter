public class RateLimitedState {
    private String user;
    private int count;
    private long windowStartTime;
    private RateLimitedState prev;
    private RateLimitedState next;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getWindowStartTime() {
        return windowStartTime;
    }

    public void setWindowStartTime(long windowStartTime) {
        this.windowStartTime = windowStartTime;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public RateLimitedState getPrev() {
        return prev;
    }

    public void setPrev(RateLimitedState prev) {
        this.prev = prev;
    }

    public RateLimitedState getNext() {
        return next;
    }

    public void setNext(RateLimitedState next) {
        this.next = next;
    }
}
