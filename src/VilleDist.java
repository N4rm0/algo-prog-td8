class VilleDist implements Comparable<VilleDist> {
    Ville v;
    double d;
    VilleDist(Ville v, double d) {
	this.v = v;
	this.d = d;
    }
    public int compareTo(VilleDist w) {
	if (this.d < w.d) return (-1);
	if (this.d > w.d) return (1);
	return (0);
    }
}