package entrants.ghosts.username;



import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.comms.BasicMessage;
import pacman.game.comms.Message;
import pacman.game.comms.Messenger;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pacman.game.internal.Maze;
import pacman.game.internal.Node;


/**
 * Created by pwillic on 25/02/2016.
 */

public class FleeingGhost extends IndividualGhostController {
    public enum State {
        SEARCH, HUNT, FLEE, LEAD
    };
    private final static int PILL_PROXIMITY = 15;        //if Ms Pac-Man is this close to a power pill, back away
    private Random rnd = new Random();
    private int TICK_THRESHOLD;
    private int lastPacmanIndex = -1;
    private int tickSeen = -1;
    private Logger logger;
    private int MasterIs = -1;
    private State state = State.SEARCH;


    /*public FleeingGhost(Constants.GHOST ghost) {
        this(ghost, 5);
    }*/
    public FleeingGhost(Constants.GHOST ghost, int TICK_THRESHOLD) {
        super(ghost);
        logger = LoggerFactory.getLogger(FleeingGhost.class);
        this.TICK_THRESHOLD = TICK_THRESHOLD;
    }

    @Override
    public Constants.MOVE getMove(Game game, long timeDue) {
        int currentTick = game.getCurrentLevelTime();
        boolean reqAction = game.doesGhostRequireAction(ghost);;
        Messenger messenger = null;
        if (game.getMessenger() != null) {
             messenger = game.getMessenger();
        }

        flushMemory(currentTick);
        int currentIndex = game.getGhostCurrentNodeIndex(ghost);;
        int pacmanIndex = game.getPacmanCurrentNodeIndex();
        if (pacmanIndex != -1) {
            //System.out.println(ghost.name() + "sees the pacman");
            lastPacmanIndex = pacmanIndex;
            tickSeen = game.getCurrentLevelTime();
            if (messenger != null) {
                messenger.addMessage(new BasicMessage(ghost,
                        null, BasicMessage.MessageType.PACMAN_SEEN, pacmanIndex, game.getCurrentLevelTime()));
            }
        }
        if(messenger != null){
            for (Message message : messenger.getMessages(ghost)) {
                if (message.getType() == BasicMessage.MessageType.PACMAN_SEEN) {
                    if (message.getTick() > tickSeen && message.getTick() < currentTick) { // Only if it is newer information
                        lastPacmanIndex = message.getData();
                        tickSeen = message.getTick();
                    }
                }
            }
        }
        //TODO update so that a leader stays until lastpcindx = 0
        if(lastPacmanIndex != -1){
            if(pacmanIndex != -1){
                state = State.LEAD;
            }else {
                state = State.HUNT;
            }
        }
        if(InDanger(game)){
            state = State.FLEE;
        }
        switch (state) {
            case SEARCH:  return searchBehaviour(game,reqAction);
            case HUNT:  return huntBehaviour(game,messenger,lastPacmanIndex,reqAction);
            case LEAD:  return leadBehaviour(game,messenger,currentIndex,pacmanIndex,reqAction);
            default: return  fleeBehaviour(game,reqAction);
        }

    }
    private Constants.MOVE searchBehaviour(Game game, boolean reqAction){
        MasterIs = -1;
        if(reqAction){
            return splitUp(game, game.getPowerPillIndices());
        }else{
            return null;
        }
    }
    private Constants.MOVE huntBehaviour(Game game, Messenger messenger,int lastPacmanIndex,boolean reqAction){
        if(messenger != null){
            for (Message message : messenger.getMessages(ghost)) {
                if (message.getType() == BasicMessage.MessageType.I_AM){
                    MasterIs = message.getData();
                }
            }
        }
        if(reqAction){
            if(MasterIs != -1){
                return towardsAvoiding(game, lastPacmanIndex);
            }
        }
        return null;
    }
    private Constants.MOVE leadBehaviour(Game game, Messenger messenger,int currentIndex, int pacmanIndex, boolean reqAction){
        if (messenger != null) {
            messenger.addMessage(new BasicMessage(ghost, null, BasicMessage.MessageType.I_AM, currentIndex, game.getCurrentLevelTime()));
        }
        if(reqAction){
            return towards(game, pacmanIndex);
        }
        return null;
    }
    private Constants.MOVE fleeBehaviour(Game game,boolean reqAction){
        MasterIs = -1;
        if(reqAction){
            if(lastPacmanIndex != -1){
                return awayfrom(game,lastPacmanIndex);
            }else{
                return randomMove(game);
            }
        }
        return null;
    }

    private void flushMemory(int currentTick){
        if (currentTick <= 2 || currentTick - tickSeen >= TICK_THRESHOLD) {
            lastPacmanIndex = -1;
            tickSeen = -1;
            state = State.SEARCH;
        }
    }

    private Constants.MOVE awayfrom(Game game,int obstacle){
        try {
            return game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),
                    obstacle, game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(e);
            //System.out.println(lastPacmanIndex + " : " + currentIndex);
            return null;
        }
    }

    private Constants.MOVE towards(Game game,int aim){
        try {
            return game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
                    aim, game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println(e);
            //System.out.println(lastPacmanIndex + " : " + currentIndex);
            return null;
        }
    }

    private Constants.MOVE towardsAvoiding(Game game,int PacmanIndex){
        Maze m = game.getCurrentMaze();
        Node[] nodes = m.graph;
        nodes = nodes.clone();
        List<Node> listNodes = Arrays.asList(nodes);
        LinkedList<Node> lnkListNodes = new LinkedList<>(listNodes);
        lnkListNodes.remove(lnkListNodes.get(MasterIs));
        lnkListNodes.toArray(nodes);
        Constants.MOVE move = null;
        double minDistance = Integer.MAX_VALUE;
        try{
            for (Map.Entry<Constants.MOVE, Integer> entry : nodes[game.getGhostCurrentNodeIndex(ghost)].allNeighbourhoods.get(game.getGhostLastMoveMade(ghost)).entrySet()) {
                double distance = game.getDistance(entry.getValue(), PacmanIndex,  Constants.DM.PATH);
                if (distance < minDistance) {
                    minDistance = distance;
                    move = entry.getKey();
                }
            }
            return move;
        }catch (NullPointerException e){
            System.out.println("no path possible for " + ghost.name() + "while avoiding his master");
            Constants.MOVE[] possibleMoves = game.getPossibleMoves(game.getGhostCurrentNodeIndex(ghost), game.getGhostLastMoveMade(ghost));
            return possibleMoves[rnd.nextInt(possibleMoves.length)];
        }
    }

    private Constants.MOVE splitUp(Game game, int[] powPillsInd){
        if(ghost.name().equals("INKY")){
            return towards(game,powPillsInd[0]);
        }else if(ghost.name().equals("SUE")){
            return towards(game,powPillsInd[1]);
        }else if(ghost.name().equals("PINKY")){
            return towards(game,powPillsInd[2]);
        }else{
            return towards(game,powPillsInd[3]);
        }
    }

    private Constants.MOVE randomMove(Game game){
        Constants.MOVE[] possibleMoves = game.getPossibleMoves(game.getGhostCurrentNodeIndex(ghost), game.getGhostLastMoveMade(ghost));
        return possibleMoves[rnd.nextInt(possibleMoves.length)];
    }

    private Boolean InDanger(Game game){
        if(game.isGhostEdible(ghost)){
            return true;
        }
        int[] powerPills = game.getActivePowerPillsIndices();
        for (int i = 0; i < powerPills.length; i++) {
            if (game.getShortestPathDistance(powerPills[i], lastPacmanIndex) < PILL_PROXIMITY) {
                return true;
            }
        }
        return false;
    }
}
