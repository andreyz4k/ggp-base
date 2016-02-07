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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Created by andrey on 12.04.15.
 */
public class DumbPlayer extends StateMachineGamer{
    @Override
    public void stateMachineAbort() {
    }

    @Override
    public void stateMachineStop() {

    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException {

    }


    private int heuristicScore(MachineState state, Role role) throws MoveDefinitionException, GoalDefinitionException {
        return getStateMachine().getGoal(state,role);
    }

    private int maxScore(Role role, MachineState state, int level) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        if (getStateMachine().isTerminal(state)) {
            return getStateMachine().getGoal(state, role);
        }
        if (level == 0) {
            return heuristicScore(state, role);
        }
        List<Move> moves = getStateMachine().getLegalMoves(state, role);
        int score = 0;
        for (Move move: moves) {
            int res = maxScore(getRole(), getStateMachine().getNextState(state, new ArrayList<>(Arrays.asList(move))), level - 1);
            if (res > score) {
                score = res;
            }
        }
        return score;
    }

    private Move bestMove(MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
        List<Move> moves = getStateMachine().getLegalMoves(state,getRole());
        Move resMove = (moves.get(new Random().nextInt(moves.size())));
        int score = 0;
        for (Move move: moves) {
            int res = maxScore(getRole(), getStateMachine().getNextState(state, new ArrayList<>(Arrays.asList(move))), 8);
            if (res == 100) {
                return move;
            }
            if (res > score) {
                score = res;
                resMove = move;
            }
        }
        return resMove;
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        MachineState state = getCurrentState();


        return bestMove(state);
    }

    @Override
    public String getName() {
        return "DumbPlayer";
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
