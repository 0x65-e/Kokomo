package california.surf.util;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Supplier;

/**
 * Parameterized classification/regression tree implementation in Java
 * @author CaliforniaCraig
 *
 * @param <T> Value or class of a data point
 */
public class RegressionTree<T extends Supplier<Double>> {
	/**
	 * Internal nodes used to hold data and determine feature splits
	 * @author FacelessHacker
	 */
	private static class Node<T extends Supplier<Double>> {
		// Parent
		private final Node<T> parent;
		private final String name;
		
		private boolean leaf;
		
		/////////////////////////////
		// For Leaf nodes
		/////////////////////////////
		
		// Default number of samples to hold before splitting
		public static final int SAMPLES = 50;
		// Feature sets for each data point
		private double[][] data;
		private int size;
		// The number of features to track and split on
		private int dimensions;
		// The values (outcomes) such that classifications[i] corresponds to the feature set data[i][]
		private Object[] classifications;
		// Priority queues for sorting data by indexes as it comes in
		private PriorityQueue<Integer>[] sorts;
		// Overall sum of each feature, used for calculating a split index for each feature
		private double leftSE, leftMean, leftMeanOld;
		
		
		/**
		 * Class to compare data indexes on a specific feature.
		 * Highest priority goes to the largest element.
		 */
		private class IndexComparator implements Comparator<Integer> {
			int dimension;
			
			public IndexComparator(int dimension) {
				this.dimension = dimension;
			}
			
			public int compare(Integer i1, Integer i2) {
				if (data[i1][dimension] > data[i2][dimension]) return -1;
				if (data[i1][dimension] < data[i2][dimension]) return 1;
				return 0;
			}
		}
		
		
		/////////////////////////////
		// For Stem nodes
		/////////////////////////////
		
		// Subtrees
		private Node<T> left, right;
		// Split parameters
		private int splitDimension;
		private double splitValue;
		
		/**
		 * Create a new leaf node (not root)
		 * @param parent The parent node of this leaf node
		 */
		@SuppressWarnings("unchecked")
		private Node(Node<T> parent) {
			this.parent = parent;
			name = GameUtils.getSaltString(7);
			dimensions = parent.dimensions;
			int initSize = Math.max(SAMPLES, parent.size);
			this.data = new double[initSize][];
			this.classifications = new Object[initSize];
			size = 0;
			leaf = true;
			
			// Statistics for calculating SE over the entire data set
			leftSE = 0;
			leftMean = 0;
			leftMeanOld = 0;
			
			// Set up the PriorityQueues to sort indices
			sorts = (PriorityQueue<Integer>[])new PriorityQueue[dimensions];
			for (int i = 0; i < dimensions; i++) {
				sorts[i] = new PriorityQueue<>(initSize, new IndexComparator(i));
			}
		}
		
		/**
		 * Create the root node of a tree
		 * @param dimensions The number of features being collected, which can be split on
		 */
		@SuppressWarnings("unchecked")
		private Node(int dimensions) {
			parent = null;
			this.name = "root";
			this.dimensions = dimensions;
			this.data = new double[SAMPLES][];
			this.classifications = new Object[SAMPLES];
			size = 0;
			// The root node is a leaf node to start
			leaf = true;
			
			// Statistics for calculating SE over the entire data set
			leftSE = 0;
			leftMean = 0;
			leftMeanOld = 0;
			
			// Set up the PriorityQueues to sort data
			sorts = (PriorityQueue<Integer>[])new PriorityQueue[dimensions];
			for (int i = 0; i < dimensions; i++) {
				sorts[i] = new PriorityQueue<>(SAMPLES, new IndexComparator(i));
			}
		}
		
		public void addAndSort(double[] point, T value) {
			System.out.println("Adding data to node " + name);
			// Add to the arrays
			data[size] = point;
			classifications[size] = value;
			
			// Update the SE if all the values are on the left
			leftMean = leftMeanOld + (value.get() - leftMeanOld) / (size+1);
            leftSE += (value.get() - leftMeanOld) * (value.get() - leftMean);
                
            leftMeanOld = leftMean;
			
			// Update priority sorting for each dimension
			for (int i = 0; i < dimensions; i++) {
				sorts[i].add(size);
			}
			size++;
			
		}
		
	}
	
	private Node<T> root;
	
	public RegressionTree(int dimensions) {
		root = new Node<T>(dimensions);
		
	}
	
	/**
	 * Add the given data array and corresponding value to the tree, updating and splitting where required
	 * @param data Array of doubles representing the features of this point
	 * @param value T representing the outcome of this point
	 */
	@SuppressWarnings("unchecked")
	public void add(double[] data, T value) {
		System.out.println("Adding data to tree");
		Node<T> curr = root;
		
		while (!curr.leaf || curr.size >= curr.data.length) { // Move this to Node class instead?
			// A leaf node that needs to split
			if (curr.leaf) {
				System.out.println("Splitting Node " + curr.name);
				// Find the axis to split on
				int splitIndex = 0;
				double splitValue = Double.POSITIVE_INFINITY;
				double bestSE = curr.leftSE;
				
				for (int dim = 0; dim < curr.dimensions; dim++) {
					double leftSE = curr.leftSE, leftMean = curr.leftMean, leftMeanOld = curr.leftMeanOld;
					double rightSE = 0, rightMean = 0, rightMeanOld = 0;
					double sumSE = 0;
					int sizeRight = 0, sizeLeft = curr.size;
					
					// Current order for this dimension
					PriorityQueue<Integer> sort = curr.sorts[dim];
					
					// Calculate iterative squared errors for every split point using Welford's algorithm
					while (sort.size() > 0) {
						// Get the next value to change from left to right
						double val = ((T)curr.classifications[sort.poll()]).get();
						
						// Update the right SE
						rightMean = rightMeanOld + (val - rightMeanOld) / ++sizeRight;
			            rightSE += (val - rightMeanOld) * (val - rightMean);
			            rightMeanOld = rightMean;
			            
			            // Update the left SE
			            leftMean = (sizeLeft * leftMeanOld - val) / (--sizeLeft);
			            leftSE -= (val - leftMeanOld) * (val - leftMean);
			            leftMeanOld = leftMean;
			            
			            // Reassign split index if better
						sumSE = leftSE + rightSE; // Is the sum of the left and right SE the best metric? What about min of the two?
			            if (sumSE < bestSE) {
							bestSE = sumSE;
							splitIndex = dim;
							splitValue = val;
							System.out.println("Better index found: " + splitIndex + " left,right=" + sizeLeft + "," + sizeRight + 
									" bestSE=" + bestSE);
						}
					}
					
					
				}
				
				// Don't split if all the entries are the same - just double the array size
				if (splitValue == Double.POSITIVE_INFINITY
						|| splitValue == Double.NEGATIVE_INFINITY
						|| splitValue == Double.NaN) {
					double[][] newData = new double[curr.data.length * 2][];
					System.arraycopy(curr.data, 0, newData, 0, curr.size);
					curr.data = newData;
					Object[] newClass = new Object[newData.length];
					System.arraycopy(curr.classifications, 0, newClass, 0, curr.size);
					curr.classifications = newClass; // This line was previously missing, could this be the source of errors?
					
					// Add back in the indices, since they were removed when finding the best split value
					for (int dim = 0; dim < curr.dimensions; dim++) {
						for (int i = 0; i < curr.size; i++) {
							curr.sorts[dim].add(i);
						}
					}
					break;
				}
				
				// Split the leaf node
				Node<T> left = new Node<T>(curr);
				Node<T> right = new Node<T>(curr);
				
				System.out.println("Splitting on index " + splitIndex + ", value=" + splitValue);
				System.out.println("        Node " + curr.name);
				System.out.println("             /\\");
				System.out.println("Node " + left.name + "    Node " + right.name);
				
				// Sort into left and right nodes
				for (int i = 0; i < curr.size; i++) {
					double[] point = curr.data[i];
					T val = (T)curr.classifications[i];
					if (point[splitIndex] < splitValue) {
						left.addAndSort(point, val);
					} else {
						right.addAndSort(point, val);
					}
				}
				
				// Update this node to a stem
				curr.left = left;
				curr.right = right;
				curr.splitDimension = splitIndex;
				curr.splitValue = splitValue;
				curr.leaf = false;
				curr.size = 0;
				// Clear out old leaf memory contents to allow GC to do its job
				curr.data = null;
				curr.sorts = null;
				curr.classifications = null;
			}
			
			System.out.print("Node " + curr.name + "->");
			// Update the cursor to one of the leaf nodes
			if (data[curr.splitDimension] < curr.splitValue) {
				curr = curr.left;
			} else {
				curr = curr.right;
			}
			System.out.println("Node " + curr.name);
			
		}
		
		curr.addAndSort(data, value);
	}
	
	/**
	 * Get the range of classifications based on the information in data
	 * @param data Values to descend the tree based on
	 * @return Array of all T in the bottom-most leaf node that data directs to
	 */
	@SuppressWarnings("unchecked")
	public double[] getClassification(double[] data) {
		Node<T> curr = root;
		
		while (!curr.leaf) {
			if (data[curr.splitDimension] < curr.splitValue) {
				curr = curr.left;
			} else {
				curr = curr.right;
			}
		}
		
		double[] out = new double[curr.size];
		for (int i = 0; i < out.length; i++) { // Replace with sys.memcopy
			out[i] = ((T)curr.classifications[i]).get();
		}
		
		return out;
	}
	
}

