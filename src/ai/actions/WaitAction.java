package ai.actions;

import ai.scoring.Context;
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

    public double getScore() {
        double score = 1.0;
        Site site = context.gameMap.getSite(context.agentLocation);
        if(site.strength == 0) score += 100;
        if(site.strength < (site.production * 3)) score += 25;
        return score;
    }

    @Override
    public Direction getTargetDirection() {
        return null;
    }

    @Override
    public Location getTarget() {
        return null;
    }

    @Override
    public Site getTargetSite() {
        return null;
    }

    @Override
    public Move perform() {
        return new Move(context.agentLocation, Direction.STILL);
    }
}
