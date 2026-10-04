import java.util.concurrent.*;

public class RateLimiter implements AutoCloseable {

    private static final class State { int count; long windowStart; long lastAccess; }

    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();
    private final int countLimit;
    private final long windowNanos;
    private final long idleNanos;
    private final ScheduledExecutorService sweeper =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ratelimiter-sweeper");
                t.setDaemon(true);
                return t;
            });

    public RateLimiter(int countLimit, int windowSeconds, long idleSeconds) {
        this.countLimit = countLimit;
        this.windowNanos = TimeUnit.SECONDS.toNanos(windowSeconds);
        this.idleNanos = TimeUnit.SECONDS.toNanos(idleSeconds);
        sweeper.scheduleAtFixedRate(this::sweep, 1, 1, TimeUnit.MINUTES);
    }

    public boolean allow(String user) {
        final long now = System.nanoTime();
        final boolean[] allowed = new boolean[1];
        states.compute(user, (k, s) -> {
            if (s == null) {
                s = new State();
                s.windowStart = now;
            } else if (now - s.windowStart >= windowNanos) {
                System.out.println("New Window");
                s.windowStart = now;
                s.count = 0;
            }
            if (s.count < countLimit) {
                s.count++;
                allowed[0] = true;
            }
            s.lastAccess = now;
            return s;
        });
        return allowed[0];
    }

    private void sweep() {
        final long now = System.nanoTime();
        for (String key : states.keySet()) {
            // check and remove atomically under that key's lock
            states.computeIfPresent(key, (k, s) -> {
                if (now - s.lastAccess > idleNanos) {
                    System.out.println("removed " + k);
                    return null;   // returning null removes the entry
                }
                return s;          // keep the entry as is
            });
        }
    }

    @Override public void close() { sweeper.shutdown(); }
}