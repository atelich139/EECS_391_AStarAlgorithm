import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class FirstClass extends Agent{
    
    public FirstClass(int i) {
        super(i);
    }
    
    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView,
                                            History.HistoryView historyView) {
        return null;
    }
    
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView,
                                           History.HistoryView historyView) {
        return null;
    }
    
    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {
    
    }
    
    @Override
    public void savePlayerData(OutputStream outputStream) {
    
    }
    
    @Override
    public void loadPlayerData(InputStream inputStream) {
    
    }
}
