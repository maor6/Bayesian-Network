import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;



public class Ex1 {

	public static void build(HashMap<String, Vertex> graph) {
		File input = new File("input.txt");
		try {
			Scanner reader = new Scanner(input);
			reader.nextLine(); // skip the line "Network"
			String pointer = "";
			HashMap<Integer, String> pos = new HashMap<Integer, String>(); // represent the arrange of the vertex by the file read
			pointer = reader.nextLine();

			for (int i = 0; i < pointer.length()-1; i++) {
				if (pointer.charAt(i) == ' ') {
					pointer = pointer.substring(i+1, pointer.length());
					i = pointer.length();
				}
			}

			int position = 0;

			for (String ver : pointer.split(",")) { // add all the vertexes to the graph
				Vertex t = new Vertex(ver);
				t.indexOnGrapg = position;
				graph.put(ver, t);
				pos.put(position++, ver);
			}

			while (!pointer.contains("Queries")) {
				pointer = reader.nextLine();

				if (pointer.contains("Var")) {
					String ver = pointer.split(" ")[1];
					Vertex vertex = graph.get(ver);
					vertex.setValues(reader.nextLine().split(" ")[1]); // add values
					pointer = reader.nextLine().split(" ")[1];

					if (!pointer.contains("none")) {
						vertex.setParents(pointer, graph);
					}

					while (!pointer.contains("CPT")) { // skip to CPT
						pointer = reader.nextLine();
					}

					while (!pointer.isEmpty()) { // read and build the CPT 
						pointer = reader.nextLine();
						vertex.setCPT(pointer);
					}
				}
			}

			int countSum = 0;
			int countMul = 0;
			double answer = 0;
			String outputs = ""; // save each query
			String targetVertex = ""; // the target vertex
			String vertexAndValue = "";
			String numberQuery = "";
			int startGone = 0;
			ArrayList<Vertex> gones = new ArrayList<>(); // save all the hidden in the queries
			HashMap <String, String> inQuery = new HashMap<String, String>(); // save all the vertex that in the query
			while (reader.hasNextLine()) { // read queries
				pointer = reader.nextLine();

				for (int i = 0; i < pointer.length(); i++) {
					if (pointer.charAt(i) == '|') {
						targetVertex = pointer.substring(2,i).split("=")[0];
						vertexAndValue = pointer.substring(2,i);
						inQuery.put(pointer.substring(2,i).split("=")[0], pointer.substring(2,i).split("=")[1]);						
						startGone = i+1;
					}

					if (pointer.charAt(i) == ')') { // all the evidence
						String[] evidence = pointer.substring(startGone, i).split(",");
						for (String string : evidence) {
							inQuery.put(string.split("=")[0], string.split("=")[1]);
						}
					}
				}

				for (int i =0; i < pos.size(); i++) { // to know which vertex are hidden
					if (!inQuery.containsKey(pos.get(i))) {
						gones.add(graph.get(pos.get(i)));
					}
				}

				if (pointer.length() != 0) { // not have empty lines at the end of the file
					numberQuery = pointer.charAt(pointer.length()-1) +""; // get which query is it, can be 1,2,3

					inQuery.remove(targetVertex);

					if (numberQuery.contains("1")) { // if the query is number 1
						Vertex tempVertex = graph.get(targetVertex); // the target vertex
						String[][] mat = getHidden(gones);
						boolean parentsInQuery = true; // represent if need to calculate or not

						if (inQuery.size() == tempVertex.parents.size()) {
							for (String parent : tempVertex.parents) { // check if i have my all parents in the query
								if (inQuery.get(parent) == null) {
									parentsInQuery = false; // i have the answer in the CPT
								}
							}
						}
						else {
							parentsInQuery = false;
						}


						if (parentsInQuery) { // the answer in the CPT  
							boolean currectRow = true;
							for (int i = 0; i < tempVertex.cpt.size(); i++) {
								if (tempVertex.cpt.get(i)[tempVertex.parents.size()].equals(vertexAndValue.split("=")[1])) {
									for (int j = 0; j < graph.get(targetVertex).parents.size(); j++) {
										if (!tempVertex.cpt.get(i)[j].equals(inQuery.get(tempVertex.parents.get(j)))) {
											currectRow = false;
										}
									}

									if (currectRow) {
										answer =  Double.parseDouble(tempVertex.cpt.get(i)[tempVertex.cpt.get(i).length-1]);
									}
									currectRow = true;
								}
							}

						}
						else { // need to calculate
							inQuery.put(targetVertex, vertexAndValue.split("=")[1]);
							answer = question1(inQuery,mat, gones, pos, graph);
							int sizeTargetValues = graph.get(targetVertex).values.size();
							countSum = (mat.length-1) * sizeTargetValues + sizeTargetValues - 1;
							countMul = mat.length * (graph.size()-1) * sizeTargetValues;
							gones.add(tempVertex);
							inQuery.remove(targetVertex);
							mat = getHidden(gones);
							answer /= question1(inQuery, mat, gones, pos, graph);
						}
					}

					if (numberQuery.contains("2")) { // if the query is number 2
						VariableEliminationAlgo elimination = new VariableEliminationAlgo(graph);
						answer = elimination.getQueryResult(pointer, gones, false); // false is not heuristic
						countSum = elimination.countSum;
						countMul = elimination.countMul;
					}

					if (numberQuery.contains("3")) { // if the query is number 3
						VariableEliminationAlgo elimination = new VariableEliminationAlgo(graph);
						answer = elimination.getQueryResult(pointer, gones, true); // true is heuristic	
						countSum = elimination.countSum;
						countMul = elimination.countMul;
					}

					// round the number to 5 digit after decimal
					outputs += String.format("%.5f", answer);
					outputs += "," + countSum;
					outputs += "," + countMul;

					// clear for new query
					gones.clear();
					inQuery.clear();
					countSum = 0;
					countMul = 0;
					outputs += "\n";
				}
			}

			//System.out.println(outputs);
			reader.close();

			// make file output and write there the answers
			File out = new File("output.txt");
			FileWriter write = new FileWriter(out);
			write.write(outputs);
			write.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private static double question1(HashMap<String, String> inQuery, String[][] mat, ArrayList<Vertex> gones,
			HashMap<Integer, String> pos, HashMap<String, Vertex> graph) {
		String[][] tableQuerie = new String[mat.length][inQuery.size() + gones.size()];
		int row = 0;
		Vertex v = null;

		for (int i = 0; i < tableQuerie.length; i++) {
			for (int j = 0; j < tableQuerie[i].length; j++) {
				String t = pos.get(j);
				v = graph.get(t);
				if (inQuery.containsKey(t)) {
					tableQuerie[i][v.indexOnGrapg] = inQuery.get(t);
				}

				if (gones.contains(v)) {
					tableQuerie[i][v.indexOnGrapg] = mat[row][gones.indexOf(v)];
				}
			}
			row++;
		}

		double resCase = 1;
		double ans = 0;
		boolean match = true;
		for (int i = 0; i < tableQuerie.length; i++) {
			for (int j = 0; j < tableQuerie[i].length; j++) {
				Vertex currentVertex = graph.get(pos.get(j)); // get the current Vertex
				if (currentVertex.parents.size() > 0) {
					for (int r = 0; r < currentVertex.cpt.size(); r++) {
						if (tableQuerie[i][currentVertex.indexOnGrapg].equals(currentVertex.cpt.get(r)[currentVertex.parents.size()])) {
							for (int k = 0; k < currentVertex.parents.size(); k++) {
								Vertex parentVer = graph.get((currentVertex.parents.get(k))); // current parent
								if (!currentVertex.cpt.get(r)[k].equals(tableQuerie[i][parentVer.indexOnGrapg])) {
									match = false;
									break;
								}
							}

							if (match) {
								resCase *= Double.parseDouble(currentVertex.cpt.get(r)[currentVertex.cpt.get(r).length-1]);
								//countMul++;
							}
							match = true;
						}

					}
				}
				else { // not have parents
					for (int k = 0; k < currentVertex.cpt.size(); k++) {
						if (currentVertex.cpt.get(k)[0].equals(tableQuerie[i][j])) {
							resCase *= Double.parseDouble(currentVertex.cpt.get(k)[1]);
						}
					}
				}
			}
			ans += resCase;
			resCase = 1;
		}

		return ans;
	}

	private static String[][] getHidden(ArrayList<Vertex> gones) {
		int solutions = 1;

		for(int i = 0; i < gones.size(); solutions *= gones.get(i).values.size(), i++); // to know how many time we have
		String[][] mat = new String[solutions][gones.size()];
		int col = 0;
		for(int i = 0; i < solutions; i++) {
			int j = 1;
			for(Vertex set : gones) {
				mat[i][col++] = set.values.get((i/j) % set.values.size());
				j *= set.values.size();
			}
			col = 0;
		}
		return mat;
	}


	public static void main(String[] args) {
		HashMap<String, Vertex> graph = new HashMap<>();
		build(graph);
	}

}
