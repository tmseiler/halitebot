package game;

import static util.Logger.out;

public class DiffusionMap {
    private double[][] map;
    private GameMap gameMap;

    public DiffusionMap(GameMap gameMap, DiffusionSeeder seeder) {
        this.gameMap = gameMap;
        this.map = seeder.seedMap(gameMap);
        int diffusionCount = Math.max(gameMap.width, gameMap.height) / 2;
        for (int i = 0; i < diffusionCount; i++) {
            diffuse();
        }
    }

    private void diffuse() {
        double coefficient = .05;
        double[][] newmap = new double[gameMap.width][gameMap.height];
        for (int y = 0; y < gameMap.height; y++) {
            for (int x = 0; x < gameMap.width; x++) {
                double cellVal = 0.0;
                cellVal += map[getIndex(x + 1, gameMap.width)][getIndex(y, gameMap.height)] - map[x][y];
                cellVal += map[getIndex(x - 1, gameMap.width)][getIndex(y, gameMap.height)] - map[x][y];
                cellVal += map[getIndex(x, gameMap.width)][getIndex(y + 1, gameMap.height)] - map[x][y];
                cellVal += map[getIndex(x, gameMap.width)][getIndex(y - 1, gameMap.height)] - map[x][y];
                cellVal *= coefficient;
                cellVal += map[x][y];
                newmap[x][y] = cellVal;
            }
        }
        this.map = newmap;
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
