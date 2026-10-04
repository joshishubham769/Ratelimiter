import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //thread-safe in-memory Rate Limiter from scratch.
        try (RateLimiter limiter = new RateLimiter(3, 10, 20)) {
            ExecutorService executor = Executors.newFixedThreadPool(4);

            for (int i = 0; i < 150; i++) {
                System.out.println(i);
                if (i < 100) {
                    executor.submit(() -> {
                        Requester req = new Requester(Thread.currentThread().getName(), limiter);
                        req.request();
                    });
                }
            }
        }

    }
}