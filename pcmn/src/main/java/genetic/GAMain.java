package genetic;

import entrants.ghosts.username.FleeingGhost;
import pacman.Executor;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.game.Constants;
import pacman.game.util.Stats;

import java.util.EnumMap;

public class GAMain {

    public static void main(String... args) {
        Executor executor = new Executor.Builder()
                .setTickLimit(4000)
                .build();

        EnumMap<Constants.GHOST, IndividualGhostController> controllers = new EnumMap<>(Constants.GHOST.class);

        controllers.put(Constants.GHOST.INKY, new GAGhost(Constants.GHOST.INKY));
        controllers.put(Constants.GHOST.BLINKY, new GAGhost(Constants.GHOST.BLINKY));
        controllers.put(Constants.GHOST.PINKY, new GAGhost(Constants.GHOST.PINKY));
        controllers.put(Constants.GHOST.SUE, new GAGhost(Constants.GHOST.SUE));

        Stats[] stats = executor.runExperiment(new examples.StarterPacMan.MyPacMan(), new MASController(controllers), 100, "GA");
        for (Stats s : stats) {
            System.out.println(s);
        }
    }
}
