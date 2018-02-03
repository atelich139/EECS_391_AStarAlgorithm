import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History.HistoryView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

public class MyResourceCollectionAgent extends Agent {

    public MyResourceCollectionAgent(int playernum) {
        super(playernum);
        // TODO Auto-generated constructor stub
    }

    @Override
    public Map<Integer, Action> initialStep(StateView newstate, HistoryView statehistory) {
        return null;
    }

    @Override
    public void loadPlayerData(InputStream arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<Integer, Action> middleStep(StateView newstate, HistoryView statehistory) {
        Map<Integer, Action> actions = new HashMap<Integer, Action>();
        // get all the units
        //
        List<Integer> myUnitIds = newstate.getUnitIds(playernum);
        List<Integer> peasantIds = new ArrayList<Integer>();
        List<Integer> townhallIds = new ArrayList<Integer>();
        List<Integer> farmIds = new ArrayList<Integer>();
        for (Integer unitID : myUnitIds) {
            UnitView unit = newstate.getUnit(unitID);
            String unitTypeName = unit.getTemplateView().getName();
            if (unitTypeName.equals("TownHall"))
                townhallIds.add(unitID);
            else if (unitTypeName.equals("Peasant"))
                peasantIds.add(unitID);
            else if (unitTypeName.equals("Farm"))
                farmIds.add(unitID);
            else
                System.err.println("Unexpected Unit type: " + unitTypeName);
        }

        // get all the resource nodes and information
        List<Integer> goldMines = newstate.getResourceNodeIds(Type.GOLD_MINE);
        List<Integer> trees = newstate.getResourceNodeIds(Type.TREE);
        int currentGold = newstate.getResourceAmount(playernum, ResourceType.GOLD);
        int currentWood = newstate.getResourceAmount(playernum, ResourceType.WOOD);

        // get information about building farms
        TemplateView farmTemplate = newstate.getTemplate(playernum, "Farm");
        int farmGoldCost = farmTemplate.getGoldCost();
        int farmWoodCost = farmTemplate.getWoodCost();
        Integer farmTemplateID = farmTemplate.getID();

        // get information about building peasants
        TemplateView peasantTemplate = newstate.getTemplate(playernum, "Peasant");
        int peasantGoldCost = farmTemplate.getGoldCost();
        int peasantFoodCost = farmTemplate.getFoodCost();
        Integer peasantTemplateID = peasantTemplate.getID();

        // get your current food and your food supply cap
        int currentFood = newstate.getSupplyAmount(playernum);
        int currentFoodCap = newstate.getSupplyCap(playernum);

        // Looking at the feedback will tell us if the unit finished the task it
        // was working on.
        int currentStep = newstate.getTurnNumber();
        Map<Integer, ActionResult> results = statehistory.getCommandFeedback(playernum, currentStep - 1);

        /*
         * assign actions to the peasants. If the peasant is busy, assign
         * nothing. Otherwise, tell him to build a farm if food is maxed out and
         * he has enough money; If he does not have enough money, then check if
         * he is carrying resources. If he is, tell him to go desposit them.
         * Otherwise, tell him to go gather more resources.
         *
         * Bug - all the peasants wait when one of them is trying to build something. All the rest should continue mining.
         */
        for (Integer peasantID : peasantIds) {
            ActionResult peasantResult = results.get(peasantID);
            if (peasantResult != null && peasantResult.getFeedback() == ActionFeedback.INCOMPLETE) {
                continue;
            }
            if (currentFood >= currentFoodCap && currentGold > farmGoldCost && currentWood > farmWoodCost) {
                actions.put(peasantID, Action.createCompoundBuild(peasantID, farmTemplateID, 5, 5));
            } else if (newstate.getUnit(peasantID).getCargoAmount() > 0) {
                actions.put(peasantID, new TargetedAction(peasantID, ActionType.COMPOUNDDEPOSIT, townhallIds.get(0)));
            } else if (currentGold < currentWood) {
                actions.put(peasantID, new TargetedAction(peasantID, ActionType.COMPOUNDGATHER, goldMines.get(0)));
            } else if (currentGold >= currentWood) {
                actions.put(peasantID, new TargetedAction(peasantID, ActionType.COMPOUNDGATHER, trees.get(0)));
            }
        }

        // assign actions to the town hall.
        // if food is not maxed and townhall has money, build another peasant.
        for (Integer townhallID : townhallIds) {
            ActionResult townhallResult = results.get(townhallID);
            if (townhallResult != null && townhallResult.getFeedback() == ActionFeedback.INCOMPLETE) {
                continue;
            }

            if (currentFood < currentFoodCap && currentGold >= peasantGoldCost) {
                actions.put(townhallID, Action.createCompoundProduction(townhallID, peasantTemplateID));
            }
        }

        return actions;
    }

    @Override
    public void savePlayerData(OutputStream arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void terminalStep(StateView arg0, HistoryView arg1) {
        // TODO Auto-generated method stub

    }

}
