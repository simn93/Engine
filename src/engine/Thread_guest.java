package engine;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;

public class Thread_guest implements Runnable, Comparator<Runnable>{

	State father;
	EngineImpl Eng;
	
	public Thread_guest(){
		
	}
	
	public Thread_guest(State p, EngineImpl E){
		this.father = p;
		this.Eng = E;
	}
	
	@Override
	public void run() {
		for (Move m : father.getMoves()) {
			/* Avvio il thread per il calcolo dello stato
			 * O calcolo lo stato se sono in modalit� sequenziale
			 * */
            State P = null;

            Class<?> c = null;
            try {
                c = Class.forName(Eng.getObj());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            Constructor<?> cons = null;
            try {
                cons = c.getConstructor(State.class, Move.class, Engine.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            Object object = null;
            try {
                object = cons.newInstance(father, m, (Engine) Eng);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            P = (State) object;
            P.run();
        }
		Eng.threadFinished();
	}

	@Override
	public int compare(Runnable o1, Runnable o2) {
		Thread_guest arg00 = (Thread_guest) o1;
		Thread_guest arg11 = (Thread_guest) o2;
		State arg0 = (State) arg00.father;
		State arg1 = (State) arg11.father;
		return (int) Math.signum(arg0.value() - arg1.value());
	}
}
