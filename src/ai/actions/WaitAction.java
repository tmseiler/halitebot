package ai.actions;

import ai.Context;
import game.*;

public class WaitAction implements Action {
    private Context context;

    public WaitAction(Context context) {
        this.context = context;
    }

    @Override
    public String toString() {
        return "WaitAction{" +
                "friendlyLoc=" + context.agentLocation +
                '}';
    }

    @Override
    public Direction getTargetDirection() {
        return Direction.STILL;
    }

    @Override
    public Location getTarget() {
        return context.agentLocation;
    }

    @Override
    public Site getTargetSite() {
        return context.gameMap.getSite(context.agentLocation);
    }

    @Override
    public Move perform() {
        return new Move(context.agentLocation, Direction.STILL);
    }
}
