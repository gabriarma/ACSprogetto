package customException;

public class MaxNumberAccountReached extends Exception {
    public MaxNumberAccountReached(){}
    public MaxNumberAccountReached(String message){
        super(message);
    }
}