package org.ggp.base.player.gamer.statemachine;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by andrey on 16.04.15.
 */
public class MinimaxPlayer extends StateMachineGamer{
    @Override
    public void stateMachineAbort() {
    }

    @Override
    public void stateMachineStop() {

    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException {

    }

    private static Logger log = Logger.getLogger(MinimaxPlayer.class.getName());

    private Role getOpponent(Role role) {
        List<Role> roles = getStateMachine().getRoles();
        for (Role r : roles) {
            if (!r.equals(role)) {
                return r;
            }
        }
        return null;
    }


    private int heuristicScore(MachineState state, Role role) throws MoveDefinitionException, GoalDefinitionException {
        Role opponent = getOpponent(role);
        return getStateMachine().getGoal(state,role) - getStateMachine().getGoal(state, opponent);
    }

    private int depthCharge(MachineState state, Role role) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        if (getStateMachine().isTerminal(state)) {
            return getStateMachine().getGoal(state, role);
        }
        List<Role> roles = getStateMachine().getRoles();
        List<Move> actions = new LinkedList<>();
        for (Role r:roles) {
            List<Move> moves = getStateMachine().getLegalMoves(state, r);
            actions.add((moves.get(new Random().nextInt(moves.size()))));
        }
        return depthCharge(getStateMachine().getNextState(state, actions), role);
    }

    private int monteCarloScore(MachineState state, Role role, int count) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        int sum = 0;
        for (int i = 0; i < count; i++) {
            sum += depthCharge(state, role);
        }
        return sum/count;
    }

    private int maxScore(Role role, MachineState state, int alpha, int beta, int level) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        if (getStateMachine().isTerminal(state)) {
            return getStateMachine().getGoal(state, role);
        }
        if (level == 0) {
            return monteCarloScore(state, role, 2);
        }
        List<Move> moves = getStateMachine().getLegalMoves(state, role);
        for (Move move: moves) {
            int res = minScore(role, move, state, alpha, beta, level);
            alpha = Math.max(alpha, res);
            if (alpha >= beta) {
                return beta;
            }
        }
        return alpha;
    }

    private int minScore(Role role, Move action, MachineState state, int alpha, int beta, int level) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        Role opponent = getOpponent(role);
        if (opponent == null) {
            beta = maxScore(getRole(), getStateMachine().getNextState(state, new ArrayList<>(Arrays.asList(action))), alpha, beta, level - 1);
        } else {
            List<Role> roles = getStateMachine().getRoles();
            List<Move> moves = getStateMachine().getLegalMoves(state, opponent);
            for (Move move : moves) {
                List<Move> actions;
                if (role.equals(roles.get(0))) {
                    actions = new ArrayList<>(Arrays.asList(action, move));
                } else {
                    actions = new ArrayList<>(Arrays.asList(move, action));
                }
                int res = maxScore(role, getStateMachine().getNextState(state, actions), alpha, beta, level - 1);
//            log.info(actions.toString() + Integer.toString(res));
                beta = Math.min(beta, res);
                if (beta <= alpha) {
                    return alpha;
                }
            }
        }
        return beta;
    }

    private Move bestMove(MachineState state, long timeout) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
        List<Move> moves = getStateMachine().getLegalMoves(state,getRole());
        Move resMove = (moves.get(new Random().nextInt(moves.size())));
        if (moves.size() == 1) {
            return resMove;
        }
        int alpha = 0;
        int beta = 100;
        log.info("hui");
        log.info(state.toString());
        int depth = 2;
        long start = System.currentTimeMillis();
        long time0 = start;
        for (int i = 0; i < moves.size(); i++) {
            Move move = moves.get(i);
            int res = minScore(getRole(), move, state, alpha, beta, depth);
            log.info(move.toString() + Integer.toString(res));
            if (res > alpha) {
                alpha = res;
                resMove = move;
            }

            long time = System.currentTimeMillis();
//            log.info(Long.toString(time - time0) + " " + Long.toString(timeout) + " " + Long.toString(timeout - time0));
//            log.info(Long.toString((timeout - time0)/(moves.size() - i)));
//            log.info(Long.toString( (timeout - time0)/(moves.size() - i)/moves.size()));
            if (time - time0 > (timeout - time0)/(moves.size() - i)) {
                depth--;
                log.info("-");
            } else if (time - time0 < (timeout - time0)/(moves.size() - i)/moves.size()) {
                depth++;
                log.info("+");
            }
            time0 = time;
        }
        return resMove;
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        MachineState state = getCurrentState();


        return bestMove(state, timeout);
    }

    @Override
    public String getName() {
        return "MinimaxPlayer";
    }

    @Override
    public StateMachine getInitialStateMachine() {
        return new ProverStateMachine();
    }

    @Override
    public DetailPanel getDetailPanel() {
        return new SimpleDetailPanel();
    }

    @Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

    }
}
