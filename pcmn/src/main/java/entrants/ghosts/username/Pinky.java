package entrants.ghosts.username;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.comms.BasicMessage;
import pacman.game.comms.Message;
import pacman.game.comms.Messenger;
import pacman.game.internal.Maze;
import pacman.game.internal.Node;

import java.util.*;

/**
 * Created by Piers on 11/11/2015.
 */
public class Pinky extends IndividualGhostController {
    private final static float CONSISTENCY = 0.9f;    //attack Ms Pac-Man with this probability
    private final static int PILL_PROXIMITY = 15;        //if Ms Pac-Man is this close to a power pill, back away
    Random rnd = new Random();
    private int TICK_THRESHOLD;
    private int lastPacmanIndex = -1;
    private int tickSeen = -1;
    private Logger logger;
    private int MasterIs;

    public Pinky( int TICK_THRESHOLD) {
        super(Constants.GHOST.PINKY);
        logger = LoggerFactory.getLogger(FleeingGhost.class);
        System.out.println("Pinky Started");
        this.TICK_THRESHOLD = TICK_THRESHOLD;
    }


    @Override
    public Constants.MOVE getMove(Game game, long timeDue) {
        // Housekeeping - throw out old info
        int currentTick = game.getCurrentLevelTime();


        // Can we see PacMan? If so tell people and update our info
        int pacmanIndex = game.getPacmanCurrentNodeIndex();
        int currentIndex = game.getGhostCurrentNodeIndex(ghost);
        Messenger messenger = game.getMessenger();
        if (pacmanIndex != -1) {
            lastPacmanIndex = pacmanIndex;
            tickSeen = game.getCurrentLevelTime();
            if (messenger != null) {
                messenger.addMessage(new BasicMessage(ghost, null, BasicMessage.MessageType.PACMAN_SEEN, pacmanIndex, game.getCurrentLevelTime()));
            }
        }

        // Has anybody else seen PacMan if we haven't?
        if (pacmanIndex == -1 && game.getMessenger() != null) {
            for (Message message : messenger.getMessages(ghost)) {
                if (message.getType() == BasicMessage.MessageType.PACMAN_SEEN) {
                    if (message.getTick() > tickSeen && message.getTick() < currentTick) { // Only if it is newer information
                        if(message.getRecipient() == Constants.GHOST.SUE){
                            lastPacmanIndex = message.getData();
                            tickSeen = message.getTick();
                        }
                    }
                }if (message.getType() == BasicMessage.MessageType.I_AM){
                    MasterIs = message.getData();
                }
            }
        }
        if (pacmanIndex == -1) {
            pacmanIndex = lastPacmanIndex;
        }

        Boolean requiresAction = game.doesGhostRequireAction(ghost);
        if (requiresAction != null && requiresAction)        //if ghost requires an action
        {
            if (pacmanIndex != -1) {
                if (game.getGhostEdibleTime(ghost) > 0 || closeToPower(game))    //retreat from Ms Pac-Man if edible or if Ms Pac-Man is close to power pill
                {
                    try {
                        return game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),
                                game.getPacmanCurrentNodeIndex(), game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println(e);
                        System.out.println(pacmanIndex + " : " + currentIndex);
                    }
                } else {
                    try {

                        Maze m = game.getCurrentMaze();
                        Node[] nodes = m.graph;
                        nodes = nodes.clone();
                        List<Node> tmp = Arrays.asList(nodes);
                        LinkedList<Node> tmp2 = new LinkedList<>(tmp);
                        //System.out.println( "removing node" + tmp2.get(MasterIs).nodeIndex+ " == ? "+ MasterIs);
                        tmp2.remove(tmp2.get(MasterIs));
                        tmp2.toArray(nodes);

                        Constants.MOVE move = null;
                        double minDistance = Integer.MAX_VALUE;
                        try{
                            for (Map.Entry<Constants.MOVE, Integer> entry : nodes[game.getGhostCurrentNodeIndex(ghost)].allNeighbourhoods.get(game.getGhostLastMoveMade(ghost)).entrySet()) {
                                double distance = game.getDistance(entry.getValue(), pacmanIndex,  Constants.DM.PATH);

                                if (distance < minDistance) {
                                    minDistance = distance;
                                    move = entry.getKey();
                                }
                            }
                            //TODO implemente la memoire peu etre ?
                            lastPacmanIndex = -1;
                            tickSeen = -1;
                            return move;
                        }catch (NullPointerException e){
                            System.out.println(e);
                            Constants.MOVE[] possibleMoves = game.getPossibleMoves(game.getGhostCurrentNodeIndex(ghost), game.getGhostLastMoveMade(ghost));
                            return possibleMoves[rnd.nextInt(possibleMoves.length)];
                        }


                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println(e);
                        System.out.println(pacmanIndex + " : " + currentIndex);
                    }

                }
            } else {
                Constants.MOVE[] possibleMoves = game.getPossibleMoves(game.getGhostCurrentNodeIndex(ghost), game.getGhostLastMoveMade(ghost));
                return possibleMoves[rnd.nextInt(possibleMoves.length)];
            }
        }
        return null;
    }

    //This helper function checks if Ms Pac-Man is close to an available power pill
    private boolean closeToPower(Game game) {
        int[] powerPills = game.getPowerPillIndices();

        for (int i = 0; i < powerPills.length; i++) {
            Boolean powerPillStillAvailable = game.isPowerPillStillAvailable(i);
            int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
            if (pacmanNodeIndex == -1) {
                pacmanNodeIndex = lastPacmanIndex;
            }
            if (powerPillStillAvailable == null || pacmanNodeIndex == -1) {
                return false;
            }
            if (powerPillStillAvailable && game.getShortestPathDistance(powerPills[i], pacmanNodeIndex) < PILL_PROXIMITY) {
                return true;
            }
        }

        return false;
    }
}

