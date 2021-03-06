package MaastCTS2.model;

import MaastCTS2.Agent;
import MaastCTS2.Globals;
import MaastCTS2.controller.MctsController;
import core.game.StateObservationMulti;
import ontology.Types;

import java.util.ArrayList;
import java.util.EnumMap;

/**
 * Created by I6091912 on 24-4-2017.
 */
public class MctNodeLucas {
    /** The parent node of this node */
    private MctNodeLucas parent;
    /** The action that, when applied to the parent, results in this node */
    private Types.ACTIONS actionFromParent;

    /** The number of times this node has been visited in the MCTS algorithm (double instead of int to accomodate tree decay) */
    private double numVisits;
    /** The sum of all the scores that have been backpropagated through this node */
    private double totalScore;
    
    private UCTStatistics[] stats;
    private int numActions = Types.ACTIONS.values().length;
    public boolean optimistic = false;
    
    /** The depth of this node in the current MCTS tree */
    private int depth;

    /** List of actions that have not yet been expanded into child nodes */
    private ArrayList<Types.ACTIONS> unexpandedActions;
    /** List of child nodes of this node */
    private ArrayList<MctNodeLucas> children;
    /** Observation of the game state in this node. Can change during the search in Open Loop MCTS */
    private StateObservationMulti stateObs;

    private ArrayList<StateObservationMulti> cachedStateObservations = null;

    /** A cached state observation that can be used for closed-loop-style traversal of tree */
    private StateObservationMulti savedStateObs = null;

    /** An integer-representation of the cell in which the avatar was in the last state observation seen in this node */
    private int lastAvatarCell;

    public EnumMap<Types.ACTIONS, Double> otherPlayerActionScores;
    public EnumMap<Types.ACTIONS, Double> otherPlayerNumVisits;
    public Types.ACTIONS lastSimmedActionOtherPlayer;

    public MctNodeLucas() {
        this(null, Types.ACTIONS.ACTION_NIL);
    }

    public MctNodeLucas(MctNodeLucas parent, Types.ACTIONS action) {
        totalScore = 0.0;
        numVisits = 0.0;
        this.parent = parent;
        actionFromParent = action;
        children = new ArrayList<MctNodeLucas>(5);
        stateObs = null;

        if (parent != null) {
            depth = parent.getDepth() + 1;
        }
        else {
            depth = 0;
        }

        unexpandedActions = null;

        lastAvatarCell = -1;

        otherPlayerActionScores = new EnumMap<Types.ACTIONS, Double>(Types.ACTIONS.class);
        otherPlayerNumVisits = new EnumMap<Types.ACTIONS, Double>(Types.ACTIONS.class);
        lastSimmedActionOtherPlayer = Types.ACTIONS.ACTION_NIL;
        
        stats = new UCTStatistics[numActions];
        int curAction = 0;
        int playerId;

        if(optimistic){
            playerId = Agent.otherID;
        }else{
            playerId = Agent.myID;
        }

        for(Types.ACTIONS a : stateObs.getAvailableActions(playerId)){
            otherPlayerActionScores.put(a, 0.0);
            otherPlayerNumVisits.put(a, 0.0);
            stats[curAction] = new UCTStatistics(this , a, 2, playerId);
            curAction++;
        }
    }

    /**
     * Backpropagates the given score from a Monte-Carlo simulation through this node
     *
     * @param score
     */
    public void backpropagate(double score, double otherPlayerScore){
        backpropagate(score, otherPlayerScore, 1.0);
    }

    /**
     * Backpropagates the given score through this node with the given number of visits
     * <br> Can be used to pretend that a score was obtained through more or less than
     * exactly 1.0 simulation
     *
     * @param score
     * @param numVisits
     */
    public void backpropagate(double score, double otherPlayerScore, double numVisits){
        totalScore += score * numVisits;
        this.numVisits += numVisits;

        otherPlayerActionScores.put(lastSimmedActionOtherPlayer,
                otherPlayerActionScores.get(lastSimmedActionOtherPlayer) + (otherPlayerScore * numVisits));
        otherPlayerNumVisits.put(lastSimmedActionOtherPlayer, otherPlayerActionScores.get(lastSimmedActionOtherPlayer) + numVisits);
    }

    /**
     * Decays the results collected in this node by the given factor
     *
     * @param decayFactor
     */
    public void decay(double decayFactor){
        // decay visit count
        numVisits *= decayFactor;

        // now decay the total scores by the same amount
        totalScore *= decayFactor;
        for(Types.ACTIONS action : Types.ACTIONS.values()){
            otherPlayerActionScores.put(action, otherPlayerActionScores.get(action) * decayFactor);
            otherPlayerNumVisits.put(action, otherPlayerNumVisits.get(action) * decayFactor);
        }

        // depth should be lowered by 1 because our new root is 1 level lower than the previous root
        decrementDepth();

        // for nondeterministic game, get rid of old state that may be incorrect now
        savedStateObs = null;
    }

    public void decrementDepth(){
        --depth;
    }

    public void addChild(MctNodeLucas newChildNode){
        children.add(newChildNode);
    }

    public void cacheStateObservation(StateObservationMulti stateObs){
        if(cachedStateObservations == null){
            cachedStateObservations = new ArrayList<StateObservationMulti>(2);
        }

        cachedStateObservations.add(stateObs);
    }

    /**
     * Caches the given list of state observations. Every cached state observation will be used once
     * instead of generating a new state when traversing the MCTS tree.
     *
     * @param stateObservations
     */
    public void cacheStateObservations(ArrayList<StateObservationMulti> stateObservations){
        if(cachedStateObservations == null){
            cachedStateObservations = new ArrayList<StateObservationMulti>(stateObservations);
        }
        else{
            cachedStateObservations.addAll(stateObservations);
        }
    }

    /**
     * Generates and sets a new State Observation for this node.
     * <br> Calling this method will likely modify the ''previousState'' object
     *
     * <p> Returns the generated state (which will be the same ''previousState'' object
     * again if the method chose to modify it, or a different object otherwise).
     *
     * @param previousState
     * @param action
     * @return
     */
    public StateObs generateNewStateObs(StateObs previousState, Types.ACTIONS action){
        StateObs returnState;

        if(hasCachedState()){
            // we still have some unused cached state observations, so use one of them instead
            // of generating a new state
            setStateObs(getNextCachedState());

            // the previousState will no longer be used anywhere, so we can let our parent cache it
            if(!previousState.shouldCopy()){
                parent.cacheStateObservation(previousState.getStateObsNoCopy());
            }

            returnState = new StateObs(stateObs, false);
        }
        else if(savedStateObs != null){
            setStateObs(savedStateObs);
            returnState = new StateObs(savedStateObs, true);
        }
        else{
            StateObservationMulti nextState = previousState.getStateObs();
            //nextState.advance(Globals.generateActionArray(action, nextState, otherPlayerActionScores, otherPlayerNumVisits, numVisits, this));
            setStateObs(nextState);
            MctsController.NUM_ADVANCE_OPS += 1;

            returnState = new StateObs(nextState, false);
        }

        return returnState;
    }

    public Types.ACTIONS getActionFromParent() {
        return actionFromParent;
    }

    public ActionLocation getActionLocationFromParent(){
        return new ActionLocation(actionFromParent, getParent().getLastAvatarCell());
    }

    public ArrayList<MctNodeLucas> getChildren() {
        return children;
    }

    public int getDepth(){
        return depth;
    }

    public MctNodeLucas getExpandedChildForAction(Types.ACTIONS action){
        for(MctNodeLucas child : children){
            if(child.getActionFromParent() == action){
                return child;
            }
        }

        return null;
    }

    public int getLastAvatarCell(){
        return lastAvatarCell;
    }

    public StateObservationMulti getNextCachedState(){
        return cachedStateObservations.remove(cachedStateObservations.size() - 1);
    }

    public double getNumVisits(){
        return numVisits;
    }

    public MctNodeLucas getParent(){
        return parent;
    }

    public StateObservationMulti getSavedStateObs(){
        return savedStateObs;
    }

    public StateObservationMulti getStateObs(){
        return stateObs;
    }

    /**
     * Returns the sum of all scores backpropagated through this node
     *
     * @return
     */
    public double getTotalScore() {
        return totalScore;
    }

    /**
     * Returns the list of actions that have not yet been used to create a new node
     *
     * @return
     */
    public ArrayList<Types.ACTIONS> getUnexpandedActions(){
        return unexpandedActions;
    }

    public boolean hasCachedState(){
        return (cachedStateObservations != null && !cachedStateObservations.isEmpty());
    }

    /**
     * Returns true if and only if the node is fully expanded (meaning that all legal
     * actions in this node have been used to generate children)
     *
     * @return
     */
    public boolean isFullyExpanded(){
        return (unexpandedActions != null && unexpandedActions.isEmpty());
    }

    /**
     * This method is called after the backup step of MCTS went through this node.
     *
     * Currently used to clean up memory a little bit in some cases
     */
    public void postBackup(){
        stateObs = null;
    }

    public void removeCachedStates(){
        cachedStateObservations = null;
    }

    public void resetParent(){
        parent = null;
    }

    public void setChildren(ArrayList<MctNodeLucas> children){
        this.children.addAll(children);
    }

    public void setStateObs(StateObservationMulti stateObs) {
        this.stateObs = stateObs;
        lastAvatarCell = Globals.knowledgeBase.positionToInt(stateObs.getAvatarPosition(Agent.myID));

        if(unexpandedActions == null){		// we haven't generated the list of unexpanded actions or children for this node yet
            ArrayList<Types.ACTIONS> availableActions = stateObs.getAvailableActions(Agent.myID);
            if(availableActions.size() > 1){
                availableActions.remove(Types.ACTIONS.ACTION_NIL);
            }

            if(availableActions.size() > 0){
                unexpandedActions = new ArrayList<Types.ACTIONS>(availableActions);
            }
        }
    }
}
