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
 * Created by andrey on 27.04.15.
 * Monte Carlo Tree Gamer
 */
public class MonteCarloTreeGamer extends StateMachineGamer{

    List<Role> roles;
    int roleIndex;

    MonteCarloTreeNode tree;
    MonteCarloTreeNode currentTree;
    Move lastAction;

    @Override
    public void stateMachineAbort() {
    }

    @Override
    public void stateMachineStop() {

    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException {

    }

    private MonteCarloTreeNode selectNode(MonteCarloTreeNode node, ListIterator<Role> roles_iterator) {
        if (node.visits == 0) {
//            log.info("Root");
            return node;
        }
//        log.info("Not root");
        for (MonteCarloTreeNode n: node.children) {
            if (n.visits == 0) {
                return n;
            }
        }
        double score = 0;
        MonteCarloTreeNode result = node;
//        log.info(node.children.toString());
        for (MonteCarloTreeNode n: node.children) {
            double newScore = selectFn(n, roles_iterator);
            if (newScore >= score) {
                score = newScore;
                result = n;
            }
        }

//        log.info(String.valueOf(score));
        roles_iterator.next();
        if (!roles_iterator.hasNext()) {
            roles_iterator = roles.listIterator();
        }
        if (result == node) {
            return node;
        }
        return selectNode(result, roles_iterator);
    }

    private double selectFn(MonteCarloTreeNode node, ListIterator<Role> roles_iterator) {
        return node.utility.get(roles_iterator.nextIndex()) + Math.sqrt(2*Math.log(node.parent.visits)/node.visits);
    }

    private void expandTree(MonteCarloTreeNode node) throws MoveDefinitionException, TransitionDefinitionException {
        if (getStateMachine().isTerminal(node.state)) {
            return;
        }
        List<Move> moves = getStateMachine().getLegalMoves(node.state, node.role);
        int curRoleIndex = roles.indexOf(node.role);
        Role nextRole;
        if (curRoleIndex + 1 < getStateMachine().getRoles().size()) {
            nextRole = roles.get(curRoleIndex + 1);
        } else {
            nextRole = roles.get(0);
        }
        for (Move move: moves) {
            ArrayList<Move> newMoves;
            if (node.prevMovesList.size() == roles.size()) {
                newMoves = new ArrayList<>();
            } else {
                newMoves = new ArrayList<>(node.prevMovesList);
            }
            newMoves.add(move);
            MachineState newState;
            if (newMoves.size() == roles.size()) {
                ArrayList<Move> transitionMoves = new ArrayList<>(newMoves);
                Move myMove = transitionMoves.remove(0);
                transitionMoves.add(roleIndex, myMove);
                newState = getStateMachine().getNextState(node.state, transitionMoves);
            } else {
                newState = node.state;
            }
            MonteCarloTreeNode newNode = new MonteCarloTreeNode(newState, node, nextRole, roles.size(), newMoves);
            node.children.add(newNode);
        }
    }

    private void backPropagate(MonteCarloTreeNode node, List<Integer> score) {
        node.visits += 1;
        for (int i = 0; i < node.utility.size(); i++) {
            node.utility.set(i, (node.utility.get(i) * (node.visits - 1) + (double)score.get(i))/node.visits);
        }
        if (node.parent != null) {
            backPropagate(node.parent, score);
        }
    }

    private static Logger log = Logger.getLogger(MinimaxPlayer.class.getName());

    private List<Integer> depthCharge(MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        if (getStateMachine().isTerminal(state)) {
            return getStateMachine().getGoals(state);
        }
        List<Role> roles = getStateMachine().getRoles();
        List<Move> actions = new LinkedList<>();
        for (Role r:roles) {
            List<Move> moves = getStateMachine().getLegalMoves(state, r);
            actions.add((moves.get(new Random().nextInt(moves.size()))));
        }
        return depthCharge(getStateMachine().getNextState(state, actions));
    }

    private void monteCarloIteration(MonteCarloTreeNode rootNode) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        ListIterator<Role> iterator = getStateMachine().getRoles().listIterator();
        MonteCarloTreeNode currentNode = selectNode(rootNode, iterator);
        expandTree(currentNode);
        List<Integer> result;

        if (getStateMachine().isTerminal(currentNode.state)) {
//            log.info("Terminal");
        } else {
            currentNode = currentNode.children.getFirst();
        }
        result = new ArrayList<>(depthCharge(currentNode.state));
        int myResult = result.remove(roleIndex);
        result.add(0, myResult);
        backPropagate(currentNode, result);
    }

    private Move bestMove(MonteCarloTreeNode rootNode) {
        Move resMove = null;
        double bestScore = 0;
        Role role = getRole();
        int roleIndex = roles.indexOf(role);
        for (MonteCarloTreeNode node: rootNode.children) {
            double score = node.utility.get(roleIndex);
            if (score >= bestScore) {
                bestScore = score;
                resMove = node.prevMovesList.get(node.prevMovesList.size()-1);
            }
        }
        log.info("bestMove");
        log.info(resMove.toString());
        log.info(String.valueOf(bestScore));
        log.info(String.valueOf(rootNode.visits));
        return resMove;
    }

    private MonteCarloTreeNode findRecursive(MonteCarloTreeNode tree, MachineState state, int limit) {
        if (tree.state.equals(state)) {
            return tree;
        }
        if (limit == 0) {
            return null;
        }
        for (MonteCarloTreeNode child: tree.children) {
            MonteCarloTreeNode newNode = findRecursive(child, state, limit - 1);
            if (newNode != null) {
                return newNode;
            }
        }
        log.info("Fail");
        return null;
    }

    private MonteCarloTreeNode getCurrentTree(MonteCarloTreeNode tree, MachineState state) {
        if (tree.state.equals(state)) {
            return tree;
        }
        MonteCarloTreeNode newNode = null;
        for (MonteCarloTreeNode child: tree.children) {
            if (child.prevMovesList.get(0).equals(lastAction)) {
                newNode = child;
            }
        }
        newNode = findRecursive(newNode, state, roles.size() - 1);
        return newNode;
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        MachineState state = getCurrentState();

        currentTree = getCurrentTree(currentTree, state);
        long time = System.currentTimeMillis();
        while (time < timeout - 1000) {
            monteCarloIteration(currentTree);
            time = System.currentTimeMillis();
        }

        lastAction = bestMove(currentTree);
        return lastAction;
    }

    @Override
    public String getName() {
        return "MonteCarloTreeGamer";
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
        roles = new ArrayList<>(getStateMachine().getRoles());
        Role role = getRole();
        log.info(role.toString());
        roleIndex = roles.indexOf(role);
        roles.remove(role);
        roles.add(0, role);

        tree = new MonteCarloTreeNode(getStateMachine().getInitialState(), null, roles.get(0), roles.size(), new ArrayList<Move>());
        currentTree = tree;
        long time = System.currentTimeMillis();
        while (time < timeout - 1000) {
            monteCarloIteration(tree);
            time = System.currentTimeMillis();
        }
    }
}
