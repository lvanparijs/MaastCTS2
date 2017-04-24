package MaastCTS2.selection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;

import MaastCTS2.Agent;
import MaastCTS2.Globals;
import MaastCTS2.controller.MctsController;
import MaastCTS2.model.ActionLocation;
import MaastCTS2.model.MctNode;
import MaastCTS2.model.MctNodeLucas;
import MaastCTS2.model.StateObs;
import MaastCTS2.selection.ISelectionStrategy;
import core.game.StateObservationMulti;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

/**
 * Standard UCT selection strategy
 *
 * @author Lucas Vanparijs
 *
 */
public class UCTSelectionMulti implements ISelectionStrategy {

    /** The exploration constant */
    private double c;

    public UCTSelectionMulti(double c) {
        this.c = c;
    }

    @Override
    public MctNode select(MctNode rootNode, ElapsedCpuTimer timer) {
        MctNode node = rootNode;
        StateObservationMulti state = rootNode.getStateObs();

        HashMap<Integer, Integer> previousResources = state.getAvatarResources(Agent.myID);
        HashMap<Integer, Integer> nextResources;

        StateObs stateObs = new StateObs(state, true);
        //Get available actions of other Agent stateObs.getStateObs().getAvailableActions(Agent.otherID)
        //Pick the best one for the other agent
        //advance the state
        //TODO: Check if this is a valid way to do this
        Types.ACTIONS[] next = new Types.ACTIONS[2];
        next[Agent.myID] = Types.ACTIONS.ACTION_LEFT;
        next[Agent.otherID] = getOpponentAction(node);
        stateObs.getStateObs().advance(next);

        // use uct to select child
        while (!state.isGameOver() && node.isFullyExpanded() && !node.getChildren().isEmpty()) {
            double previousScore = state.getGameScore(Agent.myID);
            int previousNumEvents = state.getEventsHistory().size();
            Vector2d previousAvatarPos = state.getAvatarPosition(Agent.myID);
            Vector2d previousAvatarOrientation = state.getAvatarOrientation(Agent.myID);

            node = getNextNodeByUct(node);

            stateObs = node.generateNewStateObs(stateObs, node.getActionFromParent());
            state = stateObs.getStateObsNoCopy();

            nextResources = state.getAvatarResources(Agent.myID);
            Globals.knowledgeBase.addEventKnowledge(previousScore, previousNumEvents, previousAvatarPos,
                    previousAvatarOrientation, node.getActionFromParent(), state,
                    previousResources, nextResources, false);
            previousResources = nextResources;
        }

        return node;
    }

    private Types.ACTIONS getOpponentAction(MctNode node){

        MctsController controller = (MctsController) Agent.controller;
        final double MIN_SCORE = controller.MIN_SCORE;
        final double MAX_SCORE = controller.MAX_SCORE;

        double n = node.getNumVisits();
        double log_n = Math.max(0.0, Math.log(n));


        EnumMap<Types.ACTIONS, Double> p2ActionScore = node.otherPlayerActionScores;
        EnumMap<Types.ACTIONS, Double> p2NumVisits = node.otherPlayerNumVisits;

        Types.ACTIONS bestAction = null;
        double bestVal = Double.NEGATIVE_INFINITY;

        for (Types.ACTIONS a: p2ActionScore.keySet()) {

            double n_i = Math.max(p2NumVisits.get(a), 0.00001);
            double avgScore = p2ActionScore.get(a) / n_i;
            avgScore = Globals.normalise(avgScore, MIN_SCORE, MAX_SCORE);

            double uctVal = avgScore + c * Math.sqrt(log_n / n_i);
            uctVal += Globals.smallNoise();

            double curVal = p2ActionScore.get(a);

            if (uctVal > bestVal) {
                bestVal = uctVal;
                bestAction = a;
            }
        }

        return bestAction;
    }

    private MctNode getNextNodeByUct(MctNodeLucas node) {
        //TODO: use the internal tree structure of the MctNodeLucas to determine the next node by UCT
        MctsController controller = (MctsController) Agent.controller;
        final double MIN_SCORE = controller.MIN_SCORE;
        final double MAX_SCORE = controller.MAX_SCORE;



        double n = node.getNumVisits();
        double log_n = Math.max(0.0, Math.log(n));

        ArrayList<MctNode> children = node.getChildren(); //get children of other player then all the children of those, pick the best
        int numChildren = children.size();

        // initialize best node as the first child
        MctNode bestNode = null;
        double bestUctVal = Double.NEGATIVE_INFINITY;

        for(int i = 0; i < numChildren; ++i){
            MctNode child = children.get(i);
            double n_i = Math.max(child.getNumVisits(), 0.00001);

            double avgScore = child.getTotalScore() / n_i;
            avgScore = Globals.normalise(avgScore, MIN_SCORE, MAX_SCORE);

            double uctVal = avgScore + c * Math.sqrt(log_n / n_i);
            uctVal += Globals.smallNoise();

            if (uctVal > bestUctVal) {
                bestUctVal = uctVal;
                bestNode = child;
            }
        }

        return bestNode;
    }

    @Override
    public int getDesiredActionNGramSize(){
        return -1;
    }

    @Override
    public String getName() {
        return "UCTSelectionMulti";
    }

    @Override
    public String getConfigDataString() {
        return "c=" + c;
    }

    @Override
    public void init(StateObservationMulti so, ElapsedCpuTimer elapsedTimer) {
    }

    @Override
    public boolean wantsActionStatistics(){
        return false;
    }
}
