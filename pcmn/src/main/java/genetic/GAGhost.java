package genetic;

import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;

public class GAGhost extends IndividualGhostController {

    public GAGhost(Constants.GHOST ghost) {
        super(ghost);
    }

    @Override
    public Constants.MOVE getMove(Game game, long l) {
        return Constants.MOVE.NEUTRAL;
    }
}
