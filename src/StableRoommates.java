import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * 
 * @authors Robyn MacDonald, Ryan Lampe
 * 
 * This program solves instances of the stable roommates problem
 * by printing out a stable matching, or if no such matching exists, 
 * will output false.
 * 
 * See: http://www.dcs.gla.ac.uk/~pat/jchoco/roommates/papers/Comp_sdarticle.pdf
 * for article reference
 *
 */
public class StableRoommates {

	/*
	 * The Pascal Implementation utilizes global variables. To parallel this 
	 * approach in Java, we have created these variables as apart of the class instantiation.
	 * Thus when we create our instance, these variables will be accessible anywhere.
	 */
	//OUR "GLOBAL VARIABLES" FROM PASCAL IMPLEMENTATION - 
	static int MAX_SIZE = 91;
	
	public Integer[][] rankMatrix = null;
	public Integer[][] preferenceMatrix = null;
	public Integer[][] lsrMatrix = null;
	public LinkedList<Integer>[] reducedPMatrix = null;
	public int numPeople;
	public boolean solnPossible = true;
	public boolean solnFound = false;
		
	public int firstInCycle;
	//Note: stores person, not index
	public int firstRepeat;
	public ArrayList<Integer> pCycle = new ArrayList<>();
	public ArrayList<Integer> qCycle = new ArrayList<>();
	public Integer[] positionInCycle;
	
	//Ensure output matches input format
	public boolean wasOneIndexed; 
	
	
	
	/**
	 * The constructor is responsible for developing a class instance to use based on the input 
	 * preferences matrix. 
	 * 
	 * A check to see if changing the index base of the preference matrix is done.
	 * 
	 * Initialize the LSR Matrix for the problem instance. 
	 * 
	 * Create the Rank Matrix for the problem instance.
	 * 
	 * The first phase reduction is then performed. 
	 * @param pMatrix
	 */
	public StableRoommates(Integer[][] pMatrix) {
		this.preferenceMatrix = pMatrix;
		//check the first row to see if there is an entry equal to the size of the matrix
		this.wasOneIndexed = false;
		for(int i = 0; i < pMatrix.length; i++) {
			if(pMatrix[0][i] == pMatrix.length) {
				this.wasOneIndexed = true;
			}
		}
		if(this.wasOneIndexed ) {
			this.zeroifyMatrix();
		}
		
		
		this.numPeople = pMatrix.length;
		this.positionInCycle = new Integer[this.numPeople];
		
		this.lsrMatrix = buildLRMatrix(pMatrix.length);
		this.rankMatrix = buildRankMatrix(pMatrix);
		
		this.reducedPMatrix = this.phase1Reduction();
		
		//Set default
		this.firstInCycle = -1;
	}
	
	
	
	
	
	/**
	 * The algorithm is designed for 0 indexed arrays and matrices, and thus any inputs that 
	 * are one based must be converted to 0 index form. The resulting assignments will then be
	 * converted back to one based pairings to match the input form. 
	 * @param matrix
	 */
	public void zeroifyMatrix() {
		for(int i = 0; i < this.preferenceMatrix.length; i ++) {
			for(int j = 0; j < this.preferenceMatrix.length; j++) {
				this.preferenceMatrix[i][j] -=1;
			}
		}
		
		return;
	}
	
	
	
	
	
	/**
	 * This method creates and initializes a 3 Column LSR Matrix for based on a preference matrix. 
	 * 
	 * The LSR Matrix is initialized based on the size of the preference matrix. The LSR Matrix is used in the 
	 * phase one reduction (proposal stage) of the algorithm to keep track of the reduction
	 * of the preference lists. The first, second, and third columns represent the positions of the best, second best, and worst 
	 * potential partners that each person (row) has in their current reduced preference list respectively. 
	 * 
	 * See page 587 for more information. 
	 * 
	 * 
	 * @param pMatrix
	 * @return
	 */
	public Integer[][] buildLRMatrix(int numPeople){
		Integer[][] lrMatrix= new Integer[numPeople][3] ;
		
		for(int i = 0; i < numPeople; i++) {
			lrMatrix[i][2] = numPeople - 1; //set the right index to the maximum index
			lrMatrix[i][0] = 0; //should initialize to 0 by default, but included for cross language implementation
			lrMatrix[i][1] = null;  //The Pascal implementation sets this later. DOULBE CHECK THIS
		}
		return lrMatrix;
	}
	
	/**
	 * This method takes in a preference matrix (assumed to include sentinels) 
	 * and returns the corresponding rank matrix. 
	 * 
	 * A rank matrix is created to allow for rapid comparisons between alternative choices for an individual.
	 * See page 587 for more information. 
	 * 
	 * 
	 * @param pMatrix
	 * @return
	 */
	public Integer[][] buildRankMatrix(Integer[][] pMatrix){
		
		int numPeople = pMatrix.length; 
		//number of people, same as num preferences b/c including sentinels
		
		Integer[][] rankMatrix = new Integer[numPeople][numPeople];
		
		for(int i = 0; i < numPeople; i++) {
			for(int j = 0; j < numPeople; j++) {
				rankMatrix[i][pMatrix[i][j] ] = j;
			}
		}
		return rankMatrix;
	}
	
	
	
	
	
	
	/**
	 * This method performs the phase one reduction on the object instance 
	 * and returns the resulting "reduced list" in the form of an array of linked lists
	 * where each index in the array represents a person, and the list attached to that index is the respective
	 * persons reduced preference list. 
	 * 
	 * See page 579
	 * 
	 * @return
	 */
	public LinkedList<Integer>[] phase1Reduction() {
		
		//Perform Proposals
		if(!this.doProposals()) {
			//A stable matching is not possible
			return null;
		}
		
		//Generate linked list based on the proposals and left/right preference locations 
		
		//Create array of linked lists: 
		@SuppressWarnings("unchecked") LinkedList<Integer>[] reducedPMatrix = new LinkedList[this.numPeople];

		for(int i = 0; i < this.numPeople; i++) {
			//Initialize each list
			reducedPMatrix[i] = new LinkedList<Integer>();
			for(int j = 0; j < this.numPeople; j++) {
				//See Corollaries 1.2 and 1.3
				if(!( j > this.lsrMatrix[i][2]  || this.lsrMatrix[this.preferenceMatrix[i][j] ] [2] < this.rankMatrix[this.preferenceMatrix[i][j]][i])) {
					reducedPMatrix[i].add(this.preferenceMatrix[i][j]);
				}
			}
		}
		
		return reducedPMatrix;
		
		
	}
	
	
	/**
	 * This method performs the series of proposals to determine the left/right preferences of 
	 * the LSR matrix.
	 * 
	 * 	The return value is used to determine if the instance is certain to have no stable matching (false)
	 * 	or that a stable matching may still be possible (true). 
	 *  See Corollary 1.1
	 *  
	 * @return
	 */
	public boolean doProposals() {
		//Create set to keep track of who has proposed so far
        Set<Integer> hasProposed = new HashSet<Integer>(); 
		
		int proposer = -1; //stop eclipse from complaining
		int nextChoice = -1;
		int current; 
		
		//want to give everyone in the list a chance to make a proposal
		for(int i = 0; i < this.numPeople; i++) {
			proposer = i;
			
			do {
				//proposer chooses their next choice 
				nextChoice = this.preferenceMatrix[proposer][ this.lsrMatrix[proposer][0] ] ;
				current = this.preferenceMatrix[nextChoice][this.lsrMatrix[nextChoice][2]];
				
				while(this.rankMatrix[nextChoice][proposer] > this.rankMatrix[nextChoice][current] ) {
					//the proposer is rejected by their next choice
					
					//update the proposer's left index
					this.lsrMatrix[proposer][0] += 1;
					//update next choice
					nextChoice = this.preferenceMatrix[proposer][this.lsrMatrix[proposer ][0]];
					current = this.preferenceMatrix[nextChoice][this.lsrMatrix[nextChoice][2]];
				}
				//next choice holds the proposer, and rejects current 
				this.lsrMatrix[nextChoice][2] = this.rankMatrix[nextChoice][proposer];
				proposer = current;
			} while( hasProposed.contains(nextChoice) );//while the current next choice object hasn't made a proposal
			hasProposed.add(nextChoice);
		}
		boolean possibleSoln = (proposer == nextChoice); //check if the next choice is their-self
			this.solnPossible = possibleSoln;
			return solnPossible;
	}
	
	/**
	 * This method iterates over the current stage of the reducedPmatrix LinkedList and checks if each head is equal to the 
	 * index of which list it is and if so, then it updates solnPossible to show that there is no possible solution because 
	 * the person's only remaining possible partner is his/her self.
	 * 
	 * See Corollary 1.2
	 */
	public void checkSolnPossible() {
		if(this.solnPossible == false) {
			return;
		}
		for(int i = 0; i < this.numPeople; i++) {
			if(this.reducedPMatrix[i].getFirst() == i) {
				this.solnPossible = false;
			}
		}
		return;
	}
		
	/**
	 * This method finds the first instance at which a person still has more than one
	 * possible partner and updates them to be the new first in the cycle
	 * 
	 * See PASCAL : procedure find
	 */
	public void findFirstUnmatched() {
		this.firstInCycle = 0;
		while(this.reducedPMatrix[this.firstInCycle].size() == 1) {
			this.firstInCycle++;
			if(this.firstInCycle == this.numPeople) {
				//we have a soln!
				this.solnFound = true;
				return;
			}
		}
		return;
	}
	
	/**
	 * this method generates the two cycles p and q where q is made up of the second potential partners of
	 * each p and p is made up of the last potential partner of the prior q
	 */
	public void findCycle() {
		int pTerm = this.firstInCycle;
		int count = 0;
		//while loop until an element repeats in p
		while(!(this.pCycle.contains(pTerm))) {
			this.pCycle.add(pTerm);
			//keep track of where each person is in the cycle we create
			this.positionInCycle[pTerm] = count;
			count++;
			//the next term to add to q will be the second term in pTerm's potential partners
			int qTerm = this.reducedPMatrix[pTerm].get(1);
			this.qCycle.add(qTerm);
			//update pTerm to be last potential partner in qTerm
			pTerm = this.reducedPMatrix[qTerm].getLast();
		}
		//Note the above is the person that repeated, not an index. 
		this.firstRepeat = pTerm;

		return;
	}
	
	
	/**
	 * this method uses the two sequences p and q and forces each element in q to reject the proposal it has
	 * this implements one iteration of the phase 2 reduction
	 * 
	 * 
	 * 	Make sure that when q rejects p, we go to ps new first choice (ie what used to be their backup) and delete everything in 
	 *  the backups reduced list that happens to be after p. (ie after square)
	 * 
	 */

	@SuppressWarnings("deprecation")
	//Need the above to be able to remove based on an object location not an index (but our objects are ints
	//so java likes to get upset)
	public void phase2Reduction() {
		//check if we know there isn't a possible soln:
		if(this.pCycle.size() == 1) {
			this.solnPossible = false;
			return;
		}else {
			//perform rejections
			//start at 1 b/c pi reject q_i - 1;
			//can't start at one, needs to start where the cycle starts. 
			//set i to the index in the p Cycle list that is the first 
			//person that gets repeated
			int indexOfFirstPersonOfCycle = this.positionInCycle[this.firstRepeat];
			
			for(int j = indexOfFirstPersonOfCycle + 1; j < this.pCycle.size(); j++) {
				//delete each from each others lists...
				
				//store the VALUE of the current person (ie not their index)
				int currentPersonP = this.pCycle.get(j);
				this.reducedPMatrix[currentPersonP].remove(new Integer(this.qCycle.get(j - 1))); //want to remove the value of q from the reduced list of p, not the index. 
			
				//then also, remove p from q's list
				this.reducedPMatrix[this.qCycle.get(j - 1)].remove(new Integer(currentPersonP));
				//ie, when P is forced to take their second choice, we remove
				//anyone after p on the second choice persons reduced list
				//who has become the first person b/c we made the first reject them
				
				//Quick check to see if soln isn't possible
				if(this.reducedPMatrix[currentPersonP].size() == 0) {
					this.solnPossible = false;
					return;
				}
				int psSecondChoice = this.reducedPMatrix[currentPersonP].get(0);
				//remove anything after them
				while(this.reducedPMatrix[psSecondChoice].getLast() != currentPersonP) {
					this.reducedPMatrix[psSecondChoice].removeLast();
				}
			}
			
			/*
			 * Chan - Don't worry about this, its a revision history...
			 * 
			 * when running this and removing the people that were in person 5 (for 1 indexed)
			we remove one of the elements that is also supposed to get removed by our sequence
			so probably we will need to have another check in our removal algorithm to make
			certain that we are only popping off the first/last indices if the person we 
			want to remove is still actually in that list 
			*/
			
			//Have to do the first p that is actually in cycle getting rejected by last q because we don't hold the last p in the set
			this.reducedPMatrix[pCycle.get(indexOfFirstPersonOfCycle)].remove(new Integer(this.qCycle.get(qCycle.size() - 1)));
			//remove the q from p
			//above changed due to while loop inside of above for loop potentially already deleting p
			int lastPersonInPCycle = this.pCycle.get(indexOfFirstPersonOfCycle);
			int lastPersonInQCycle = this.qCycle.get(this.qCycle.size() - 1);
			this.reducedPMatrix[lastPersonInQCycle].remove(new Integer(lastPersonInPCycle));
			
			//do the same while loop for their second:
			
			int psSecondChoice = this.reducedPMatrix[lastPersonInPCycle].get(0);
			//remove anything after them
			//IE Corollary 1.3 (Ref'd page 584)
			while(this.reducedPMatrix[psSecondChoice].getLast() != lastPersonInPCycle) {
				this.reducedPMatrix[psSecondChoice].removeLast();
			}
			//update the first in cycle to be the tail of this guy
			
			if(this.positionInCycle[this.firstRepeat] > 0) {
				this.firstInCycle = this.pCycle.get(this.positionInCycle[this.firstRepeat] - 1);
			}else {
				this.findFirstUnmatched();
			}			
		}
	}
	
	
	
	
	public void findSolution() {
		//First set all the second preferences based on linked list 
		for(int i = 0; i < this.numPeople; i++) {
			//actually just set it to two
			this.lsrMatrix[i][1] = 1;
		}
		
		this.findFirstUnmatched();
		
		while(this.solnPossible && !this.checkIfSolnFound()) {
			//see if we found a soln
			this.checkIfSolnFound();
			
			//empty p and q
			this.pCycle.clear();
			this.qCycle.clear();
			
			this.findCycle();
			if(this.pCycle.size() == 1) {
				//there is no solution
				this.solnPossible = false;
				return;
			}
			//do phase two reduction
			this.phase2Reduction();
			this.checkSolnPossible();
		}
		return;
	}
	
	
	/**
	 * Based on Lemma four (Page 585)
	 * @return
	 */
	public boolean checkIfSolnFound() {
		
		this.solnFound = true;
		for(int i = 0; i < this.numPeople; i++) {
			if(this.reducedPMatrix[i].size() != 1) {
				this.solnFound = false;
				return false;
			}
		}
		return true;
	}
	
	
	
	public void printSolution() {
		System.out.println("A stable pairing has been found!\nThe assignments are:\n");
		int j = 0;
		if(this.wasOneIndexed) {
			j++;
		}
		for(int i = 0; i < this.numPeople; i++) {
			System.out.printf("%2d is paired with: %2d\n", i + j, this.reducedPMatrix[i].get(0) + j);
		}
	}
	
	public static void printNoSolution() {
		System.out.println("There is no stable matching for these preferences.\n");
		return;
	}
	
	
	
	public static void main(String[] args) {
		//input matrices should be n by n! 
		
		//Test from page 582
		Integer[][] testPMatrix = new Integer[][] {
			{4, 6, 2, 5, 3, 1},
			{6, 3, 5, 1, 4, 2},
			{4, 5, 1, 6, 2, 3},
			{2, 6, 5, 1, 3, 4},
			{4, 2, 3, 6, 1, 5},
			{5, 1, 4, 2, 3, 6}
		};
		long startTime1 = System.nanoTime();
		
		//create the object
		StableRoommates instance1 = new StableRoommates(testPMatrix);
		
		//DEBUG and GENERAL TESTING OF INDIVIDUAL METHODS SO FAR
		//Pause here in debugger to look at values
		/*System.out.println("Testing\n\nNumPeople...");
		if(instance1.numPeople == 6) {
			System.out.println("Passed\n");
		}else {
			System.out.println("FAILURE");
		}
		
		System.out.println("Reduced preferences matrix:\nExpected:");
		System.out.println("[5], [2, 4, 3,], [4, 1], [1, 4], [3, 1, 2], [0]");
		System.out.println("Acutal\n" + Arrays.deepToString(instance1.reducedPMatrix));
		
		
		System.out.println("\nISR Matrix: -- Expeceted:\n(1,1)\t\t(1,4),\t\t(1,4),\t\t(0,2),\t\t(0,2),\t\t(1,1)");
		System.out.println(Arrays.deepToString(instance1.lsrMatrix));
		
		System.out.println("\nRank Matrix: -- Expected:\n 1:  6, 3, 5, 1, 4, 2\n2:  4, 6, 2, 5, 3, 1\n3"
				+ "3, 5, 6, 1, 2, 5\n4:   4, 1, 5, 6, 3, 2\n5:  5, 2, 3, 1, 6, 5\n6:  2, 4, 5, 3, 1, 6\n");
		System.out.println(Arrays.deepToString(instance1.rankMatrix));
		*/
		
		//Run phase two reduction until we either make it work or know it can't be done
		System.out.println("\nTest 1\nTesting on a known solvable preferences matrix:");
		instance1.findSolution();
		
		
		//Don't count time taken to print the solution:
		long elapsedTime1 = System.nanoTime() - startTime1;
		if(instance1.solnFound) {
			instance1.printSolution();
		}else {
			StableRoommates.printNoSolution();
		}
		System.out.printf("The runtime of this instance (size 6) was: %d nanoseconds\n", elapsedTime1);
		
		
		
		
		
		//Test number 2: Known to have no soln:
		//test from page 586
		Integer[][] testPMatrix2 = new Integer[][] {
			{2, 6, 4, 3, 5, 1},
			{3, 5, 1, 6, 4, 2},
			{1, 6, 2, 5, 4, 3},
			{5, 2, 3, 6, 1, 4},
			{6, 1, 3, 4, 2, 5},
			{4, 2, 5, 1, 3, 6}
		};
		
		long startTime2 = System.nanoTime();

		StableRoommates instance2 = new StableRoommates(testPMatrix2);
		instance2.findSolution();
		long elapsedTime2 = System.nanoTime() - startTime2;

		System.out.println("\nTest 2:\nTesting on a known no soln- preferences matrix:");

		if(instance2.solnFound) {
			instance2.printSolution();
		}else {
			StableRoommates.printNoSolution();
		}
		System.out.printf("The runtime of this instance (size 6) was: %d nanoseconds\n", elapsedTime2);

		
		
		//Test 3 - Page 579 - Several stable pairings. 
		Integer[][] testPMatrix3 = new Integer[][] {
			{2, 5, 4, 6, 7, 8, 3, 1},
			{3, 6, 1, 7, 8, 5, 4, 2},
			{4, 7, 2, 8, 5, 6, 1, 3},
			{1, 8, 3, 5, 6, 7, 2, 4},
			{6, 1, 8, 2, 3, 4, 7, 5},
			{7, 2, 5, 3, 4, 1, 8, 6},
			{8, 3, 6, 4, 1, 2, 5, 7},
			{5, 4, 7, 1, 2, 3, 6, 8}
		};
		long startTime3 = System.nanoTime();

		StableRoommates instance3 = new StableRoommates(testPMatrix3);
		instance3.findSolution();
		long elapsedTime3 = System.nanoTime() - startTime3;

		System.out.println("\nTest 3:\nTesting on a known several soln- preferences matrix:");
		
		if(instance3.solnFound) {
			instance3.printSolution();
		}else {
			StableRoommates.printNoSolution();
		}
		System.out.printf("The runtime of this instance (size 8) was: %d nanoseconds\n", elapsedTime3);

	}
	
	
}
