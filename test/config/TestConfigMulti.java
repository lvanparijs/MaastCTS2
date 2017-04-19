package test.config;

import core.game.StateObservation;
import core.game.StateObservationMulti;
import core.player.AbstractMultiPlayer;
import core.player.AbstractPlayer;

/**
 * Created by i6091912 on 19-4-2017.
 */
public abstract class TestConfigMulti {
    public abstract AbstractMultiPlayer createAgent(String actionFile,
                                                    StateObservationMulti stateObs, int randomSeed);

    public abstract String getName();

    public String getAdditionalLogData(char c) {
        return "";
    }
}
