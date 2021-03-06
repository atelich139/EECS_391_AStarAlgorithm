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
    
    // Cache for the GameMap
    GameMap gameMap;
    HashMap<AstarNode, AstarNode[]> cachedNeighbors = new HashMap<>();
    MapLocation startLoc1;
    boolean pathReplanCount;
    
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
    public Map<Integer, Action> initialStep(State.StateView newstate,
                                            History.HistoryView statehistory) {
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
        
        // steps necessary to instantiate the gamemap once
        Unit.UnitView townhallUnit = newstate.getUnit(townhallID);
        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);
        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(),
                                               footmanUnit.getYPosition(), null, 0);
        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(),
                                              townhallUnit.getYPosition(), null, 0);
        MapLocation footmanLoc = null;
        if (enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = newstate.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(),
                                         enemyFootmanUnit.getYPosition(), null, 0);
        }
        
        List<Integer> resourceIDs = newstate.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for (Integer resourceID : resourceIDs) {
            ResourceNode.ResourceView resource = newstate.getResourceNode(resourceID);
            resourceLocations
                    .add(new MapLocation(resource.getXPosition(), resource.getYPosition(),
                                         null, 0));
        }
        
        if (footmanLoc == null) {
            gameMap = new GameMap(newstate.getXExtent(), newstate.getYExtent(),
                                  resourceLocations, goalLoc);
        } else {
            gameMap = new GameMap(newstate.getXExtent(), newstate.getYExtent(),
                                  footmanLoc, resourceLocations, goalLoc);
        }
        
        long startTime = System.nanoTime();
        path = findPath(newstate);
        totalPlanTime += System.nanoTime() - startTime;
        
        return middleStep(newstate, statehistory);
    }
    
    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate,
                                           History.HistoryView statehistory) {
        long startTime = System.nanoTime();
        long planTime = 0;

        // Instantiates the actions that are to be taken
        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        // If the agent should replan its path, then it will find a path again for the new state
        if (shouldReplanPath(newstate, path)) {
            long planStartTime = System.nanoTime();
            path = findPath(newstate);
            planTime = System.nanoTime() - planStartTime;
            totalPlanTime += planTime;
        }
        
        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);

        // Gets the position of the footman and stores it
        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();

        // If the path is not empty and we have a nextLocation then move to it
        if (!path.empty() &&
            (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {
            
            // stat moving to the next step in the path
            nextLoc = path.pop();
            
            System.out.println("Moving to (" + nextLoc.x + ", " + nextLoc.y + ")");
        }

        // If the next location is not found yet then move in next direction
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
                
                System.out.println(totalExecutionTime);
                
                return actions;
            } else {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(footmanID,
                            Action.createPrimitiveAttack(footmanID, townhallID));
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
     *
     * This method should return true when the path needs to be re-planned
     * and false otherwise. This will be necessary on the dynamic map where the
     * footman will move to block your unit.
     *
     * @param state
     * @param currentPath
     *
     * @return
     */
    private boolean shouldReplanPath(State.StateView state,
                                     Stack<MapLocation> currentPath) {
        if (enemyFootmanID < 0) return false;
        else {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            MapLocation enemyLoc = new MapLocation(enemyFootmanUnit.getXPosition(),
                                                   enemyFootmanUnit.getYPosition(), null,
                                                   0);
            if (currentPath.contains(enemyLoc)) {
                gameMap.updateEnemyLocation(enemyLoc);
                pathReplanCount = true;
                return true;
            }else {
                return false;
            }
        }
        
    }
    
    /**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     *
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state) {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);
        
        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(),
                                               footmanUnit.getYPosition(), null, 0);
        if (!pathReplanCount) startLoc1 = startLoc;
        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(),
                                              townhallUnit.getYPosition(), null, 0);
        
        return AstarSearch(startLoc, goalLoc);
    }
    
    /**
     * This is the method for the AStar Algorithm
     *
     * @param start Starting position of the footman
     * @param goal  MapLocation of the townhall
     *
     * @return Stack of positions with top of stack being first move in plan
     *
     * @author Alexander Telich
     */
    private Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal) {
        MapLocation goal1 = new MapLocation(goal.x - 1, goal.y, null, (float) 0.0);
        MapLocation goal2 = new MapLocation(goal.x - 1, goal.y - 1, null, (float) 0.0);
        Stack<MapLocation> path = new Stack<>();
        AstarNode startNode = new AstarNode(start.x, start.y, gameMap);
        AstarNode[] neighbors;
        HashMap<AstarNode, AstarNode> cameFrom = new HashMap<>();
        HashMap<AstarNode, Double> gValue = new HashMap<>();
        HashMap<AstarNode, Double> fValue = new HashMap<>();
        Set<AstarNode> closedList = new HashSet<>();
        PriorityQueue<AstarNode> openList = new PriorityQueue<>(
                new Comparator<AstarNode>() {
                    @Override
                    public int compare(AstarNode o1, AstarNode o2) {
                        return Double.compare(fValue.get(o1), fValue.get(o2));
                    }
                });
        int goalQuadrant = new Quadrant().getQuadrant(goal.x, goal.y);
        
        
        gValue.put(startNode, 0.0);
        fValue.put(startNode, startNode.getHeuristic(goal));
        openList.offer(startNode);
        
        int quadrantArea1 = new Quadrant().getQuadrantArea(goal.x, goal.y);
        boolean quadrantViolated = false;
        
        while (!openList.isEmpty()) {
            AstarNode current = openList.poll();
            
            if (current.getMapLocation().equals(goal1) || current.getMapLocation()
                                                                 .equals(goal2)) {
                while (!current.equals(startNode)) {
                    path.push(current.getMapLocation());
                    current = cameFrom.get(current);
                }
                return path;
            }
            
            closedList.add(current);
            
            if (closedList.size() == quadrantArea1) {
                quadrantViolated = true;
            }
            
            if (!cachedNeighbors.isEmpty() && cachedNeighbors.containsKey(current)) {
                neighbors = cachedNeighbors.get(current);
                
            } else {
                neighbors = current.getNeighbors();
                cachedNeighbors.putIfAbsent(current, neighbors);
            }
            
            for (AstarNode neighbor : neighbors) {
                if (closedList.contains(neighbor)) continue;
                if (neighbor == null) continue;
                
                int neighborQuadrant = new Quadrant().getQuadrant(neighbor.getX(),
                                                                      neighbor.getY());
                
                if (!quadrantViolated && !pathReplanCount && neighborQuadrant
                                                             != goalQuadrant) {
                    continue;
                }
                
                Double tentativeG = gValue.get(current) + current.getTraverseCost();
                Double neighborHeuristic;
                
                if (!openList.contains(neighbor) ||
                    tentativeG < gValue.get(current)) {
                    
                    if (neighbor.getCachedHeuristic() != null) {
                        neighborHeuristic = neighbor.getCachedHeuristic();
                    } else {
                        neighborHeuristic = neighbor.getHeuristic(goal);
                        neighbor.setCachedHeuristic(neighborHeuristic);
                    }
                    
                    gValue.put(neighbor, tentativeG);
                    fValue.put(neighbor, tentativeG + neighborHeuristic);
                    
                    if (openList.contains(neighbor)) {
                        openList.remove(neighbor);
                    }
                    
                    openList.offer(neighbor);
                    cameFrom.put(neighbor, current);
                }
            }
        }
        return null;
    }
    
    /**
     * Primitive actions take a direction (e.g. NORTH, NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     *
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
    
    static class MapLocation {
        public int x, y;
        
        public MapLocation(int x, int y, MapLocation cameFrom, float cost) {
            this.x = x;
            this.y = y;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MapLocation c = (MapLocation) obj;
            
            if (c.x == this.x && c.y == this.y) {
                return true;
            } else {
                return false;
            }
        }
    }
    
    /**
     * Helper class for getting the quadrant area and and quadrants.
     */
    class Quadrant {
        private int getQuadrant(int x, int y) {
            int nodeX = x;
            int nodeY = y;
            
            final int startX = startLoc1.x;
            final int startY = startLoc1.y;
            
            int cX = nodeX - startX;
            int cY = nodeY - startY;
            
            int nodeQuadrant = 0;
            
            if (cX >= 0 && cY < 0) nodeQuadrant = 1;
            else if (cX > 0 && cY >= 0) nodeQuadrant = 2;
            else if (cX <= 0 && cY > 0) nodeQuadrant = 3;
            else if (cX > 0 && cY <= 0) nodeQuadrant = 4;
            
            return nodeQuadrant;
        }
        
        private int getQuadrantArea(int x, int y) {
            int width = Math.abs(x - startLoc1.x);
            int height = Math.abs(y - startLoc1.y);
            
            int area = width * height;
            return area;
        }
        
    }
}
