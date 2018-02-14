import java.util.HashSet;

/**
 * Testing class for updateEnemyLocationMethod
 *
 * @author Daniel Luo
 */
public class testGameMap {
    /**
     * Method prints string "board" format
     */
    public static String arrayToString(int[][] arr) {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < arr.length; i++) {
            System.out.print('[');
            sb.append('[');
            for (int j = 0; j < arr[i].length - 1; j++) {
                System.out.print(arr[j][i] + "  ");
                sb.append(arr[j][i] + "  ");

            }
            System.out.print(arr[arr[i].length - 1][i]);
            sb.append(arr[arr[i].length - 1][i]);
            System.out.print("]\n");
            sb.append("]\n");
        }
        System.out.println("");

        return sb.toString();

    }

    /**
     * Main method to put testing methods in
     * @author Daniel Luo
     * @param args holds the main method arguments
     */
    public static void main(String[] args) {
        /**
         * the map will have:
         * 0's for empty, traversable squares
         * 1's for trees/resources
         * 2 for the enemy
         * 3 for the townhall
         */
        int[][] map = new int[10][10];

        for(int i = 0; i < 10; i++) {
            for(int j = 0; j < 10; j++) {
                map[i][j] = 0;
            }
        }

        // places trees at following locations on our control map array
        map[2][6] = 1;
        map[2][8] = 1;
        map[4][2] = 1;
        map[6][7] = 1;
        map[6][8] = 1;
        map[7][0] = 1;
        map[9][2] = 1;
        map[9][4] = 1;
        map[9][6] = 1;

        // place an enemy at 0,1
        map[0][1] = 2;
        // place a townhall at 0,0
        map[0][0] = 3;


        // create a set of maplocations of resources
        HashSet<AstarAgent.MapLocation> set = new HashSet<AstarAgent.MapLocation>();

        // fill in the set of maplocations of resources
        set.add(new AstarAgent.MapLocation(2,6,null,1));
        set.add(new AstarAgent.MapLocation(2,8,null,1));
        set.add(new AstarAgent.MapLocation(4,2,null,1));
        set.add(new AstarAgent.MapLocation(6,7,null,1));
        set.add(new AstarAgent.MapLocation(6,8,null,1));
        set.add(new AstarAgent.MapLocation(7,0,null,1));
        set.add(new AstarAgent.MapLocation(9,2,null,1));
        set.add(new AstarAgent.MapLocation(9,4,null,1));
        set.add(new AstarAgent.MapLocation(9,6,null,1));

        // creates MapLocation of an enemy
        AstarAgent.MapLocation enemyLoc = new AstarAgent.MapLocation(0,1,null,0);

        // creates a GameMap by taking input enemy location, resource locations, and townhall location
        // the constructor will also generate the generatedMap with locations filled in
        GameMap gameMap = new GameMap(10,10,enemyLoc,set,new AstarAgent.MapLocation(0,0,null,0));

        // this will get the map from GameMap after instantiation
        int[][] a = gameMap.getGeneratedMap();

        if(testGameMap.arrayToString(map).equals(testGameMap.arrayToString(a)))
            System.out.println("TRUE TEST PASSED");
        else
            System.out.println("FALSE TEST FAILED");
    }

}