

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashSet;

public class Carte {
    private final HashSet<Ville> villes;
    public final HashMap<String, LinkedList<Ville>> index;

    public Carte(String nom) {
	// lit un fichier et construit l'ensemble des villes qu'il contient
	VilleParser vp = new VilleParser(nom);
	villes = new LinkedHashSet<Ville>();
	vp.readAll(villes);
	index = new HashMap<String, LinkedList<Ville>>();
	for (Ville v : villes) {
	    LinkedList<Ville> villes = index.get(v.getNom());
	    if (villes == null) {
		villes = new LinkedList<Ville>();
		index.put(v.getNom(), villes);
	    }
	    villes.add(v);
	}
	System.out.println("Il y a " + villes.size() + " villes et " + index.size() + " noms");
    }
    
    public HashSet<Ville> villes() {
	return villes;
    }
    
    public Collection<Ville> villes(String nom) {
	Collection<Ville> villes = index.get(nom);
	if (villes == null) {
	    throw new IllegalArgumentException("Le nom " + nom + " n'existe pas");
	}
	return villes;
    }
    
    public Ville uneVille(String nom, int index) {
	Collection<Ville> villes = villes(nom);
	if (index >= villes.size())
	    index = villes.size() - 1;
	int i = 0;
	for (Ville v : villes(nom))
	    if (i++ == index)
		// return v;
        return new Ville(v.getNom(),v.getLatitude(),v.getLongitude());
    throw new AssertionError("La liste des villes de nom " + nom + " est vide");
  }

  public Ville premiereVille(String nom) {
    for (Ville v : villes(nom))
      // return v;
      return new Ville(v.getNom(),v.getLatitude(),v.getLongitude());
    throw new AssertionError("La liste des villes de nom " + nom + " est vide");
  }

    }

class VilleParser {
  private int currentChar;
  private final StringBuilder buffer = new StringBuilder();
  private final BufferedReader b;

  public VilleParser(String name) {
    try {
      if (name.startsWith("http://"))
        b = new BufferedReader(new InputStreamReader(
          new URL(name).openStream(), "UTF-8"));
      else
        b = new BufferedReader(new InputStreamReader(new FileInputStream(name),
          "UTF-8"));
    } catch (IOException e) {
      throw new IllegalArgumentException("unreadable file", e);
    }
  }

  private void skipToEOL() throws IOException {
    while (currentChar != -1 && "\r\n".indexOf(currentChar) == -1) {
      currentChar = b.read();
    }
    while (currentChar != -1 && "\r\n".indexOf(currentChar) != -1)
      currentChar = b.read();
  }

  private void skipNextField() throws IOException {
    while ("\t\f".indexOf(currentChar) == -1) {
      currentChar = b.read();
    }
    currentChar = b.read();
  }

  private double readDouble() throws IOException {
    int d = 0, sign = 1;
    if (currentChar == '-') {
      sign = -1;
      currentChar = b.read();
    }
    while (currentChar <= '9' && currentChar >= '0') {
      d = 10 * d + (currentChar - '0');
      currentChar = b.read();
    }
    if (currentChar != '.') {
      currentChar = b.read();
      return sign * d;
    }
    currentChar = b.read();
    int dot = 1;
    while (currentChar <= '9' && currentChar >= '0') {
      d = 10 * d + (currentChar - '0');
      dot *= 10;
      currentChar = b.read();
    }
    currentChar = b.read();
    return sign * d / (double) dot;
  }

  private String readString() throws IOException {
    buffer.setLength(0);
    while ("\t\f".indexOf(currentChar) == -1) {
      buffer.append((char) currentChar);
      currentChar = b.read();
    }
    return buffer.toString();
  }

  public void readAll(Collection<Ville> villes) {
    try {
      skipToEOL(); // On saute la ligne d entete
      do {
        skipNextField(); // RC : Region font code
        skipNextField(); // UFI : Unique feature identifier
        // double id = readDouble(); //UNI : Unique name identifier
        readDouble(); // UNI : Unique name identifier
        double latitude = readDouble();
        double longitude = readDouble();
        skipNextField(); // DMS_LAT
        skipNextField(); // DMS_LONG
        skipNextField(); // UTM : Universal transverse Mercator
        skipNextField(); // JOG : Joint operations Graphic reference
        if (currentChar != 'P') {
          skipToEOL();
          continue;
        }
        skipNextField(); // FC : Feature classification
        skipNextField(); // DSG : Feature designation code
        skipNextField(); // PC : Populated place classification
        skipNextField(); // CC1 : Primary coutry code
        skipNextField(); // DSG : Feature designation code
        skipNextField(); // ADM1 : First order administrative division code
        skipNextField(); // ADM2
        skipNextField(); // DIM : Dimension
        skipNextField(); // CC2
        skipNextField(); // NT : Name type
        skipNextField(); // LC : Language code
        skipNextField(); // SHORT_FORM
        skipNextField(); // GENERIC NAME
        skipNextField(); // SHORT_NAME
        String nom = readString(); // FULL_NAME
        skipToEOL();
        villes.add(new Ville(nom, latitude, longitude));
      } while (currentChar != -1);
      b.close();
      return;
    } catch (IOException e) {
      try {
        b.close();
      } catch (IOException e1) {/* nothing */
      }
      throw new IllegalArgumentException("invalid file", e);
    }
  }

}
