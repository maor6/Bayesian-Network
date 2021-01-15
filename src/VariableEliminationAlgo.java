import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class VariableEliminationAlgo {

	private HashMap<String, Vertex> graph; // represent the Bayesian network 
	public int countMul;
	public int countSum;

	/*
	 * Constructor 
	 */

	public VariableEliminationAlgo(HashMap<String, Vertex> netWork) {
		countSum = 0;
		countMul = 0;
		graph = netWork;
	}

	/*
	 * this method will prepare the information we have 
	 * and return the result of the query
	 */

	public double getQueryResult(String query, ArrayList<Vertex> gones, boolean heuristic) {
		String vertexQuery[] = query.substring(query.indexOf('(') + 1, query.indexOf('|')).split("=");
		String hidden[] = new String[gones.size()];
		HashMap<String, String> evidences = new HashMap<String, String>();

		if (heuristic) { // arrange by heuristic  
			Collections.sort(gones); // sort by comparable in Vertex class
		}

		for (int i = 0; i < hidden.length; i++) {
			hidden[i] = gones.get(i).id;
		}

		if (!heuristic) { // arrange by the ABC
			Arrays.sort(hidden);
		}

		for (String tempEvidence : query.substring(query.indexOf('|') + 1, query.indexOf(')')).split(",")) {
			if (!tempEvidence.isEmpty()) {
				evidences.put(tempEvidence.substring(0, tempEvidence.indexOf('=')), tempEvidence.substring(tempEvidence.indexOf('=') + 1));
			}
		}

		double answer = elimination(hidden, evidences, vertexQuery);
		return answer;
	}


	/*
	 * this method start the elimination algorithm
	 */

	private double elimination(String[] hidden, HashMap<String, String> evidences, String[] vertexQuery) {
		double instantResult = instantResult(vertexQuery, evidences);
		if (instantResult != 0) {
			return instantResult;
		}

		ArrayList<Vertex> myFactors = Factors(hidden, evidences, vertexQuery[0]); // save the factors
		int[] indexMinimalPair = null;
		for (String hiddens : hidden) { // over all the hidden vertexes
			while (true) {
				indexMinimalPair = findMinimalPair(hiddens, myFactors, evidences); // find and start with the minimal multiplexing
				if (indexMinimalPair == null) {
					break;
				}
				else if (indexMinimalPair.length == 1) {
					if (!hiddens.equals( myFactors.get(indexMinimalPair[0]).id) || myFactors.get(indexMinimalPair[0]).parents.size() > 0) {
						Vertex vertexSum = sumFactors(hiddens, myFactors.get(indexMinimalPair[0])); // do sumOut
						myFactors.remove(indexMinimalPair[0]); // remove the old factor
						myFactors.add(vertexSum); // add the limited factor
					}
					else {
						myFactors.remove(indexMinimalPair[0]);
					}
				}
				else {
					join(evidences, hiddens, indexMinimalPair, myFactors); // join the factors
				}
			}
		}

		while (myFactors.size() > 1) { // if some factors remain do join until we have 1 factor
			int[] indexMinimalPair2 = {0, 1};
			join(evidences, vertexQuery[0], indexMinimalPair2, myFactors);
		}

		Vertex v = myFactors.get(0);

		normalize(v);

		countSum += v.cpt.size() - 1;
		int indexOfResult = v.indexColum(vertexQuery[0]);
		double answer = 0;
		for (String s[] : v.cpt) { // get the probability
			if (s[indexOfResult].equals(vertexQuery[1])) {
				answer =  Double.parseDouble(s[s.length - 1]);
			}
		}

		return answer;
	}

	/*
	 * this method check 
	 * if we can get the answer from the CPT
	 */

	private double instantResult(String[] vertexQuery, HashMap<String, String> evidences) {
		Vertex vertexQueryInstance = graph.get(vertexQuery[0]);

		for (String evidence : evidences.keySet()) {
			if (vertexQueryInstance.indexColum(evidence) == -1)
				return 0;
		}

		for (String row[] : vertexQueryInstance.cpt) { // Find row that contain our result in the CPT
			boolean isRowResult = true;
			for (String parent : vertexQueryInstance.parents) {
				if (!row[vertexQueryInstance.indexColum(parent)].equals(evidences.get(parent))) {
					isRowResult = false;
					break;
				}
			}

			if (isRowResult && row[vertexQueryInstance.indexColum(vertexQuery[0])].equals(vertexQuery[1])) {  // Check query is same value in the row
				return Double.parseDouble(row[row.length - 1]);
			}
		}

		return 0;
	}


	/*
	 * this method calculate and do normalization
	 */

	private void normalize(Vertex vertex) {
		double sumRows = 0;
		for (String row[] : vertex.cpt) { 
			sumRows += Double.parseDouble(row[row.length - 1]);
		}

		for (String row[] : vertex.cpt) {
			row[row.length - 1] = (Double.parseDouble(row[row.length - 1]) / sumRows) + "";
		}
	}

	/*
	 * this method join the factors
	 */

	private void join( HashMap<String, String> evidences, String hiddens, int[] indexMinimalPair, ArrayList<Vertex> factors) {
		Vertex vertexMin1 = factors.get(indexMinimalPair[0]);
		Vertex vertexMin2 = factors.get(indexMinimalPair[1]);
		Vertex newFac = new Vertex(hiddens);
		
		initializeParent(newFac, vertexMin1, evidences, hiddens);
		initializeParent(newFac, vertexMin2, evidences, hiddens);
		
		cleanCPT(vertexMin1, evidences);
		cleanCPT(vertexMin2, evidences);

		joinVertexes(vertexMin1, vertexMin2, newFac, evidences);

		factors.remove(vertexMin1);
		factors.remove(vertexMin2);

		factors.add(newFac);
	}

	private void fillRow(Vertex vertex, int indexRowVertex, Vertex result) {
		for (int i = 0; i < vertex.parents.size(); i++) {
			String currentName = vertex.parents.get(i);
			int IndexOfCurrent = result.indexColum(currentName);
			if (IndexOfCurrent != -1) {
				String value = vertex.cpt.get(indexRowVertex)[i];
				result.fillRow(value, IndexOfCurrent);
			}
		}

		int indexOfCurrent = result.indexColum(vertex.id);
		if (indexOfCurrent != -1) {
			String value = vertex.cpt.get(indexRowVertex)[vertex.parents.size()];
			result.fillRow(value, indexOfCurrent);
		}
	}

	/*
	 * this method clean rows from CPT
	 */

	private void cleanCPT(Vertex vertex, HashMap<String, String> evidences) {
		ArrayList<String[]> newCPT = new ArrayList<String[]>();
		for (int i = 0; i < vertex.cpt.size(); i++) {
			String row[] = vertex.cpt.get(i);
			boolean saveRow = true;
			for (Map.Entry<String, String> set : evidences.entrySet()) {
				int keyCPTindex = vertex.indexColum(set.getKey());
				if (keyCPTindex != -1 && !row[keyCPTindex].equals(set.getValue())) {
					saveRow = false;
					break;
				}
			}
			
			if (saveRow) {
				newCPT.add(row);
			}
		}
		vertex.cpt = newCPT;
	}

	private void initializeParent(Vertex verAfteSum, Vertex vertex, HashMap<String, String> evidences, String hiddenVertex) {
		for (String parent : vertex.parents) {
			if (!parent.equals(hiddenVertex) && !verAfteSum.parents.contains(parent) && !evidences.containsKey(parent)) {
				verAfteSum.addParent(parent, graph);
			}
		}

		if (!vertex.id.equals(hiddenVertex) && !vertex.parents.contains(vertex.id) &&
				!evidences.containsKey(vertex.id) && !verAfteSum.parents.contains(vertex.id)) {
			verAfteSum.addParent(vertex.id, graph);
		}
	}


	private Vertex sumFactors(String hiddenVertex, Vertex vertex) {
		boolean isRowCalculated[] = new boolean[vertex.cpt.size()];
		Vertex sumVertex = initNewVertexSum(vertex, hiddenVertex);
		int indexOfHidden = vertex.indexColum(hiddenVertex);
		ArrayList<String[]> vertexCPT = vertex.cpt;
		//System.out.println(vertex);
		for (int i = 0; i < vertexCPT.size(); i++) {
			if (!isRowCalculated[i]) {
				isRowCalculated[i] = true;
				String row[] = vertexCPT.get(i);
				double sumRows = Double.parseDouble(row[row.length - 1]);
				for (int j = i + 1; j < vertexCPT.size(); j++) {
					if (!isRowCalculated[j]) {
						String otherRow[] = vertexCPT.get(j);
						boolean isMatch = true;
						for (int k = 0; k < row.length - 1 && isMatch; k++) {
							if (k != indexOfHidden) {
								if (!row[k].equals(otherRow[k])) {
									isMatch = false;
								}
							}
						}

						if (isMatch) {
							isRowCalculated[j] = true;
							sumRows += Double.parseDouble(otherRow[otherRow.length - 1]);
							countSum++;
						}
					}
				}

				sumVertex.cpt.add(new String[sumVertex.parents.size() + 2]);
				int col = 0;
				for (int l = 0; l < row.length - 1; l++) {
					if (l != indexOfHidden) {
						sumVertex.fillRow(row[l], col++);
					}
				}
				sumVertex.fillRow(sumRows + "", col);
			}
		}

		return sumVertex;
	}


	/*
	 * find matching rows
	 * if we found then multiply the rows by the share vertex
	 */
	private void joinVertexes(Vertex vertexMin1, Vertex vertexMin2, Vertex newFac, HashMap<String, String> evidences) {
		HashMap<String, Integer> sharedVertexes = sharedVertex(vertexMin1, vertexMin2, evidences);

		for (int i = 0; i < vertexMin1.cpt.size(); i++) {
			for (int j = 0; j < vertexMin2.cpt.size(); j++) {
				boolean isMatchRow = true;

				for (Map.Entry<String, Integer> set : sharedVertexes.entrySet()) {
					if (set.getValue() == 2 && !vertexMin1.cpt.get(i)[vertexMin1.indexColum(set.getKey())].
							equals(vertexMin2.cpt.get(j)[vertexMin2.indexColum(set.getKey())])) {
						isMatchRow = false;
						break;
					}
				}

				if (isMatchRow) {
					newFac.cpt.add(new String[newFac.parents.size() + 2]);
					fillRow(vertexMin1, i, newFac);
					fillRow(vertexMin2, j, newFac);
					double newValue = Double.parseDouble(vertexMin1.cpt.get(i)[vertexMin1.cpt.get(i).length - 1]) * Double.parseDouble(vertexMin2.cpt.get(j)[vertexMin2.cpt.get(j).length - 1]);
					countMul++;
					newFac.fillRow("" + newValue, newFac.parents.size() + 1);
				}
			}
		}
	}


	private Vertex initNewVertexSum(Vertex vertex, String hiddenVertex) {
		ArrayList<String> vertexes = new ArrayList<String>();

		for (int i = 0; i < vertex.parents.size(); i++) {
			if (!vertex.parents.get(i).equals(hiddenVertex)) {
				vertexes.add(vertex.parents.get(i));
			}
		}

		if (!vertex.id.equals(hiddenVertex)) {
			vertexes.add(vertex.id);
		}

		Vertex verAfteSum = new Vertex(vertexes.get(vertexes.size() - 1));

		for (int i = 0; i < vertexes.size() - 1; i++) {
			verAfteSum.addParent(vertexes.get(i), graph);
		}

		return verAfteSum;
	}


	/*
	 * calculate the factors
	 */
	private ArrayList<Vertex> Factors(String[] hidden, HashMap<String, String> evidences, String vertexQuery) {
		ArrayList<Vertex> myFactors = new ArrayList<Vertex>();
		for (String hiddenVertex : hidden) {
			if (dfsFindAncestor(hiddenVertex, evidences, vertexQuery, new HashSet<String>(Arrays.asList(hiddenVertex)))) {
				myFactors.add(new Vertex(graph.get(hiddenVertex)));				
			}
		}

		myFactors.add(new Vertex(graph.get(vertexQuery)));

		for (String evidence : evidences.keySet()) {
			myFactors.add(new Vertex(graph.get(evidence)));
		}

		for (int i = 0; i < myFactors.size(); i++) { // remove the vertex that we have the info about it
			boolean isOneValued = true;
			ArrayList<String> parents = myFactors.get(i).parents;
			
			if (!evidences.containsKey(myFactors.get(i).id)) {
				isOneValued = false;
			}

			for (int j = 0; j < parents.size() && isOneValued; j++) {
				if (!evidences.containsKey(parents.get(j))) {
					isOneValued = false;
				}
			}

			if (isOneValued) {
				myFactors.remove(i--);
			}
		}
		return myFactors;
	}

	/*
	 * this method check if vertex is ancestor of evidence or vertex target
	 */
	private boolean dfsFindAncestor(String myVertex, HashMap<String, String> evidences, String vertexQuery, HashSet<String> visited) {
		if (conatinInQueryEvidence(myVertex, evidences, vertexQuery)) {
			return true;
		}

		boolean result = false;
		for (String vertexChild : graph.get(myVertex).childs) {
			if (!visited.contains(vertexChild)) {
				visited.add(vertexChild);
				result = result || dfsFindAncestor(vertexChild, evidences, vertexQuery, visited);
				if (result) {
					return true;
				}
			}
		}

		return result;
	}

	private boolean conatinInQueryEvidence(String currentVertex, HashMap<String, String> evidences, String vertexQuery) {
		if (currentVertex.equals(vertexQuery)) {
			return true;
		}

		for (String evidence : evidences.keySet()) {
			if (evidence.equals(currentVertex)) {
				return true;
			}
		}

		return false;
	}

	private int[] findMinimalPair(String hiddens, ArrayList<Vertex> factors, HashMap<String, String> evidences) {
		int[] minInCPT = {Integer.MAX_VALUE, Integer.MAX_VALUE};
		ArrayList<Integer> containsHidden = new ArrayList<Integer>();
		for (int i = 0; i < factors.size(); i++) { // save the hidden vertex from the factors
			if (factors.get(i).id.equals(hiddens)) {
				containsHidden.add(i);
			}
			else {
				for (String parent : factors.get(i).parents) { // save the hidden vertex from the parents of the factor
					if (parent.equals(hiddens)) {
						containsHidden.add(i);
						break;
					}
				}
			}
		}

		if (containsHidden.size() == 0) {
			return null;
		}

		if (containsHidden.size() == 1) {
			int result[] = {containsHidden.get(0)};
			return result;
		}

		int minimals[] = {containsHidden.get(0), containsHidden.get(1)};

		for (int i = 0; i < containsHidden.size() - 1 ; i++) {
			for (int j = i + 1; j < containsHidden.size(); j++) {
				int tempRows[] = expectedRowsAndASCII(factors.get(containsHidden.get(i)), factors.get(containsHidden.get(j)), evidences);
				if (tempRows[0] < minInCPT[0] || (tempRows[0] == minInCPT[0] && tempRows[1] < minInCPT[1])) {
					minInCPT = tempRows;
					minimals[0] = containsHidden.get(i);
					minimals[1] = containsHidden.get(j);
				}
			}
		}

		return minimals;
	}


	private int[] expectedRowsAndASCII(Vertex vertex1, Vertex vertex2, HashMap<String, String> evidences) {
		HashMap<String, Integer> sharedVertexes = sharedVertex(vertex1, vertex2, evidences);

		int result[] = {1, 0};
		for (String vertex : sharedVertexes.keySet()) {
			result[0] *= graph.get(vertex).values.size();

			for (int i = 0; i < vertex.length(); i++) {
				result[1] += vertex.charAt(i);
			}
		}

		return result;
	}

	/*
	 * calculate all the shared vertex
	 */
	HashMap<String, Integer> sharedVertex(Vertex vertexMin1, Vertex vertexMin2, HashMap<String, String> evidences) {
		HashMap<String, Integer> sharedVertexes = new HashMap<String, Integer>();

		if (!evidences.containsKey(vertexMin1.id)) {
			addToMap(vertexMin1.id, sharedVertexes);
		}

		if (!evidences.containsKey(vertexMin2.id)) {
			addToMap(vertexMin2.id, sharedVertexes);
		}

		for (String parent : vertexMin1.parents) {
			if (!evidences.containsKey(parent)) {
				addToMap(parent, sharedVertexes);
			}
		}

		for (String parent : vertexMin2.parents) {
			if (!evidences.containsKey(parent)) {
				addToMap(parent, sharedVertexes);
			}
		}

		return sharedVertexes;
	}

	private void addToMap(String name, HashMap<String, Integer> sharedVertexes) {
		if (sharedVertexes.containsKey(name)) {
			sharedVertexes.put(name, sharedVertexes.get(name) + 1);
		}
		else {
			sharedVertexes.put(name, 1);
		}
	}
}
