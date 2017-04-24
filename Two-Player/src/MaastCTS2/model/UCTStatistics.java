package MaastCTS2.model;

import MaastCTS2.Agent;
import core.game.StateObservationMulti;
import ontology.Types;

/**
 * Created by I6091912 on 24-4-2017.
 */
public class UCTStatistics {

    public double s;
    public double v;
    
    public int terminal;

    int pId;

    public MctNodeLucas node;

    public StateObservationMulti state;

    public UCTStatistics[] children;

    public UCTStatistics parent;

    public Types.ACTIONS action;//The associated action with this statistic

    public UCTStatistics(MctNodeLucas node, Types.ACTIONS action, int terminal, int playerID, UCTStatistics parent){
        this(0,0,node , action,terminal, playerID, parent);
    }

    public UCTStatistics(double s, double v, MctNodeLucas node,  Types.ACTIONS action, int terminal, int playerID, UCTStatistics parent){
        this.action = action;
        this.s = s;
        this.v = v;
        this.terminal = terminal;
        this.node = node;
        this.state = node.getStateObs();
        this.parent = parent;

        if(playerID == 0){
            pId = 1;
        }else{
            pId = 0;
        }

        if(terminal >= 1) {
            //Creates children if this is not terminal, since I only need 2 layers i can pass in !terminal in the next constructor  
            this.children = new UCTStatistics[Types.ACTIONS.values().length];
            int curIndex = 0;
            for (Types.ACTIONS a : state.getAvailableActions(playerID)) {
                children[curIndex] = new UCTStatistics(node, a, terminal--, pId, this);
            }
        }else{
            //TODO: This should work according to exploration and stuff
            //TODO: If terminal get the score and propagate backwards
            //TODO: Update the scores when the nodes are altered
        }
    }

    public void update(){
        state = node.getStateObs();
    }
    
    public double getAvgScore(){
        double sumS = 0;
        double sumV = 0;
        if(children == null){
            System.out.println("Terminal Node reached");
        }else {
            for (UCTStatistics stat : children) {
                sumS += stat.s;
                sumV += stat.v;
            }
        }
        return sumS/sumV;
    }

}
