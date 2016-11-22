class Site {
    int owner, strength, production;

    double strengthProductionIndex, bolsterScore;

    boolean hold;

    double setStrengthProductionIndex() {
        strengthProductionIndex = (float) production * (float) production / (float) strength;
        return strengthProductionIndex;
    }
}
