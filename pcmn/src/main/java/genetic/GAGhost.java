package genetic;

import io.jenetics.Chromosome;
import io.jenetics.IntegerGene;
import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.comms.BasicMessage;
import pacman.game.comms.Message;
import pacman.game.comms.Messenger;

public class GAGhost extends IndividualGhostController {

    private final static int PILL_PROXIMITY = 15;
    private final static int TICK_THRESHOLD = 10;
    private final static Object lock = new Object();
    private int tickSeen;
    private int lastPacmanIndex;
    private Chromosome<IntegerGene> ch;

    GAGhost(Constants.GHOST ghost) {
        super(ghost);
        lastPacmanIndex = -1;
        tickSeen = 0;
    }

    @Override
    public Constants.MOVE getMove(Game game, long l) {
        // Can we see PacMan? If so tell people and update our info
        int currentTick = game.getCurrentLevelTime();
        int pacmanIndex = game.getPacmanCurrentNodeIndex();
        int currentIndex = game.getGhostCurrentNodeIndex(ghost);
        Messenger messenger = game.getMessenger();

        if (pacmanIndex != -1) {
            lastPacmanIndex = pacmanIndex;
            tickSeen = game.getCurrentLevelTime();
            if (messenger != null) {
                messenger.addMessage(new BasicMessage(ghost,
                        null, BasicMessage.MessageType.PACMAN_SEEN, pacmanIndex, game.getCurrentLevelTime()));
            }
        }

        // Has anybody else seen PacMan if we haven't?
        if (pacmanIndex == -1 && game.getMessenger() != null) {
            for (Message message : messenger.getMessages(ghost)) {
                if (message.getType() == BasicMessage.MessageType.PACMAN_SEEN) {
                    if (message.getTick() > tickSeen && message.getTick() < currentTick) { // Only if it is newer information
                        lastPacmanIndex = message.getData();
                        tickSeen = message.getTick();
                    }
                }
            }
        }
        try {
        int state = getState(game);
        switch(ch.getGene(state).getAllele()) {
            case 1:
                // Move towards pacman
                synchronized (lock) {
                    if (lastPacmanIndex != -1) {
                        return game.getApproximateNextMoveTowardsTarget(currentIndex,
                                lastPacmanIndex, game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
                    }
                }
                break;
            case 2:
                // Move away from pacman
                synchronized (lock) {
                    if (lastPacmanIndex != -1) {
                        return game.getApproximateNextMoveAwayFromTarget(currentIndex,
                                lastPacmanIndex, game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
                    }
                }
                break;
            case 3:
                // Move towards closest powerpill.
                synchronized (lock) {
                    Integer closestPillIndex = getClosestPowerPillIndex(game);
                    if (closestPillIndex != null) {
                        return game.getApproximateNextMoveAwayFromTarget(currentIndex,
                                closestPillIndex, game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
                    }
                }
                break;
        }} catch (Exception e) {
            e.printStackTrace();
        }

        return Constants.MOVE.NEUTRAL;
    }

    private int getState(Game game) {
        int inDanger = this.inDanger(game) ? 1 : 0;
        int pacmanIndexKnown = lastPacmanIndex != -1 ? 1 : 0;
        int closeToPower = closeToPower(game) ? 1 : 0;
        int pacmanSeen = game.getPacmanCurrentNodeIndex() != -1 ? 1 : 0;

        return inDanger + (int)Math.pow(pacmanIndexKnown, 2)
                + (int)Math.pow(closeToPower, 3) + (int)Math.pow(pacmanSeen, 4);
    }

    private Boolean inDanger(Game game){
        if(game.isGhostEdible(ghost)){
            return true;
        }
        synchronized (lock) {
            if (lastPacmanIndex == -1) return false;
            int[] powerPills = game.getActivePowerPillsIndices();
            for (int powerPill : powerPills) {
                try {
                    if (game.getShortestPathDistance(powerPill, lastPacmanIndex) < PILL_PROXIMITY) {
                        return true;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private Boolean closeToPower(Game game) {
        int[] powerPills = game.getActivePowerPillsIndices();

        for (int i = 0; i < powerPills.length; i++) {
            int idx = game.getGhostCurrentNodeIndex(ghost);
            int g = game.getShortestPathDistance(idx, powerPills[i]);
            if (g < PILL_PROXIMITY) {
                return true;
            }
        }

        return false;
    }

    private Integer getClosestPowerPillIndex(Game game) {
        int [] powerPills = game.getActivePowerPillsIndices();
        int minDist = Integer.MAX_VALUE;
        Integer closestPill = null;
        for (int idx : powerPills) {
            int dist = game.getShortestPathDistance(idx, game.getGhostCurrentNodeIndex(ghost));
            if (dist < minDist) {
                minDist = dist;
                closestPill = idx;
            }
        }
        return closestPill;
    }

    void setChromosome(Chromosome<IntegerGene> _ch) {
        ch = _ch;
    }

    private void flushMemory(int currentTick){
        if (currentTick <= 2 || currentTick - tickSeen >= TICK_THRESHOLD) {
            lastPacmanIndex = -1;
            tickSeen = -1;
        }
    }
}
