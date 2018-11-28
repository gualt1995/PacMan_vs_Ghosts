import entrants.ghosts.username.FleeingGhost;
import pacman.Executor;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.game.Constants.GHOST;
import pacman.game.util.Stats;

import java.util.EnumMap;

public class Main {

    public static void main(String[] args) {

        Executor executor = new Executor.Builder()
                .setVisual(true)
                .setTickLimit(4000)
                .build();

        EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);

        controllers.put(GHOST.INKY, new FleeingGhost(GHOST.INKY,200,20));
        controllers.put(GHOST.BLINKY, new FleeingGhost(GHOST.BLINKY,200,20));
        controllers.put(GHOST.PINKY, new FleeingGhost(GHOST.PINKY,200,20));
        controllers.put(GHOST.SUE, new FleeingGhost(GHOST.SUE,200,20));

        //executor.runGameTimed(new examples.StarterPacMan.MyPacMan(), new MASController(controllers));
        Stats[] stats = executor.runExperiment(new examples.StarterPacMan.MyPacMan(), new MASController(controllers), 100, "PO");
        for (Stats s : stats) {
            System.out.println(s);
        }
    }
}
