import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class SavingsAccount {
    private final Lock lock = new ReentrantLock();
    private final Condition sufficientFunds = lock.newCondition();
    private final Condition noPrefWithdrawls = lock.newCondition();
    private double balance;
    private int numPreferred = 0;

    public double getBalance(){
        return balance;
    }

    public void deposit(double amount) {
        lock.lock();
        try {
            balance += amount;
            sufficientFunds.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void withdraw(boolean preferred, double amount) throws InterruptedException {
        lock.lock();
        try {
            if(preferred){
                numPreferred++;
            }else{
                //wait until numPreferred is 0
                while(numPreferred != 0){
                    noPrefWithdrawls.await();
                }
            }
            //wait for sufficient funds
            while (balance < amount) {
                sufficientFunds.await();
            }
            balance -= amount;
            if(preferred){
                //we're done with this preferred withdrawl
                numPreferred--;
                noPrefWithdrawls.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    public void transfer(double amount, SavingsAccount reserve) throws InterruptedException{
        lock.lock();
        try{
            while(balance < amount){
                sufficientFunds.await();
            }
            balance -= amount;
            reserve.deposit(amount);
        } finally {
            lock.unlock();
        }

    }
}



public class Main {

    public static void main(String[] args) {
        SavingsAccount account = new SavingsAccount();
        SavingsAccount account2 = new SavingsAccount();

        Runnable depositor = () -> {
            double amount = 100.0;
            account.deposit(amount);
            System.out.println("Deposited " + amount);
            System.out.println("Account 1 Balance: " + account.getBalance());
        };

        Runnable withdrawer = () -> {
            double amount = 50.0;
            try {
                account.withdraw(false, amount);
                System.out.println("Withdrew " + amount);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        System.out.println("Account 2 Balance: " + account.getBalance());
        Runnable transferer = () -> {
            double amount = 123.0;
            try{
                System.out.println("Account 2 Balance: " + account.getBalance());
                account.transfer(2.0, account2);

            } catch (InterruptedException e){
                e.printStackTrace();
            }
        };

        new Thread(depositor).start();
        new Thread(withdrawer).start();
        new Thread(transferer).start(); //spent 20 min debugging code only to realize I didn't start the thread

    }


}