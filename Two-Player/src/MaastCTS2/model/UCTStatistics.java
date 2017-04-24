package MaastCTS2.model;

import ontology.Types;

/**
 * Created by I6091912 on 24-4-2017.
 */
public class UCTStatistics {

    public double s;
    public double v;
    
    public boolean terminal

    public UCTStatistics[] children;

    public Types.ACTIONS action;//The associated action with this statistic

    public UCTStatistics(Types.ACTIONS action, boolean terminal){
        this(0,0,action,terminal);
    }

    public UCTStatistics(double s, double v, Types.ACTIONS action, boolean terminal){
        this.action = action;
        this.s = s;
        this.v = v;
        this.terminal = terminal;
        
        if(!terminal) {
            //Creates children if this is not terminal, since I only need 2 layers i can pass in !terminal in the next constructor  
            this.children = new UCTStatistics[Types.ACTIONS.values().length];
            int curIndex = 0;
            for (Types.ACTIONS a : Types.ACTIONS.values()) {
                children[curIndex] = new UCTStatistics(a,!terminal);
            }
        }
    }

    public void update(double score){

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
