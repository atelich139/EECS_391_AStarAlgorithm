import java.util.Iterator;
import java.util.Set;

/**
 * Use the GameMap class to conceptualize the map.
 * Examples:
 *
 * To create a gameMap -
 * GameMap gameMap = new GameMap(xExtent, yExtent, enemyFootmanLoc, resourceLocations)
 *
 * To check if a tile is blocked by a tree or an enemy footman -
 * if(gameMap.isBlocked(5, 5)){
 * System.out.println("can't go that way");
 * }
 *
 * To check what item is located at a position in the game board -
 * int num = gameMap.getPosition(2, 3);
 * if(num == 0){
 * System.out.println("the board is clear at (2, 3)");
 * } else if(num == 1){
 * System.out.println("there is a tree at (2, 3)");
 * } else if(num == 2){
 * System.out.println("there is an enemy footman at (2, 3)");
 * } else if(num == 3){
 * System.out.println("there is a townhall at (2, 3)");
 * }
 *
 * To get the location of the enemy footman -
 * int[][] enemyLocation = gameMap.getEnemyPosition();
 * int enemyX = enemyLocation[0];
 * int enemyY = enemyLocation[1];
 *
 * To get the size of the gameMap -
 * int lengthX = gameMap.getLengthX();
 * int lengthY = gameMap.getLengthY();
 *
 * To print out the appearance of the board -
 * gameMap.printMap();
 *
 * @author Patrick Do
 */
public class GameMap {
    
    private int[][] generatedMap;
    private int[] enemyLocation = new int[2];
    
    /**
     * This is the constructor for the GameMap class, if there is an enemy footman on
     * the board.
     *
     * @param xExtent           x length of the board.
     * @param yExtent           y length of the board.
     * @param enemyFootmanLoc   location of the enemy footman.
     * @param resourceLocations set of locations of the trees.
     * @param townHallLoc       location of the goal - the enemy townhall.
     */
    public GameMap(int xExtent, int yExtent, AstarAgent.MapLocation enemyFootmanLoc,
                   Set<AstarAgent.MapLocation> resourceLocations,
                   AstarAgent.MapLocation townHallLoc) {
        generatedMap = new int[xExtent][yExtent];
        addResourceLocations(resourceLocations, generatedMap);
        addTownHallLocation(townHallLoc);
        enemyLocation[0] = enemyFootmanLoc.x;
        enemyLocation[1] = enemyFootmanLoc.y;
        updateEnemyLocation(enemyFootmanLoc);
    }
    
    /**
     * This is the constructor for the GameMap class, if there is no enemy footman on
     * the board.
     *
     * @param xExtent           x length of the board.
     * @param yExtent           y length of the board.
     * @param resourceLocations set of locations of the trees.
     * @param townHallLoc       location of the goal - the enemy townhall.
     */
    public GameMap(int xExtent, int yExtent,
                   Set<AstarAgent.MapLocation> resourceLocations,
                   AstarAgent.MapLocation townHallLoc) {
        generatedMap = new int[xExtent][yExtent];
        addResourceLocations(resourceLocations, generatedMap);
        addTownHallLocation(townHallLoc);
        enemyLocation[0] = -1;
        enemyLocation[1] = -1;
    }
    
    /**
     * Get the type of unit at the given position on the board. A 0 is a free space, a
     * 1 is tree, a 2 is an enemy, a 3 is a townhall.
     *
     * @param x
     * @param y
     *
     * @return
     */
    public int getPosition(int x, int y) {
        return generatedMap[x][y];
    }
    
    /**
     * Tells you if the given space is blocked - meaning its occupied by tree, an
     * enemy, or a townhall.
     *
     * @param x
     * @param y
     *
     * @return
     */
    public boolean isBlocked(int x, int y) {
        return generatedMap[x][y] != 0;
    }
    
    /**
     * Tells you the position of the enemy. If there is no enemy on the board, returns
     * -1, -1.
     *
     * @return
     */
    public int[] getEnemyPosition() {
        return enemyLocation;
    }
    
    /**
     * Tells you the width of the GameMap.
     *
     * @return
     */
    public int getLengthX() {
        return generatedMap.length;
    }
    
    /**
     * tells you the height of the GameMap.
     *
     * @return
     */
    public int getLengthY() {
        return generatedMap[0].length;
    }
    
    public int[][] getGeneratedMap() {
        return generatedMap;
    }
    
    /**
     * Moves the footman from its last known position on the GameMap to its current
     * new position.
     *
     * @param enemyFootmanLoc
     */
    public void updateEnemyLocation(AstarAgent.MapLocation enemyFootmanLoc) {
        generatedMap[enemyLocation[0]][enemyLocation[1]] = 0;
        enemyLocation[0] = enemyFootmanLoc.x;
        enemyLocation[1] = enemyFootmanLoc.y;
        generatedMap[enemyLocation[0]][enemyLocation[1]] = 2;
    }
    
    /**
     * Adds new resource locations to the map. Shouldn't be called outside of the
     * constructor, since here
     * no new resources will be added to the board.
     *
     * @param resourceLocations
     * @param generatedMap
     */
    private void addResourceLocations(Set<AstarAgent.MapLocation> resourceLocations,
                                      int[][] generatedMap) {
        Iterator<AstarAgent.MapLocation> iter = resourceLocations.iterator();
        while (iter.hasNext()) {
            AstarAgent.MapLocation tree = iter.next();
            generatedMap[tree.x][tree.y] = 1;
        }
    }
    
    /**
     * Adds a new town hall location to the map. Shouldn't be called outside of
     * constructor, since no new town
     * halls will be added to the board.
     *
     * @param townHallLoc
     */
    private void addTownHallLocation(AstarAgent.MapLocation townHallLoc) {
        generatedMap[townHallLoc.x][townHallLoc.y] = 3;
    }
    
    /**
     * Prints out the map as numbers in the console. Easy way to verify the gameMap
     * representation isn't completely
     * off.
     */
    private void printMap() {
        for (int k = 0; k < generatedMap[0].length; k++) {
            for (int j = 0; j < generatedMap.length; j++) {
                System.out.print(generatedMap[j][k] + " ");
            }
            System.out.println();
        }
    }
}