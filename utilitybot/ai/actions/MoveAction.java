package ai.actions;

import ai.Context;
import game.Direction;
import game.Location;
import game.Move;
import game.Site;

public class MoveAction implements Action {
    private Context context;
    public Direction direction;

    public MoveAction(Context context, Direction direction) {
        this.context = context;
        this.direction = direction;
    }

    @Override
    public Direction getTargetDirection() {
        return direction;
    }

    @Override
    public Location getTarget() {
        return context.gameMap.getLocation(context.agentLocation, direction);
    }

    @Override
    public Site getTargetSite() {
        return context.gameMap.getSite(getTarget());
    }

    @Override
    public Move perform() {
        return new Move(context.agentLocation, direction);
    }

    @Override
    public String toString() {
        return "MoveAction{" +
                "direction=" + direction +
                '}';
    }
}
