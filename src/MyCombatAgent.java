import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.*;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.UnitTemplate.UnitTemplateView;

public class MyCombatAgent extends Agent{

    private int enemyPlayerNum = 1;
    private int lastTarget = 0;

    public MyCombatAgent(int playernum, String[] otherargs) {
        super(playernum);

        if(otherargs.length > 0)
        {
            enemyPlayerNum = new Integer(otherargs[0]);
        }

        System.out.println("Constructed MyCombatAgent");
    }

    @Override
    public Map<Integer, Action> initialStep(StateView newstate,
                                            HistoryView statehistory) {
        // This stores the action that each unit will perform
        // if there are no changes to the current actions then this
        // map will be empty
        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        // This is a list of all of your units
        // Refer to the resource agent example for ways of
        // differentiating between different unit types based on
        // the list of IDs
        List<Integer> myUnitIDs = newstate.getUnitIds(playernum);

        // This is a list of enemy units
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

        System.out.println(enemyUnitIDs);
        if(enemyUnitIDs.size() == 0)
        {
            // Nothing to do because there is no one left to attack
            return actions;
        }


        // start by commanding every single unit to attack an enemy unit
        for(Integer myUnitID : myUnitIDs) {
            // Command all of my units to attack the first enemy unit in the list
            actions.put(myUnitID, Action.createCompoundAttack(myUnitID, enemyUnitIDs.get(0)));
        }


        return actions;
    }

    @Override
    public Map<Integer, Action> middleStep(StateView newstate, HistoryView statehistory) {
        // This stores the action that each unit will perform
        // if there are no changes to the current actions then this
        // map will be empty
        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        // This is a list of all your units
        List<Integer> myUnitIDs = newstate.getUnitIds(playernum);

        // This is a list of enemy units
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);



        if(enemyUnitIDs.size() == 0)
        {
            // Nothing to do because there is no one left to attack
            return actions;
        }

        int currentStep = newstate.getTurnNumber();


        // go through the action history
        for(ActionResult feedback : statehistory.getCommandFeedback(playernum, currentStep-1).values())
        {
            // if the previous action is no longer in progress (either due to failure or completion)
            // then add a new action for this unit
            if(feedback.getFeedback() != ActionFeedback.INCOMPLETE)
            {
                // attack the first enemy unit in the list
                int unitID = feedback.getAction().getUnitId();

                UnitView unitView = newstate.getUnit(unitID);
                if(unitView != null) {
                    // set boolean that tells whether or not an enemy unit is adjacent or not
                    boolean inRange = false;
                    int unitIDToAttack = lastTarget;

                    // check if last target is adjacent to anyone else to team up on them
                    if (canAttack(newstate, unitID, lastTarget)) {
                        inRange = true;
                    }

                    // for each unit, check if enemy is in range and if so, attack it.
                    for (int id = 0; id < enemyUnitIDs.size() && !inRange; id++) {
                        // if we find an enemy unit near our unit, we will record the unitID and set inRange to true
                        if (canAttack(newstate, unitID, id)) {
                            inRange = true;
                            unitIDToAttack = id;
                        }
                    }

                    // if there is an enemy unit in range, we will attack it
                    if (inRange) {
                        actions.put(unitID, Action.createPrimitiveAttack(unitID, enemyUnitIDs.get(unitIDToAttack)));
                    }

                    else {
                        // attack the first enemy unit in the list
                        actions.put(unitID, Action.createCompoundAttack(unitID, enemyUnitIDs.get(0)));
                    }

                }
            }

            else {
                // attack the first enemy unit in the list
                int unitID = feedback.getAction().getUnitId();
                actions.put(unitID, Action.createCompoundAttack(unitID, enemyUnitIDs.get(0)));
            }
        }

        return actions;
    }

    @Override
    public void terminalStep(StateView newstate, HistoryView statehistory) {
        System.out.println("Finished the episode");
    }

    @Override
    public void savePlayerData(OutputStream os) {
        // TODO Auto-generated method stub

    }

    @Override
    public void loadPlayerData(InputStream is) {
        // TODO Auto-generated method stub

    }

    /**
     *
     * @param currentState the current state
     * @param unitID1 id of first unit
     * @param unitID2 id of second unit
     * @return whether or not unit1 can attack unit2
     */
    public boolean canAttack(StateView currentState, int unitID1, int unitID2) {
        if(currentState != null) {



            // get the unit views of the IDs
            UnitView unitView1 = currentState.getUnit(unitID1);
            UnitView unitView2 = currentState.getUnit(unitID2);
            UnitTemplateView templateView1 = null;
            UnitTemplateView templateView2 = null;

            if(unitView1 != null && unitView2 != null) {
                //get unit template for first ID to check if in range of attack on second ID
                templateView1 = unitView1.getTemplateView();
                templateView2 = unitView2.getTemplateView();
            }


            if(unitView1 != null && unitView2 != null && templateView1 != null && templateView2 != null) {
                // get coordinates of unit1 and unit2
                int x1 = unitView1.getXPosition();
                int y1 = unitView1.getYPosition();
                int x2 = unitView2.getXPosition();
                int y2 = unitView2.getYPosition();

                // if difference between x and y coordinates is <= 1, then the squares and units are adjacent
                if (Math.abs(x1 - x2) + Math.abs(y1 - y2) <= templateView1.getRange()) {
                    System.out.println("In range");
                    return true;
                }

                else {
                    System.out.println("Not in range");
                    return false;
                }
            }
        }

        return false;

    }


}
