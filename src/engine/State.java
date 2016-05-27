package engine;

import java.util.Comparator;
import java.util.Vector;

/**
 * Created by Simo93 on 26/05/2016.
 */
public interface State extends Comparator<Runnable>, Runnable{
    boolean isGoal();
    boolean isFail();
    Vector<Move> getMoves();
    int value();
    void eval();
    String toString();
    String toPrintString();
    void getSolution();
    void run();
    int compare(Runnable o1, Runnable o2);

}
