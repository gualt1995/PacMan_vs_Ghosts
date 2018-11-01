
import entrants.ghosts.username.*;


import examples.StarterPacManOneJunction.MyPacMan;
import pacman.Executor;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.controllers.examples.StarterPacMan;
import pacman.game.Constants.*;

import java.util.EnumMap;


/**
 * Created by pwillic on 06/05/2016.
 */
public class Main {

    public static void main(String[] args) {

        Executor executor = new Executor.Builder()
                .setVisual(true)
                .setTickLimit(4000)
                .build();

        EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);

        controllers.put(GHOST.INKY, new FleeingGhost(GHOST.INKY,200));
        controllers.put(GHOST.BLINKY, new FleeingGhost(GHOST.BLINKY,200));
        controllers.put(GHOST.PINKY, new FleeingGhost(GHOST.PINKY,200));
        controllers.put(GHOST.SUE, new FleeingGhost(GHOST.SUE,200));

        executor.runGameTimed(new examples.StarterPacMan.MyPacMan(), new MASController(controllers));
    }
}
