package test.config;

import MaastCTS2.Agent;
import MaastCTS2.heuristics.states.IPlayoutEvaluation;
import MaastCTS2.move_selection.IMoveSelectionStrategy;
import MaastCTS2.playout.IPlayoutStrategy;
import MaastCTS2.selection.ISelectionStrategy;
import core.competition.CompetitionParameters;
import core.game.StateObservationMulti;
import core.player.AbstractMultiPlayer;
import tools.ElapsedCpuTimer;


/**
 * Created by i6091912 on 19-4-2017.
 */
public class LucasMCTSTestConfig extends TestConfigMulti {

    private ISelectionStrategy selectionStrategy;
    private IPlayoutStrategy playoutStrategy;
    private IMoveSelectionStrategy moveSelectionStrategy;
    private IPlayoutEvaluation playoutEval;
    private boolean initBreadthFirst;
    private boolean noveltyBasedPruning;
    private boolean exploreLosses;
    private boolean knowledgeBasedEval;
    private boolean detectDeterministicGames;
    private boolean treeReuse;
    private String name;
    private double treeReuseGamma;
    private int maxNumSafetyChecks;
    private boolean alwaysKB;
    private boolean noTreeReuseBFTI;

    private AbstractMultiPlayer player = null;

    public LucasMCTSTestConfig(String name, ISelectionStrategy selectionStrategy,
                               IPlayoutStrategy playoutStrategy, IMoveSelectionStrategy moveSelectionStrategy,
                               IPlayoutEvaluation playoutEval, boolean initBreadthFirst, boolean noveltyBasedPruning,
                               boolean exploreLosses, boolean knowledgeBasedEval, boolean detectDeterministicGames,
                               boolean treeReuse, double treeReuseGamma, int maxNumSafetyChecks, boolean alwaysKB, boolean noTreeReuseBFTI){
        this.name = name;
        this.selectionStrategy = selectionStrategy;
        this.playoutStrategy = playoutStrategy;
        this.moveSelectionStrategy = moveSelectionStrategy;
        this.playoutEval = playoutEval;
        this.initBreadthFirst = initBreadthFirst;
        this.noveltyBasedPruning = noveltyBasedPruning;
        this.exploreLosses = exploreLosses;
        this.knowledgeBasedEval = knowledgeBasedEval;
        this.detectDeterministicGames = detectDeterministicGames;
        this.treeReuse = treeReuse;
        this.treeReuseGamma = treeReuseGamma;
        this.maxNumSafetyChecks = maxNumSafetyChecks;
        this.alwaysKB = alwaysKB;
        this.noTreeReuseBFTI = noTreeReuseBFTI;
    }

    @Override
    public AbstractMultiPlayer createAgent(String actionFile, StateObservationMulti stateObs, int randomSeed) {
        try {

            // Determine the time due for the controller creation.
            ElapsedCpuTimer ect = new ElapsedCpuTimer(CompetitionParameters.TIMER_TYPE);
            ect.setMaxTimeMillis(CompetitionParameters.INITIALIZATION_TIME);
            /*
            player = new Agent(stateObs, ect.copy(), selectionStrategy, playoutStrategy,
                    moveSelectionStrategy, playoutEval, initBreadthFirst,
                    noveltyBasedPruning, exploreLosses, knowledgeBasedEval,
                    detectDeterministicGames, treeReuse, treeReuseGamma,
                    maxNumSafetyChecks, alwaysKB, noTreeReuseBFTI);
            */
            // Check if we returned on time, and act in consequence.
            long timeTaken = ect.elapsedMillis();
            if (ect.exceededMaxTime()) {
                long exceeded = -ect.remainingTimeMillis();
                System.out.println("Controller initialization time out ("
                        + exceeded + ").");

                return null;
            }
            else {
                System.out.println("Controller initialization time: "
                        + timeTaken + " ms.");
            }

            // This code can throw many exceptions (no time related):

            if (player != null){
                player.setup(actionFile, randomSeed, false);
            }
        }
        catch (Exception e) {
            // This probably happens because controller took too much time to be
            // created.
            e.printStackTrace();
            System.exit(1);
        }

        return player;
    }

    @Override
    public String getName() {
        return null;
    }
}
