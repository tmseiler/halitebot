package ai.actions;

import ai.Context;
import game.Direction;
import game.Location;
import game.Move;
import game.Site;

public class MoveTowardAction implements Action {

    private final Context context;
    private final Location destination;
    private Direction direction;

    public MoveTowardAction(Context context, Location destination) {
        this.context = context;
        this.destination = destination;
        direction = context.moveToward(context.agentLocation, destination, true);
    }

    @Override
    public Direction getStepDirection() {
        return direction;
    }

    @Override
    public Location getStep() {
        return context.gameMap.getLocation(context.agentLocation, getStepDirection());
    }

    @Override
    public Location getTarget() {
        return destination;
    }

    @Override
    public Site getTargetSite() {
        return context.gameMap.getSite(destination);
    }

    @Override
    public Move perform() {
        return new Move(context.agentLocation, getStepDirection());
    }

    @Override
    public Site getStepSite() {
        return context.gameMap.getSite(getStep());
    }
}
