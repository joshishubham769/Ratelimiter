public class Requester {

    private final String userName;
    private final RateLimiter limiter;

    public Requester(String userName, RateLimiter limiter){
        this.userName = userName;
        this.limiter = limiter;
    }

    public void request(){
        if(limiter.allow(userName)){
            System.out.println(userName+" "+"allowed");
        }
        else{
            System.out.println(userName+" "+"rejected");
        }
    }
}
