package MaastCTS2.model;

import ontology.Types;

/**
 * Created by I6091912 on 24-4-2017.
 */
public class UCTStatistics {

    public double s;
    public double v;

    public UCTStatistics[] children;

    public Types.ACTIONS action;//The associated action with this statistic

    public UCTStatistics(Types.ACTIONS action){
        this.action = action;
        this.s = 0;
        this.v = 0;
    }

    public UCTStatistics(double s, double v, Types.ACTIONS action){
        this.action = action;
        this.s = s;
        this.v = v;
    }

    public void update(double score){

    }

}
