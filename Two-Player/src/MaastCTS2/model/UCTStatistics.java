package MaastCTS2.model;

/**
 * Created by I6091912 on 24-4-2017.
 */
public class UCTStatistics {

    public double s;
    public double v;

    public UCTStatistics[] children;


    public UCTStatistics(){
        this.s = 0;
        this.v = 0;
    }

    public UCTStatistics(double s, double v){
        this.s = s;
        this.v = v;
    }

    public void update(double score){

    }

}
