package engine;

/**
 * Created by SIMONE on 15/05/2016.
 */
public interface Engine {
	engine.State completeSearch();
	void enque(engine.State p);
	void threadFinished();
}
