package org.ggp.base.player.gamer.statemachine;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * Created by andrey on 24.05.15.
 * Class for Node in Game tree
 */
public class MonteCarloTreeNode {
    MachineState state;
    int visits;
    ArrayList<Double> utility;
    LinkedList<MonteCarloTreeNode> children;
    ArrayList<Move> prevMovesList;
    MonteCarloTreeNode parent;
    Role role;

    public MonteCarloTreeNode(MachineState state, MonteCarloTreeNode parent, Role role, int roles_count, ArrayList<Move> moves) {
        this.state = state;
        this.parent = parent;
        this.visits = 0;
        this.utility = new ArrayList<>(roles_count);
        for (int i = 0; i < roles_count; i++) {
            this.utility.add((double) 0);
        }
        this.children = new  LinkedList<>();
        this.role = role;
        this.prevMovesList = moves;
    }
}
