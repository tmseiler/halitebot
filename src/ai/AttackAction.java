package ai;

import game.*;

public class AttackAction implements Action {

    private GameMap gameMap;
    private final Location location;
    private final Direction enemyDirection;

    @Override
    public String toString() {
        return "AttackAction{" +
                "location=" + location +
                ", enemyDirection=" + enemyDirection +
                '}';
    }

    public AttackAction(GameMap gameMap, Location location, Direction enemyDirection) {
        this.gameMap = gameMap;
        this.location = location;
        this.enemyDirection = enemyDirection;
    }

    @Override
    public double getScore() {
        double score = 0.0;
        Site friendlySite = gameMap.getSite(location);
        Site enemySite = gameMap.getSite(location, enemyDirection);
        if (friendlySite.strength > enemySite.strength) {
            score += 100.00;
        } else {
            score += -100.00;
        }
        return score;
    }

    @Override
    public Move perform() {
        return new Move(location, enemyDirection);
    }
}
