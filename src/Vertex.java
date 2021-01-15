import java.util.ArrayList;
import java.util.HashMap;


public  class Vertex implements Comparable<Vertex>  {

	public String id;
	public int indexOnGrapg;
	public ArrayList<String> values; // save all the values of vertex
	public ArrayList<String> parents; // save all the parents of vertex
	public ArrayList<String> childs; // save all the childs of vertex
	public HashMap<String, Integer>  indexReference; // save index of the CPT
	public ArrayList<String[]> cpt; // the CPT

	/*
	 * Constructor
	 */

	public Vertex(String id) {
		this.id = id;
		values = new ArrayList<String>();
		parents = new ArrayList<String>();
		childs = new ArrayList<String>();
		indexReference = new HashMap<String, Integer>();
		cpt = new ArrayList<String[]>();
	}

	/*
	 * copy constructor
	 */

	public Vertex(Vertex v) {
		this.id = v.id;
		this.indexOnGrapg = v.indexOnGrapg;
		this.values = v.values;
		this.parents = v.parents;
		this.childs = v.childs;
		this.indexReference = v.indexReference;
		this.cpt = new ArrayList<String[]>(v.cpt);
	}

	public void setValues(String values) { // set all the values of the vertex
		for (String value : values.split(",")) {
			this.values.add(value);
		}
	}

	public void setParents(String parents, HashMap<String, Vertex> graph) { // set all the parents of the vertex
		for (String parent : parents.split(",")) {
			this.parents.add(parent);
			graph.get(parent).addChild(this.id);
		}

	}

	public void addParent(String parent,  HashMap<String, Vertex> graph) {
		this.parents.add(parent);
	}

	public void setCPT(String values) {
		if (values.length() == 0) return;
		String[] valuesSplit = values.split(",");
		float oppositeValue = 0;
		ArrayList<String> opposite = new ArrayList<String>(this.values); // copy the values
		String[] withGiven = new String[this.parents.size()];
		int colum = 0; // colum in the CPT
		int pointRow = this.cpt.size()-1; // point to row in the CPT
		int sizeCPTRow = this.parents.size() + 2; // plus 2 for me and the result
		for (int i = 0; i < valuesSplit.length; i++) {
			//System.out.println(valuesSplit[i]);
			if (i < this.parents.size()) {
				withGiven[i] = valuesSplit[i];
			}
			else if (valuesSplit[i].contains("=")) { // we got my value
				this.cpt.add(new String[sizeCPTRow]);
				pointRow++;
				for (int j = 0; j < withGiven.length; j++) {
					this.cpt.get(pointRow)[colum] = withGiven[j]; // add the parents values
					colum++;
				}
				String myValue = valuesSplit[i].substring(1);
				this.cpt.get(pointRow)[colum] = myValue; // add my value without = at beginning
				colum++;
				opposite.remove(myValue);
			}
			else if (valuesSplit[i].isEmpty()) {}
			else { // we got to the number result
				cpt.get(pointRow)[colum] = valuesSplit[i];
				oppositeValue += Float.parseFloat(valuesSplit[i]);
				colum = 0;
			}
		}
		this.cpt.add(new String[sizeCPTRow]); // add the opposite result
		pointRow++;

		for (int i = 0; i < withGiven.length; i++) {
			this.cpt.get(pointRow)[colum] = withGiven[i];
			colum++;
		}
		this.cpt.get(pointRow)[colum] = opposite.get(0);
		colum++;
		oppositeValue = 1 - oppositeValue;
		this.cpt.get(pointRow)[colum] = oppositeValue + "";	
	}

	public void fillRow(String ver, int colum) {
		this.cpt.get(cpt.size()-1)[colum] = ver;
	}


	public void addChild(String child) {
		this.childs.add(child);
	}

	public int indexColum(String ver) { // get the index of specific vertex
		Integer result = indexReference.get(ver);
		if (result != null)
			return indexReference.get(ver);

		if (ver.equals(this.id))
			result = parents.size();
		else
			result = parents.indexOf(ver);

		indexReference.put(ver, result);
		return result;
	}

	/*
	 * this method compare between vertex to find which is best for the heuristic 
	 * in calculate all the neighbors and the size of the cpt (values)
	 */
	@Override
	public int compareTo(Vertex arg0) {
		int myNeighbersAndValues = this.parents.size() + this.childs.size() + this.cpt.size();
		int arg0NeighbersAndValues = arg0.parents.size() + arg0.childs.size() + arg0.cpt.size();
		return myNeighbersAndValues - arg0NeighbersAndValues;
	}
}