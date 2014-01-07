import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Test {

	static Logger LOGGER = null;

	static void init() throws SecurityException, IOException {
		LOGGER = Logger.getLogger("default");
		LOGGER.setUseParentHandlers(false);
		Handler handler = new FileHandler("logs.txt");
		handler.setFormatter(new LogFormatter());
		handler.setLevel(Level.ALL);
		LOGGER.addHandler(handler);
		LOGGER.setLevel(Level.INFO);
	}

	// pour charger les cartes
	static Carte carte;
	static final String chemin = "./"; // data file location
	Carte ens = new Carte(chemin + "mf.txt");

	static HashMap<Ville, HashSet<Ville>> voisins;
	static HashMap<String, HashSet<Ville>> nom;

	static double minDistStatic;
	// dijkstra elements
	private static Map<Ville, Double> distances;
	private static Set<Ville> closedSet, openSet;
	private static Ville start, finish;

	static void construitGraphe(Collection<Ville> cv, double minDist) {
		// use big number for dijkstra
		double R = 6371000;
		double latDist = minDist * 180.0 / Math.PI / R;
		minDistStatic = minDist;
		// des villes à moins de minDist l'une de l'autre auront au plus une
		// différence de
		// latitude (ou de longitude) de latDist

		// indication : on peut trier un tableau de villes par Array.sort

		// ici construire le graphe des villes en mettant les bonnes valeurs
		// dans les tables voisins et nom
		Timer.start();
		List<Ville> list = new ArrayList<Ville>(cv);
		Collections.sort(list);
		nom = new HashMap<>();
		voisins = new HashMap<>();
		for (int i =0; i< list.size(); i++) {
			Ville v = list.get(i);
			if (nom.containsKey(v.getNom())) {
				nom.get(v.getNom()).add(v);
			} else {
				HashSet<Ville> setVille = new HashSet<>();
				setVille.add(v);
				nom.put(v.getNom(), setVille);
			}
			// put here voisin loop
			fillVoisinWithDoubleLoop(v, list, latDist);
		}

		Timer.end();
		Timer.log("Build of Graph :", LOGGER);

	}

	static void fillVoisinWithDoubleLoop(Ville v, List<Ville> list,
			double latDist) {
		int index = list.indexOf(v);
		int size = list.size();
		// upward loop
		for (int i = 1; i + index < size; i++) {
			Ville otherVille = list.get(i + index);
			HashSet<Ville> setVoisin = voisins.containsKey(v) ? voisins.get(v)
					: new HashSet<Ville>();
			voisins.put(v, setVoisin);
			if (distanceWithDiff(v, otherVille, latDist)) {
				if (v.distance(otherVille) <= minDistStatic) {
					setVoisin.add(otherVille);
					if (LOGGER.getLevel() == Level.FINER)
						LOGGER.finer("add neighbor " + otherVille.getNom()
								+ " to " + v.getNom() + " distance "
								+ v.distance(otherVille));
				}
			} else {
				// stop here
				if (LOGGER.getLevel() == Level.FINE)
					LOGGER.fine("added " + setVoisin.size() + " neighbor of "
							+ v.getNom() + " with bigger latitude");
				break;
			}
		}
		// downward loop
		for (int i = 1; index - i > -1; i++) {
			Ville otherVille = list.get(index - i);
			HashSet<Ville> setVoisin = voisins.containsKey(v) ? voisins.get(v)
					: new HashSet<Ville>();
			voisins.put(v, setVoisin);
			if (distanceWithDiff(v, otherVille, latDist)) {
				if (v.distance(otherVille) <= minDistStatic) {
					setVoisin.add(otherVille);
					if (LOGGER.getLevel() == Level.FINER)
						LOGGER.finer("add neighbor " + otherVille.getNom()
								+ " to " + v.getNom() + " distance "
								+ v.distance(otherVille));
				}
			} else {
				// stop here
				if (LOGGER.getLevel() == Level.FINE)
					LOGGER.fine("added " + setVoisin.size() + " neighbor of "
							+ v.getNom() + " with lower latitude");
				break;
			}
		}
	}

	static void fillVoisinWithON2(Ville v, List<Ville> list, double latDist) {
		// fill voisins
		for (Ville otherVille : list) {
			HashSet<Ville> setVoisin = voisins.containsKey(v) ? voisins.get(v)
					: new HashSet<Ville>();
			voisins.put(v, setVoisin);
			if (v.distance(otherVille) < minDistStatic) {
				if (!v.equals(otherVille)) {
					// distance is ok, both villes are neighbors
					setVoisin.add(otherVille);
					// LOGGER.finer("add neighbor " + otherVille.getNom() +
					// " to "
					// + v.getNom() + " distance "
					// + v.distance(otherVille));
				}
			}
			// else {
			// if (list.indexOf(otherVille) > list.indexOf(v)) {
			// // break only if
			// break;
			// }
			// }
		}
	}

	static boolean distanceWithDiff(Ville a, Ville b, double latDist) {
		return Math.abs(a.getLatitude() - b.getLatitude()) <= latDist;
	}

	static void printVilleWithSameName() {
		for (Entry<String, HashSet<Ville>> entry : nom.entrySet()) {
			if (entry.getValue().size() > 1) {
				LOGGER.fine("Ville " + entry.getKey() + " has not unique name");
			}
		}
	}

	static Ville premiereVille(String s) {
		return (nom.get(s).iterator().next());
	}

	static double Dijkstra(Ville orig, Ville dest) {
		// utiliser Dijkstra pour calculer la longueur du plus court chemin
		// entre v1 et v2
		// rendre -1 s'il n'y a pas de chemin
		start = orig;
		finish = dest;
		initDijkstra(start);

		while (!openSet.isEmpty()) {
			Ville nearestVille = findNearestVille();
			if (LOGGER.getLevel() == Level.FINE) {
				LOGGER.fine("size close set " + closedSet.size());
				LOGGER.fine("size open set " + openSet.size());
			}
			logDistances();
			if (nearestVille.equals(finish)) {

				return distances.get(finish);
			}
		}
		// not found => return 0
		return 0;
	}

	private static void initDijkstra(Ville orig) {
		// init distances
		distances = new HashMap<>();
		for (Set<Ville> set : nom.values()) {
			for (Ville ville : set) {
				distances.put(ville, Double.MAX_VALUE);
			}
		}
		distances.put(orig, 0.0);

		// init sets
		closedSet = new HashSet<>();
		closedSet.add(orig);
		// use a copy of voisin
		openSet = new HashSet<>(voisins.get(orig));
		LOGGER.fine("add " + voisins.get(orig).size() + " villes neighbor of "
				+ orig.getNom());
	}

	private static void updateDistances(Ville s1, Ville s2) {
		if (distances.get(s2) > distances.get(s1) + s2.distance(s1)) {
			LOGGER.fine("update distance to " + s2.getNom() + " with "
					+ s1.getNom());
			distances.put(s2, distances.get(s1) + s2.distance(s1));
		}
	}

	private static Ville findNearestVille() {
		double min = Double.MAX_VALUE;
		Ville nextVille = null;
		Ville originVille = null;
		for (Ville ville1 : closedSet) {
			for (Ville ville2 : openSet) {
				if (voisins.get(ville1).contains(ville2)) {
					double dist = distances.get(ville1)
							+ ville1.distance(ville2);
					if (dist < min) {
						min = dist;
						originVille = ville1;
						nextVille = ville2;
					}
				}
			}
		}
		LOGGER.fine(String.format(
				"min distance: %.0f. origin: %s. destination: %s", min,
				originVille.getNom(), nextVille.getNom()));
		updateDistances(originVille, nextVille);
		addVilleToClosedSet(nextVille);
		return nextVille;
	}

	private static void addVilleToClosedSet(Ville ville) {
		closedSet.add(ville);
		if (LOGGER.getLevel() == Level.FINE)
			LOGGER.fine("add " + ville.getNom()
					+ " to close set, remove it from open set");
		openSet.remove(ville);

		HashSet<Ville> voisinsList = voisins.get(ville);
		for (Ville nextVille : voisinsList) {
			if (!closedSet.contains(nextVille)) {
				openSet.add(nextVille);
				LOGGER.fine("add " + nextVille.getNom() + " to open set");
			}
		}
		for (Ville villeVoisine : openSet) {
			if (villeVoisine.distance(ville) <= minDistStatic) {
				updateDistances(ville, villeVoisine);
			}
		}
	}

	public static void initMayotte(double minDist) {
		Carte ens = new Carte(chemin + "mf.txt");
		construitGraphe(ens.villes(), minDist);
	}

	public static void initFrance(double minDist) {
		Carte ens = new Carte(chemin + "fr.txt");
		minDistStatic = minDist;
		construitGraphe(ens.villes(), minDist);

	}

	public static void test0() {
		initFrance(2000);
		printVilleWithSameName();
		
	}

	static void logDistances() {
		if (LOGGER.getLevel() == Level.FINE)
			for (Entry<Ville, Double> entry : distances.entrySet()) {
				if (entry.getValue() != Double.MAX_VALUE) {
					String message = String.format("distance %s %.0f", entry
							.getKey().getNom(), entry.getValue());
					LOGGER.fine(message);
				}
			}
	}

	public static void test1(double minDist) {
		System.out.println();
		System.out.println("Mayotte, pas de " + minDist);
		initMayotte(minDist);

		Ville v1 = premiereVille("Accua");
		Ville v2 = premiereVille("Moutsamoudou");
		Ville v3 = premiereVille("Bandraboua");
		Ville v4 = premiereVille("Mambutzou");
		afficheDijkstra(v1, v2);
		afficheDijkstra(v2, v1);
		afficheDijkstra(v1, v3);
		afficheDijkstra(v3, v1);
		afficheDijkstra(v1, v4);
		afficheDijkstra(v4, v1);
		afficheDijkstra(v2, v3);
	}

	public static void afficheDijkstra(Ville v1, Ville v2) {
		DecimalFormat df = new DecimalFormat("#.000");
		double d = Dijkstra(v1, v2);
		String s = "  pas de chemin";
		if (d > 0)
			s = df.format(d / 1000);

		System.out.println(v1.getNom() + " " + v2.getNom() + " " + s);
	}

	public static void test2(double minDist) {
		System.out.println();
		System.out.println("France, pas de " + minDist);

		initFrance(minDist);

		Ville paris = premiereVille("Paris");
		Ville rouen = premiereVille("Rouen");
		Ville palaiseau = premiereVille("Palaiseau");
		Ville perpignan = premiereVille("Perpignan");
		Ville strasbourg = premiereVille("Strasbourg");
		Ville hagenau = premiereVille("Hagenau");
		Ville brest = premiereVille("Brest");
		Ville hendaye = premiereVille("Hendaye");

		afficheDijkstra(paris, rouen);
		afficheDijkstra(palaiseau, rouen);
		afficheDijkstra(palaiseau, paris);
		afficheDijkstra(paris, perpignan);
		afficheDijkstra(hendaye, perpignan);
		afficheDijkstra(paris, strasbourg);
		afficheDijkstra(hagenau, strasbourg);
		afficheDijkstra(hagenau, brest);

	}

	public static void stupidTest(Collection<Ville> set) {
		List<Ville> list= new ArrayList<>(set); 
		Collections.sort(list);
		Timer.start();
		long count = 0;
		int size = list.size();
		for (int i = 0; i< list.size(); i++) {
			Ville v = list.get(i);

			double R = 6371000;
			double latDist = minDistStatic * 180.0 / Math.PI / R;
			// upward loop
			for (int j = 1; j + i < size; j++) {
				Ville otherVille = list.get(i + j);
				if (distanceWithDiff(v, otherVille, latDist)) {
					count++;
				} else {
					break;
				}
			}
			// downward loop
			for (int k = 1; i - k > -1; k++) {
				Ville otherVille = list.get(i - k);
				if (distanceWithDiff(v, otherVille, latDist)) {
					count++;
				} else {
					// stop here
					break;
				}
			}
		}
		Timer.end();
		Timer.log("test stupid double loop time count " + count, LOGGER);
		System.exit(0);
	}

	public static void main(String[] args) throws SecurityException,
			IOException {
		init();
		// test1(1850);
		// test1(2000);
		// test1(3000);
		// test1(3400);
		// test1(4000);
		//
		// tests sur la carte de France
		//test2(2000);
		 test2(5000);
		// test2(7000);
		// test2(10000);

	}

}

class LogFormatter extends Formatter {

	@Override
	public String format(LogRecord record) {
		StringBuffer s = new StringBuffer(1000);
		Date d = new Date(record.getMillis());
		DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,
				DateFormat.SHORT, Locale.FRANCE);
		s.append(df.format(d));
		s.append(" ");
		s.append(record.getLevel().getName());
		s.append(" ");
		s.append(record.getSourceMethodName());
		s.append(" ");
		s.append(record.getMessage());
		s.append("\n");
		return s.toString();
	}

}

class Timer {

	static long start, end;

	public static void start() {
		start = System.currentTimeMillis();
	}

	public static void end() {
		end = System.currentTimeMillis();
	}

	public static void log(String prefixMessage, Logger logger) {
		logger.log(Level.INFO, prefixMessage + " Time taken: " + (end - start)
				+ "ms");
	}

}