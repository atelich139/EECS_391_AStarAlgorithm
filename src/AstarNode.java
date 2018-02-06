import java.util.ArrayList;
import java.util.HashMap;

/**
 * The AstarNode Class
 *
 * @author Alexander Telich
 */
public class AstarNode {
    // x and y coordinates of the node
    private final int x, y;
    // HashMap for the map
    private final HashMap<Integer, ArrayList<Integer>> generatedMap;
    // The AstarNode which this one came from
    private final AstarNode cameFrom;
    
    /**
     * Constructor declaration for AstarNode
     *
     * @param x An int for the x-coordinate of the node
     * @param y An int for the y-coordinate of the node
     * @param generatedMap A HashMap for the map, where the Set of Integers is the y-values and
     *            each y-value corresponds to an ArrayList of Integers for x-values in
     *            that row.
     */
    public AstarNode(int x, int y, HashMap<Integer, ArrayList<Integer>> generatedMap,
                     AstarNode cameFrom) {
        this.x = x;
        this.y = y;
        this.generatedMap = generatedMap;
        this.cameFrom = cameFrom;
    }
    
    /**
     * Method getHeuristic calculates the heuristic
     *
     * @param goal The goal of the Astar search
     * @return A double that is the calculated heuristic to the goal
     */
    public double getHeuristic(AstarAgent.MapLocation goal) {
        // Using Chebyshev distance formula provided in the assignment instructions
        double heuristic = Math.max(Math.abs(goal.x - x), Math.abs(goal.y - y));
        
        return heuristic;
    }
    
    // TODO
    
    
    
    
    
    


}
