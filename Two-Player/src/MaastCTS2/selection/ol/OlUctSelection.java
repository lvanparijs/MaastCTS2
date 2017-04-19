package MaastCTS2.selection.ol;

import java.util.ArrayList;
import java.util.HashMap;

import MaastCTS2.Agent;
import MaastCTS2.Globals;
import MaastCTS2.controller.MctsController;
import MaastCTS2.model.MctNode;
import MaastCTS2.model.StateObs;
import MaastCTS2.selection.ISelectionStrategy;
import core.game.StateObservationMulti;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

/**
 * Standard (open-loop) UCT selection strategy
 * 
 * <p> NOTE: This class  has not been used for a long time, and may not include all the special cases
 * and/or heuristics that have been added to ProgressiveHistory in the meantime
 * 
 * @author Dennis Soemers
 *
 */
public class OlUctSelection implements ISelectionStrategy {

	/** The exploration constant */
	private double c;

	public OlUctSelection(double c) {
		this.c = c;
	}

	@Override
	public MctNode select(MctNode rootNode, ElapsedCpuTimer timer) {
		MctNode node = rootNode;
		StateObservationMulti state = rootNode.getStateObs();
		
		HashMap<Integer, Integer> previousResources = state.getAvatarResources(Agent.myID);
		HashMap<Integer, Integer> nextResources;
		
		StateObs stateObs = new StateObs(state, true);
		
		// use uct to select child
		while (!state.isGameOver() && node.isFullyExpanded() && !node.getChildren().isEmpty()) {
			double previousScore = state.getGameScore(Agent.myID);
			int previousNumEvents = state.getEventsHistory().size();
			Vector2d previousAvatarPos = state.getAvatarPosition(Agent.myID);
			Vector2d previousAvatarOrientation = state.getAvatarOrientation(Agent.myID);
			
			node = getNextNodeByUct(node);
			//Do the same for opponent, combine the two moves into the next state
			
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

	private MctNode getNextNodeByUct(MctNode node) {	
		MctsController controller = (MctsController) Agent.controller;
		final double MIN_SCORE = controller.MIN_SCORE;
		final double MAX_SCORE = controller.MAX_SCORE;
		
		double n = node.getNumVisits();
		double log_n = Math.max(0.0, Math.log(n));
		
		ArrayList<MctNode> children = node.getChildren();
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
		return "ol.UctSelection";
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
