import java.util.concurrent.Callable;

public class Requester {

    private String userName;
    private RateLimiter limiter;

    public Requester(String userName, RateLimiter limiter){
        this.userName = userName;
        this.limiter = limiter;
    }

    public void request(){
        if(limiter.allow(userName)){
            System.out.println(Thread.currentThread().getName()+" "+userName+" "+"allowed");
        }
        else{
            System.out.println(Thread.currentThread().getName()+" "+userName+" "+"rejected");
        }
    }
}
