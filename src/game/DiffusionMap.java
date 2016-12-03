package game;

import static util.Logger.out;

public class DiffusionMap {
    private double[][] map;
    private GameMap gameMap;
    private final boolean friendlyOnly;

    public DiffusionMap(GameMap gameMap, DiffusionSeeder seeder, boolean friendlyOnly) {
        this.gameMap = gameMap;
        this.friendlyOnly = friendlyOnly;
        this.map = seeder.seedMap(gameMap);
        int diffusionCount = Math.max(gameMap.width, gameMap.height) / 4;
        for (int i = 0; i < diffusionCount; i++) {
            diffuse();
        }
    }

    public DiffusionMap(GameMap gameMap, DiffusionSeeder seeder) {
        this(gameMap, seeder, false);
    }

    private void diffuse() {
        double coefficient = .05;
        double[][] newmap = new double[gameMap.width][gameMap.height];
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                double cellVal = 0.0;

                int eastX = getIndex(x + 1, gameMap.width);
                int westX = getIndex(x - 1, gameMap.width);
                int southY = getIndex(y + 1, gameMap.height);
                int northY = getIndex(y - 1, gameMap.height);

                cellVal += (map[eastX][y] - map[x][y]) * getDiffusionMultiplier(eastX, y);
                cellVal += map[westX][y] - map[x][y] * getDiffusionMultiplier(westX, y);
                cellVal += map[x][southY] - map[x][y] * getDiffusionMultiplier(x, southY);
                cellVal += map[x][northY] - map[x][y] * getDiffusionMultiplier(x, northY);
                cellVal *= coefficient;
                cellVal += map[x][y];
                newmap[x][y] = cellVal;
            }
        }
        this.map = newmap;
    }

    private double getDiffusionMultiplier(int x, int y) {
        Site site = gameMap.getSite(new Location(x, y));
        return (friendlyOnly && site.owner == GameMap.NEUTRAL_OWNER) ? 0.0: 1.0;
    }

    public void printMap() {
        for (int i = 0; i < gameMap.height; i++) {
            for (int j = 0; j < gameMap.width; j++) {
                out.printf("%-8.2f", map[i][j]);
            }
            out.printf("\n");
        }
    }

    private static int getIndex(int index, int bounds) {
        if (index < 0) return index + bounds;
        else return index % bounds;
    }

    public double getValue(int x, int y) {
        return map[x][y];
    }

    public double getValue(Location loc) {
        return getValue(loc.x, loc.y);
    }
}
