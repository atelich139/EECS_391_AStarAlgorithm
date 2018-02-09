
/**
 * Testing class for updateEnemyLocationMethod
 *
 * @author Daniel Luo
 */
public class testUpdateEnemyLocation {
    /**
     * Method prints string "board" format
     */
    public static String arrayToString(Integer[][] arr) {
        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < arr.length; i++) {
            System.out.print('[');
            sb.append('[');
            for (int j = 0; j < arr[i].length; j++) {
                System.out.print(arr[j][i] + "  ");
                sb.append(arr[j][i] + "  ");

            }
            System.out.print("]\n");
            sb.append("]\n");
        }
        System.out.println("");

        return sb.toString();

    }

    /**
     * main method to put testing methods in
     * @param args holds the main method arguments
     */
    public static void main(String[] args) {
        Integer[][] map = new Integer[10][10];

        for(int i = 0; i < 10; i++) {
            for(int j = 0; j < 10; j++) {
                map[i][j] = 0;
            }
        }

        // places trees at following locations
        map[2][6] = 1;
        map[2][8] = 1;
        map[4][2] = 1;
        map[6][7] = 1;
        map[6][8] = 1;
        map[7][0] = 1;
        map[9][2] = 1;
        map[9][4] = 1;
        map[9][6] = 1;

        Integer[][] updatedMap = new Integer[10][10];
        for(int i = 0; i < 10; i++) {
            for(int j = 0; j < 10; j++) {
                updatedMap[i][j] = map[i][j];
            }
        }

        updatedMap[0][1] = 2;

        // creates MapLocation of an enemy
        AstarAgent.MapLocation enemyLoc = new AstarAgent.MapLocation(0,1,null,0);

        AstarAgent.GameMap gameMap = new AstarAgent.GameMap(10,10,enemyLoc,null,new AstarAgent.MapLocation(0,0,null,0));

        gameMap.setGeneratedMap(map);
        gameMap.updateEnemyLocation(enemyLoc);

        Integer[][] a = gameMap.getGeneratedMap();

        if(testUpdateEnemyLocation.arrayToString(updatedMap).equals(testUpdateEnemyLocation.arrayToString(a)))
            System.out.println("TRUE TEST PASSED");
        else
            System.out.println("FALSE TEST FAILED");
    }

}
