import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LlenaVacia1
{
   public static void main(String[] args)
   {
      Datos s = new Datos();
      new Llena(s).start();
      new Lee(s).start();
   }
}

class Datos
{
   // Fields c and available are volatile so that writes to them are visible to 
   // the various threads. Fields lock and condition are final so that they're
   // initial values are visible to the various threads. (The Java memory model 
   // promises that, after a final field has been initialized, any thread will 
   // see the same [correct] value.)

   private volatile char c;
   private volatile boolean available;
   private final Lock lock;
   private final Condition condition;

   Datos()
   {
      c = '\u0000'; //valor nulo (unicode internacional)
      available = false;
      lock = new ReentrantLock();
      condition = lock.newCondition();
   }    

   Lock getLock()
   {
      return lock;
   }

   char getDatosChar()
   {
      lock.lock();
      try
      {
         while (!available)
            try
            {
               condition.await();
            }
            catch (InterruptedException ie)
            {
               ie.printStackTrace();
            }
         available = false;
         System.out.println(c + " consumed by consumer.");
         condition.signal();
      }
      finally
      {
         lock.unlock();
         return c;
      }
   }

   void setDatosChar(char c)
   {
      lock.lock();
      try
      {
         while (available)
            try
            {
               condition.await();
            }
            catch (InterruptedException ie)
            {
               ie.printStackTrace();
            }
         this.c = c;
         available = true;
         System.out.println(c + " produced by producer.");
         condition.signal();
      }
      finally
      {
         lock.unlock();
      }
   }
}

class Llena extends Thread
{
   // l is final because it's initialized on the main thread and accessed on the
   // producer thread.

   private final Lock l;

   // s is final because it's initialized on the main thread and accessed on the
   // producer thread.

   private final Datos s;
   
   Llena(Datos s)
   {
      this.s = s;
      l = s.getLock();
   }

   @Override
   public void run()
   {
      for (char ch = 'A'; ch <= 'Z'; ch++)
      {
        s.setDatosChar(ch);
      }
   }
}
class Lee extends Thread
{
   // l is final because it's initialized on the main thread and accessed on the
   // consumer thread.

   private final Lock l;

   // s is final because it's initialized on the main thread and accessed on the
   // consumer thread.

   private final Datos s;

   Lee(Datos s)
   {
      this.s = s;
      l = s.getLock();
   }

   @Override
   public void run()
   {
      char ch;
      do
      {
         ch = s.getDatosChar();
      }
      while (ch != 'Z');
   }
}