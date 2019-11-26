import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * 
 * @author rjlam, Robyn MacDonald
 * 
 * This program solves instances of the stable roomates problems, or will
 * ouput false if the instance cannot be solve. 
 *
 */
public class StableRoommates {

	//OUR "GLOBAL VARIABLES" IN PASCAL IMPLEMENTATION - 
	static int MAX_SIZE = 91;
	
	//globals for problem instance 
	public Integer[][] rankMatrix = null;
	public Integer[][] preferenceMatrix = null;
	public Integer[][] lsrMatrix = null;
	
	public int numPeople;
	public boolean solnPossible = true;
	public boolean solnFound = false;
	
	public LinkedList<Integer>[] reducedPMatrix = null;
	
	public int firstInCycle;
	public int firstRepeat;
	public ArrayList<Integer> pCycle = new ArrayList<>();
	public ArrayList<Integer> qCycle = new ArrayList<>();
	public Integer[] positionInCycle;
	
	
	
	//constructor
	public StableRoommates(Integer[][] pMatrix) {
		this.preferenceMatrix = pMatrix;
		//check the first row to see if there is an entry equal to the size of the matrix
		boolean oneIndex = false;
		for(int i = 0; i < pMatrix.length; i++) {
			if(pMatrix[0][i] == pMatrix.length) {
				oneIndex = true;
			}
		}
		if(oneIndex ) {
			this.zeroifyMatrix();
		}
		
		
		this.numPeople = pMatrix.length;
		this.positionInCycle = new Integer[this.numPeople];
		
		this.lsrMatrix = buildLRMatrix(pMatrix.length);
		this.rankMatrix = buildRankMatrix(pMatrix);
		
		this.reducedPMatrix = this.phase1Reduction();
		
		
	}
	
	
	
	
	
	/**
	 * Helper function for constructor to make certain we don't have 1 based preference
	 * matricies 
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
	 * This method creates and initializes a 3 column matrix where the left column is left index, second column is
	 * the second person in their reduced list, and the third colum is the right index. 
	 * build
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
	 * This method takes in a preference matrix, (assumed to include sentinels) 
	 * and returns the corresponding rank matrix. 
	 * @param pMatrix
	 * @return
	 */
	public Integer[][] buildRankMatrix(Integer[][] pMatrix){
		
		int numPeople = pMatrix.length; //number of people, same as num preferences b/c including sentials
		
		Integer[][] rankMatrix = new Integer[numPeople][numPeople];
		
		for(int i = 0; i < numPeople; i++) {
			for(int j = 0; j < numPeople; j++) {
				rankMatrix[i][pMatrix[i][j] ] = j;
			}
		}
		
		return rankMatrix;
	}
	
	
	
	
	
	
	/**
	 * This method performs the phase one reduction on the object instatnce (ie to get access to "global" variables)
	 * and returns the "reduced list" in the form of an array of linked lists for each person. 
	 * @return
	 */
	public LinkedList<Integer>[] phase1Reduction() {
		
		//Do proposals and set left/right based on those
		if(!this.doProposals()) {
			//a solution is not possible
			return null;
		}

		
		//Generate linked list based on the proposals and left/right preference locations 
		
		//Array of linked lists: 
		@SuppressWarnings("unchecked") LinkedList<Integer>[] reducedPMatrix = new LinkedList[this.numPeople];
		//the above is a "type safety" conversion issue... so use array list instead 
		
		//List<LinkedList<Integer>> reducedPMatrix = new ArrayList<LinkedList<Integer>>();
		//wouldn't resolve to array type... ? 
		
		for(int i = 0; i < this.numPeople; i++) {
			//initlize each list
			reducedPMatrix[i] = new LinkedList<Integer>();
			for(int j = 0; j < this.numPeople; j++) {
				
				//see corollary 1.2 and 1.3 or something like that
				if(!( j > this.lsrMatrix[i][2] ) || this.lsrMatrix[this.preferenceMatrix[i][j] ] [2] < this.rankMatrix[this.preferenceMatrix[i][j]][i]) {
					reducedPMatrix[i].add(this.preferenceMatrix[i][j]);
				}
			}
		}
		
		return reducedPMatrix;
		
		
	}
	
	
	/**
	 * This method performs the series of propsals to find and set the left/right preferences of 
	 * the preference matrix. It returns false if any persons left/right indicies match, ie saying that they
	 * proposed to themself. 
	 * @return
	 */
	public boolean doProposals() {
		//build set to keep track of who has made a proposal
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
			
				//what is the purpose of current?
				current = this.preferenceMatrix[nextChoice][this.lsrMatrix[nextChoice][2]];
				
				while(this.rankMatrix[nextChoice][proposer] > this.rankMatrix[nextChoice][current] ) {
					//the proposer is rejected by their next choice
					
					//update the proposers left index
					this.lsrMatrix[proposer][0] += 1;
					//update next choice
					nextChoice = this.preferenceMatrix[proposer][this.lsrMatrix[proposer ][0]];
					
				//	nextChoice = this.proposalMatrix[proposer][this.lsrMatrix[0][proposer ++] ];
					//I don't think this is gonna do what we want. 
					
					current = this.preferenceMatrix[nextChoice][this.lsrMatrix[nextChoice][2]];
	
				}
			
				//next choice holds the proposer, and rejects current 
				this.lsrMatrix[nextChoice][2] = this.rankMatrix[nextChoice][proposer];
				
				proposer = current;

			} while( hasProposed.contains(nextChoice) );//while the current next choice object hasn't made a proposal
			
			
			hasProposed.add(nextChoice);
		}
		
		boolean possibleSoln = (proposer == nextChoice); //check if the next choice is theirself
			
			this.solnPossible = possibleSoln;
			return solnPossible;

	}
	
	/*
	 * This method iterates over the current stage of the reducedPmatrix LinkedList and checks if each head is equal to the 
	 * index of which list it is and if so, then it updates solnPossible to show that there is no possible solution because 
	 * the person's only remaining possible partner is themself
	 * 
	 * Tbh not sure if we will use this yet
	 */
	public void checkSolnPossible() {
		for(int i = 0; i < this.numPeople; i++) {
			if(this.reducedPMatrix[i].getFirst() == i) {
				this.solnPossible = false;
			}
		}
		return;
	}
		
	/*
	 * this method finds the first instance at which a person still has more than one
	 * possible partner and updates them to be the new first in the cycle
	 */
	public void findFirstUnmatched() {
		this.firstInCycle = 0;
		while(this.reducedPMatrix[this.firstInCycle].size() == 1) {
			this.firstInCycle++;
		}
		return;
	}
	
	/*
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
			pTerm = this.reducedPMatrix[qTerm].get(this.reducedPMatrix[qTerm].size());
		}
		this.firstRepeat = pTerm;
		return;
	}
	
	
	/*
	 * this method uses the two sequences p and q and forces each element in q to reject the proposal it has
	 * this implements one iteration of the phase 2 reduction
	 */
	public void phase2Reduction() {
		if(this.pCycle.size() == 1) {
			this.solnPossible = false;
			return;
		}else {
			for(int i = 0; i < this.pCycle.size(); i++) {
				//force each q to reject the proposal it currently holds which moves its right index left one 
				//and moves the first and second index of its previous proposer (p_i by construction) each to the right one
				
			//	this.lsrMatrix[this.pCycle.get(i)][0]++;
				this.reducedPMatrix[this.pCycle.get(i)].removeFirst();
			//	this.lsrMatrix[this.pCycle.get(i)][1]++;
			//	this.lsrMatrix[this.qCycle.get((i+1) % this.pCycle.size())][2]--;
				this.reducedPMatrix[this.qCycle.get((i+1) % this.pCycle.size())].removeLast();
			}
			//update next firstInCycle to be the last element of the tail
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
			//this.lsrMatrix[i][1] = this.reducedPMatrix[i].get(1); 
			//actually just set it to two
			this.lsrMatrix[i][1] = 1;
		}
		
		while(this.solnPossible && !this.solnFound) {
			//see if we found a soln
			if(this.firstInCycle >= this.numPeople -1) {
				
			}
		}
		
		return;
	}
	
	public  void printSolution() {
		System.out.println("A stable pairing has been found!\nThe assignments are:\n");
		for(int i = 0; i < this.numPeople; i++) {
			System.out.printf("%2d is paired with: %2d\n", this.reducedPMatrix[i].get(0), this.reducedPMatrix[i].getLast());
		}
	}
	
	public static void printNoSolution() {
		System.out.println("There is no stable matching for these preferences.\n");
		return;
	}
	
	
	
	public static void main(String[] args) {
		//input matrices should be n by n! 
		
		//test from page 582
		Integer[][] testPMatrix = new Integer[][] {
			{4, 6, 2, 5, 3, 1},
			{6, 3, 5, 1, 4, 2},
			{4, 5, 1, 6, 2, 3},
			{2, 6, 5, 1, 3, 4},
			{4, 2, 3, 6, 1, 5},
			{5, 1, 4, 2, 3, 6}
		};
		
		//create the object
		StableRoommates instance1 = new StableRoommates(testPMatrix);
		
		//DEBUG/GENERAL TESTING SO FAR
		//Pause here in debugger to look at values
		//System.out.println("Testing\n\nNumPeople expected 6\nResult:");
		
		//Run phase two reduction until we either make it work or know it can't be done
		instance1.findSolution();
		
		if(instance1.solnFound) {
			instance1.printSolution();
		}else {
			StableRoommates.printNoSolution();
		}
		
		
	}
	
	
}
