import java.util.LinkedList;

/**
 * The AstarNode Class
 *
 * @author Alexander Telich
 * @author Daniel Luo
 */
public class AstarNode {
    // x and y coordinates of the node
    private final int x, y;
    private final AstarAgent.MapLocation mapLocation;
    // 2D array for the map
    private int[][] generatedMap;
    // The GameMap object
    private GameMap gameMap;
    // Stores the heuristic for the node if necessary
    private Double cachedHeuristic;
    
    /**
     * Constructor declaration for AstarNode
     *
     * @param x       An int for the x-coordinate of the node
     * @param y       An int for the y-coordinate of the node
     * @param gameMap A HashMap for the map, where the Set of Integers is the y-values and
     *                each y-value corresponds to an ArrayList of Integers for x-values in
     *                that row.
     *
     * @author Alexander Telich
     */
    public AstarNode(int x, int y, GameMap gameMap) {
        this.mapLocation = new AstarAgent.MapLocation(x, y, null, 0);
        this.x = x;
        this.y = y;
        this.generatedMap = gameMap.getGeneratedMap();
        this.gameMap = gameMap;
    }
    
    /**
     * Method getHeuristic calculates the heuristic
     *
     * @param goal The goal of the Astar search
     *
     * @return A double that is the calculated heuristic to the goal
     *
     * @author Alexander Telich
     */
    public Double getHeuristic(AstarAgent.MapLocation goal) {
        // Using Chebyshev distance formula provided in the assignment instructions
        return (double) Math.max(Math.abs(goal.x - x), Math.abs(goal.y - y));
    }
    
    /**
     * Method getNeighbors gets the AstarNode that are neighbors of the current AstarNode
     *
     * @return a set of AstarNode neighbors
     *
     * @author Alexander Telich
     * @author Daniel Luo
     */
    public LinkedList<AstarNode> getNeighbors() {
        LinkedList<AstarNode> neighbors = new LinkedList<>();
        
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                if ((i == x && j == y) || i < 0 || j < 0 || j >= gameMap.getLengthY() ||
                    i >= gameMap.getLengthX()) {
                    continue;
                }
                
                if (generatedMap[i][j] == 0) {
                    neighbors.add(new AstarNode(i, j, gameMap));
                }
            }
        }
        return neighbors;
    }
    
    /**
     * Method getTraverseCost gets the cost of traversing from this node to a neighbor
     * node
     *
     * @return cost of traversing to a neighbor is always 1
     *
     * @author Daniel Luo
     */
    public int getTraverseCost() {
        return 1;
    }
    
    /**
     * Getter methods for getting x and y coordinate of this AstarNode
     *
     * @return int of either the x location of the y location
     *
     * @author Alexander Telich
     */
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    /**
     * Getter method for getting the MapLocation of this AstarNode.
     *
     * @return MapLocation of this AstarNode
     *
     * @author Alexander Telich
     */
    public AstarAgent.MapLocation getMapLocation() {
        return mapLocation;
    }
    
    /**
     * Getter method for getting the cached heuristic of this node.
     *
     * @return Double that is the calculated heuristic
     */
    public Double getCachedHeuristic() {
        return cachedHeuristic;
    }
    
    /**
     * Setter for setting cached heuristic
     *
     * @param cachedHeuristic Double that represents the calculated heuristic
     */
    public void setCachedHeuristic(Double cachedHeuristic) {
        this.cachedHeuristic = cachedHeuristic;
    }
    
    
}
