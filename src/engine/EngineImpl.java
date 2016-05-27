package engine;

import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classe per la gestione della ricerca della soluzione.
 * <p>
 * Implementa l'algoritmo BF per l'estrazione del nodo da espandere.
 * <p>
 * Implementa una pseudo-DFS, ma solo in modalit� multithread, 
 * in quanto d� la precedenza di esecuzione ai nodi i cui padri hanno il valore pi� promettente.
 * <p>
 * Per questioni di efficienza Peg_game modifica in maniera diretta "q", "q2" e "activethread".
 *   	
 * @author Simone Schirinzi
 *
 */
public class EngineImpl implements Comparator<State>, Engine{
	/** Queue of states */
	private PriorityBlockingQueue<State> q;
	
	/** Queue of thread to run */
	private PriorityBlockingQueue<Runnable> mq;
	
	/** Map of visited states */
	private Set<String> q2;
	
	/** Number of expanded nodes */
	private int expandedNodes;
	
	/** Number of added table */
	private int addedNodes;

	/** Limit value for queue insertion */
	private final int limitValue = 1000;

	/** Limit size for queue */
	private final int limitSize = 100000;
	
	/** Number of local cores */
	private final int cores;
	
	/** Load factor for hashmap */
	//public final float loadFactor = 0.65f;
	
	/** Threadpool */
	private ExecutorService pool;

	/** Number of job started and not finished */
	private AtomicInteger activethread;
    
	/** For single thread execution */
	private boolean isSingleThread;

	/** Concrete object */
	private String obj;

	/** Avvio con seme
	 * @param seed: seme
	 * @param mode:true modalit� single thread
	 * @param mode:false modalit� multi thread 
	 *
	 * Pool di thread:
	 * Un numero fisso di thread in esecuzione: cores
	 * Tempo massimo di esecuzione per thread: 2^63ms
	 * Viene scelto per l'esecuzione il thread il cui padre ha il valore pi� promettente
	 * scelto tramite una coda a priorit� mq
	 * */
	public EngineImpl(State seed, boolean mode, String game) {
		if(seed == null) throw new NullPointerException();
		this.q = new PriorityBlockingQueue<>(limitSize, this);
		this.mq = new PriorityBlockingQueue<>(limitSize, new Thread_guest());
		this.q2 = Collections.newSetFromMap (new ConcurrentHashMap<String, Boolean>());
		this.expandedNodes = 0;
		this.addedNodes = 0;
		this.cores = Runtime.getRuntime().availableProcessors();
		this.pool = new ThreadPoolExecutor (cores,cores,Long.MAX_VALUE,TimeUnit.NANOSECONDS,mq);
		this.activethread = new AtomicInteger(0);
		this.isSingleThread = mode;
		
		if(isSingleThread) pool.shutdown();

		seed.eval();
		this.q.add(seed);
		this.q2.add(seed.toString());

		this.obj = game;
	}
	
	/**
	* Expand a state putting its successors in the queue
	* @return the solution state if the goal has been reached
	* null otherwise
	**/
	private State expand() throws InterruptedException {
		//must exit
		if(pool.isShutdown()&& !isSingleThread) return null; 
		
		/* Estraggo un nodo dalla coda.
		mi metto in attesa se essa � vuota.
		Il nodo estratto lo considero gi� valutato */
		State item = q.take();
		
		/* ho espando un nodo */
		expandedNodes++;
		
		//controllo se sono allo stato goal
		if(item.isGoal()){
			if(!isSingleThread)pool.shutdown();
			return item;
		}
		
		//controllo se sono allo stato fail
		if(item.isFail()){
			if(!isSingleThread)pool.shutdown();
			return null;
		}
		
		//visito TUTTI i figli
        /* Ho aggiunto un nodo alla lista
				di quelli da elaborare*/
        this.addedNodes += item.getMoves().size();

        this.activethread.getAndIncrement();
		if(isSingleThread){
            Thread_guest tg = new Thread_guest(item,this);
            tg.run();
        }
		else{
			/* Notifico che sto avviando un worker */

			pool.execute(new Thread_guest(item,this));
            // TODO:astrarre qui.
		}
		
		return null;
	}

	/**
	* Perform a complete search
	* @return the solution state if the goal has been reached
	* null otherwise
	*/
	@Override
	public State completeSearch() {
		/* variabile per l'iterazione */
		State son = null;
		
		/* si espande il seme */
		try {
			son = expand();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		/* main loop */
		while(	
			/* la coda non � vuota e/o ci sono ancora
			dei thread in elaborazione */
			!(q.isEmpty() && activethread.compareAndSet(0,0))
			
			// la coda non � cresciuta troppo
			&& q.size() < limitSize 
			&& mq.size() < limitSize
						
			/* se son � diverso da null, 
			allora ho trovato una soluzione */
			&& son == null
			
			/* il pool di thread � attivo */		
			&& (!pool.isShutdown() || isSingleThread) ){
				
				/* Vai avanti con la ricerca */			
				try {
					son = expand();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
		
		/* Sei uscito dal main loop */
		/* Termina il threadpool 
		se non � ancora stato terminato */
		if(!isSingleThread)pool.shutdown();
		
		/* Avvisa che hai terminato la ricerca */
		java.awt.Toolkit.getDefaultToolkit().beep();
		
		/* Restituisci il risultato */
		return son;
	}
	
	@Override
	public void threadFinished(){ 
		activethread.getAndDecrement();
	}

    public String getObj(){ return this.obj;}

	@Override
	public void enque(State p){
		/* Se il valore di uno stato � oltre il limite,
		oppure se non ho gi� visitato questo stato */
		if (p.value() < limitValue &&
				!q2.contains(p.toString())) {
			/* Allora posso aggiungerlo alla coda di priorit� */
			q.add(p);

			/* E alla coda degli stati visitati */
			q2.add(p.toString());
		}
	}
	/**
	* @return a string with 
	* 	the queue size
	* 	the minimal value
	*	the number of expanded nodes and
	*	the number of added nodes
	*/
    @Override
	public String toString() {
		String ret = "";
		
		ret+= "Queue size : " + q.size();
		
		if(q.isEmpty()) ret+=", Empty queue";
		else{ret+= ", min value : " + q.peek().value();} 
		
		ret+= ", expanded nodes : " + expandedNodes;
		ret+= ", evaluated nodes : " + addedNodes;
		return ret+ "";
	}
		
	/**
	* Comparator for the Peg_game Priority Queue
	* @return the sign of the difference arg0.value()-arg1.value()
	*/
	public int compare(State arg0, State arg1) {
		return (int) Math.signum(arg0.value() - arg1.value());
	}
}