import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class AstarAgent extends Agent {

    GameMap gamemap;

    class MapLocation {
        public int x, y;

        public MapLocation(int x, int y, MapLocation cameFrom, float cost) {
            this.x = x;
            this.y = y;
        }
    }

    Stack<MapLocation> path;
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;

    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs

    public AstarAgent(int playernum) {
        super(playernum);

        System.out.println("Constructed AstarAgent");
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        // get the footman location
        List<Integer> unitIDs = newstate.getUnitIds(playernum);

        if (unitIDs.size() == 0) {
            System.err.println("No units found!");
            return null;
        }

        footmanID = unitIDs.get(0);

        // double check that this is a footman
        if (!newstate.getUnit(footmanID).getTemplateView().getName().equals("Footman")) {
            System.err.println("Footman unit not found");
            return null;
        }

        // find the enemy playernum
        Integer[] playerNums = newstate.getPlayerNumbers();
        int enemyPlayerNum = -1;
        for (Integer playerNum : playerNums) {
            if (playerNum != playernum) {
                enemyPlayerNum = playerNum;
                break;
            }
        }

        if (enemyPlayerNum == -1) {
            System.err.println("Failed to get enemy playernumber");
            return null;
        }

        // find the townhall ID
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

        if (enemyUnitIDs.size() == 0) {
            System.err.println("Failed to find enemy units");
            return null;
        }

        townhallID = -1;
        enemyFootmanID = -1;
        for (Integer unitID : enemyUnitIDs) {
            Unit.UnitView tempUnit = newstate.getUnit(unitID);
            String unitType = tempUnit.getTemplateView().getName().toLowerCase();
            if (unitType.equals("townhall")) {
                townhallID = unitID;
            } else if (unitType.equals("footman")) {
                enemyFootmanID = unitID;
            } else {
                System.err.println("Unknown unit type");
            }
        }

        if (townhallID == -1) {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }

        long startTime = System.nanoTime();
        path = findPath(newstate);
        totalPlanTime += System.nanoTime() - startTime;

        // steps necessary to instantiate the gamemap once
        Unit.UnitView townhallUnit = newstate.getUnit(townhallID);
        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);
        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);
        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);
        MapLocation footmanLoc = null;
        if (enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = newstate.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }
        List<Integer> resourceIDs = newstate.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for (Integer resourceID : resourceIDs) {
            ResourceNode.ResourceView resource = newstate.getResourceNode(resourceID);
            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }
        if(footmanLoc == null){
            gamemap = new GameMap(newstate.getXExtent(), newstate.getYExtent(), resourceLocations, goalLoc);
        } else {
            gamemap = new GameMap(newstate.getXExtent(), newstate.getYExtent(), footmanLoc, resourceLocations, goalLoc);
        }
        gamemap.printMap();

        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        long startTime = System.nanoTime();
        long planTime = 0;

        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        if (shouldReplanPath(newstate, statehistory, path)) {
            long planStartTime = System.nanoTime();
            path = findPath(newstate);
            planTime = System.nanoTime() - planStartTime;
            totalPlanTime += planTime;
        }

        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);

        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();

        if (!path.empty() && (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {

            // stat moving to the next step in the path
            nextLoc = path.pop();

            System.out.println("Moving to (" + nextLoc.x + ", " + nextLoc.y + ")");
        }

        if (nextLoc != null && (footmanX != nextLoc.x || footmanY != nextLoc.y)) {
            int xDiff = nextLoc.x - footmanX;
            int yDiff = nextLoc.y - footmanY;

            // figure out the direction the footman needs to move in
            Direction nextDirection = getNextDirection(xDiff, yDiff);

            actions.put(footmanID, Action.createPrimitiveMove(footmanID, nextDirection));
        } else {
            Unit.UnitView townhallUnit = newstate.getUnit(townhallID);

            // if townhall was destroyed on the last turn
            if (townhallUnit == null) {
                terminalStep(newstate, statehistory);
                return actions;
            }

            if (Math.abs(footmanX - townhallUnit.getXPosition()) > 1 ||
                    Math.abs(footmanY - townhallUnit.getYPosition()) > 1) {
                System.err.println("Invalid plan. Cannot attack townhall");
                totalExecutionTime += System.nanoTime() - startTime - planTime;
                return actions;
            } else {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(footmanID, Action.createPrimitiveAttack(footmanID, townhallID));
            }
        }

        totalExecutionTime += System.nanoTime() - startTime - planTime;
        return actions;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
        System.out.println("Total turns: " + newstate.getTurnNumber());
        System.out.println("Total planning time: " + totalPlanTime / 1e9);
        System.out.println("Total execution time: " + totalExecutionTime / 1e9);
        System.out.println("Total time: " + (totalExecutionTime + totalPlanTime) / 1e9);
    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this method.
     * <p>
     * This method should return true when the path needs to be replanned
     * and false otherwise. This will be necessary on the dynamic map where the
     * footman will move to block your unit.
     *
     * @param state
     * @param history
     * @param currentPath
     * @return
     */
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath) {
        return false;
    }

    /**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state) {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);

        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);

        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);

        MapLocation footmanLoc = null;
        if (enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }

        // get resource locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for (Integer resourceID : resourceIDs) {
            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);

            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }

        return AstarSearch(startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
    }

    /**
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     * <p>
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     * <p>
     * As an example consider the following simple map
     * <p>
     * F - - - -
     * x x x - x
     * H - - - -
     * <p>
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     * <p>
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     * <p>
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     * <p>
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     * <p>
     * The path would be
     * <p>
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     * <p>
     * Notice how the initial footman position and the townhall position are not included in the path stack
     *
     * @param start             Starting position of the footman
     * @param goal              MapLocation of the townhall
     * @param xExtent           Width of the map
     * @param yExtent           Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @return Stack of positions with top of stack being first move in plan
     */
    private Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int
            xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations) {






        // return an empty path
        return new Stack<MapLocation>();
    }

    /**
     * Use the GameMap class to conceptualize the map.
     * Examples:
     *
     * To create a gamemap -
     * GameMap gamemap = new GameMap(xExtent, yExtent, enemyFootmanLoc, resourceLocations)
     *
     * To check if a tile is blocked by a tree or an enemy footman -
     * if(gamemap.isBlocked(5, 5)){
     *     System.out.println("can't go that way");
     * }
     *
     * To check what item is located at a position in the game board -
     * int num = gamemap.getPosition(2, 3);
     * if(num == 0){
     *     System.out.println("the board is clear at (2, 3)");
     * } else if(num == 1){
     *     System.out.println("there is a tree at (2, 3)");
     * } else if(num == 2){
     *     System.out.println("there is an enemy footman at (2, 3)");
     * } else if(num == 3){
     *     System.out.println("there is a townhall at (2, 3)");
     * }
     *
     * To get the location of the enemy footman -
     * int[][] enemyLocation = gamemap.getEnemyPosition();
     * int enemyX = enemyLocation[0];
     * int enemyY = enemyLocation[1];
     *
     * To get the size of the gameboard -
     * int lengthX = gamemap.getLengthX();
     * int lengthY = gamemap.getLengthY();
     *
     * To print out the appearance of the board -
     * gamemap.printMap();
     * @author Patrick Do
     */
    class GameMap {

        int[][] generatedMap;
        int[] enemyLocation;

        /**
         *
         * This is the constructor for the GameMap class, if there is an enemy footman on the board.
         *
         * @param xExtent               x length of the board.
         * @param yExtent               y length of the board.
         * @param enemyFootmanLoc       location of the enemy footman.
         * @param resourceLocations     set of locations of the trees.
         * @param townHallLoc           location of the goal - the enemy townhall.
         */
        public GameMap(int xExtent, int yExtent, MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations, MapLocation townHallLoc) {
            generatedMap = new int[xExtent][yExtent];
            enemyLocation = new int[2];
            addResourceLocations(resourceLocations, generatedMap);
            addTownHallLocation(townHallLoc);
            enemyLocation[0] = enemyFootmanLoc.x;
            enemyLocation[1] = enemyFootmanLoc.y;
            updateEnemyLocation(enemyFootmanLoc);
        }

        /**
         *
         * This is the constructor for the GameMap class, if there is no enemy footman on the board.
         * @param xExtent               x length of the board.
         * @param yExtent               y length of the board.
         * @param resourceLocations     set of locations of the trees.
         * @param townHallLoc           location of the goal - the enemy townhall.
         */
        public GameMap(int xExtent, int yExtent, Set<MapLocation> resourceLocations, MapLocation townHallLoc) {
            generatedMap = new int[xExtent][yExtent];
            enemyLocation = new int[2];
            addResourceLocations(resourceLocations, generatedMap);
            addTownHallLocation(townHallLoc);
            enemyLocation[0] = -1;
            enemyLocation[1] = -1;
        }

        /**
         *
         * Get the type of unit at the given position on the board. A 0 is a free space, a 1 is tree, a 2 is an enemy,
         * a 3 is a townhall.
         *
         * @param x
         * @param y
         * @return
         */
        public int getPosition(int x, int y) {
            return generatedMap[x][y];
        }

        /**
         *
         * Tells you if the given space is blocked - meaning its occupied by tree, an enemy, or a townhall.
         *
         * @param x
         * @param y
         * @return
         */
        public boolean isBlocked(int x, int y) {
            if (generatedMap[x][y] != 0) {
                return true;
            } else {
                return false;
            }
        }

        /**
         *
         * Tells you the position of the enemy. If there is no enemy on the board, returns -1, -1.
         * @return
         */
        public int[] getEnemyPosition() {
            return enemyLocation;
        }

        /**
         *
         * Tells you the width of the GameMap.
         *
         * @return
         */
        public int getLengthX(){
            return generatedMap.length;
        }

        /**
         *
         * tells you the height of the GameMap.
         *
         * @return
         */
        public int getLengthY(){
            return generatedMap[0].length;
        }

        /**
         *
         * Moves the footman from its last known position on the gamemap to its current new position.
         *
         * @param enemyFootmanLoc
         */
        private void updateEnemyLocation(MapLocation enemyFootmanLoc) {
            generatedMap[enemyLocation[0]][enemyLocation[1]] = 0;
            enemyLocation[0] = enemyFootmanLoc.x;
            enemyLocation[1] = enemyFootmanLoc.y;
            generatedMap[enemyLocation[0]][enemyLocation[1]] = 2;
        }

        /**
         *
         * Adds new resource locations to the map. Shouldn't be called outside of the constructor, since here
         * no new resources will be added to the board.
         *
         * @param resourceLocations
         * @param generatedMap
         */
        private void addResourceLocations(Set<MapLocation> resourceLocations, int[][] generatedMap) {
            Iterator<MapLocation> iter = resourceLocations.iterator();
            while (iter.hasNext()) {
                MapLocation tree = iter.next();
                generatedMap[tree.x][tree.y] = 1;
            }
        }

        /**
         *
         * Adds a new town hall location to the map. Shouldn't be called outside of constructor, since no new town
         * halls will be added to the board.
         *
         * @param townHallLoc
         */
        private void addTownHallLocation(MapLocation townHallLoc){
            generatedMap[townHallLoc.x][townHallLoc.y] = 3;
        }

        /**
         *
         * Prints out the map as numbers in the console. Easy way to verify the gamemap representation isn't completely
         * off.
         */
        private void printMap(){
            for(int k = 0; k < generatedMap[0].length; k++){
                for(int j = 0; j < generatedMap.length; j++){
                    System.out.print(generatedMap[j][k] + " ");
                }
                System.out.println();
            }
        }

    }

    /**
     * Primitive actions take a direction (e.g. NORTH, NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
    private Direction getNextDirection(int xDiff, int yDiff) {

        // figure out the direction the footman needs to move in
        if (xDiff == 1 && yDiff == 1) {
            return Direction.SOUTHEAST;
        } else if (xDiff == 1 && yDiff == 0) {
            return Direction.EAST;
        } else if (xDiff == 1 && yDiff == -1) {
            return Direction.NORTHEAST;
        } else if (xDiff == 0 && yDiff == 1) {
            return Direction.SOUTH;
        } else if (xDiff == 0 && yDiff == -1) {
            return Direction.NORTH;
        } else if (xDiff == -1 && yDiff == 1) {
            return Direction.SOUTHWEST;
        } else if (xDiff == -1 && yDiff == 0) {
            return Direction.WEST;
        } else if (xDiff == -1 && yDiff == -1) {
            return Direction.NORTHWEST;
        }

        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
}
