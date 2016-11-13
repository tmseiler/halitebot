class Site {
    int owner, strength, production;

    double strengthProductionIndex, bolsterScore;

    double clusterAcquisitionScore;

    double individualAcquisitionScore() {
        strengthProductionIndex = (float) production / ((float) strength + 1);
        return strengthProductionIndex;
    }
}
