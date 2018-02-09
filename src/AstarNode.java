import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * The AstarNode Class
 *
 * @author Alexander Telich
 * @author Daniel Luo
 */
public class AstarNode {
    // x and y coordinates of the node
    private final int x, y;
    // HashMap for the map
    private final Integer[][] generatedMap;

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
    public AstarNode(int x, int y, Integer[][] generatedMap,
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

    /**
     * Method getNeighbors gets the astarnode that are neighbors of the current astarnode
     *
     * @author Daniel Luo
     * @return a set of astarnode neighbors
     */
    public HashSet<AstarNode> getNeighbors() {
        HashSet<AstarNode> set = new HashSet<AstarNode>();

        for(int i = x - 1; i <= x + 1; i++) {
            for(int j = y - 1; j <= y + 1; j++) {
                if(generatedMap[j][i] == 0) {
                    set.add(new AstarNode(i, j, generatedMap, this));
                }
            }
        }

        return set;
    }

    /**
     * Method getTraverseCost gets the cost of traversing from this node to a neighbor node
     *
     * @author Daniel Luo
     * @return cost of traversing to a neighbor is always 1
     */
    public int getTraverseCost() {
        return 1;
    }

    /**
     * Method getCameFrom gets the astarnode that preceded the current astarnode in the agent's path
     *
     * @author Daniel Luo
     * @return previous astarnode in path
     */
    public AstarNode getCameFrom() {
        return cameFrom;
    }


}
