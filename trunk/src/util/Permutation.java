/**
 * Utilities: Prints a pseudorandom permution of the integers 0 through N.
 * 
 * @version 9/1/08
 *
 *    % java Shuffle 6
 *    5 0 2 3 1 4 
 *    . * . . . . 
 *    . . . . * . 
 *    . . * . . . 
 *    . . . * . . 
 *    . . . . . * 
 *    * . . . . . 
 *
 **/

package util;

import java.util.Random;

import org.apache.commons.math3.random.RandomDataGenerator;

public class Permutation { 
	
   public static void main(String[] args) { 
	   
	   int N = 20;
	   int[] a = Permutation.permute(N, new RandomDataGenerator());
		   
	   // print permutation
	   for (int i = 0; i < N; i++)
	      System.out.print(a[i] + " ");
	   System.out.println("");

	   // print checkerboard visualization
	   for (int i = 0; i < N; i++) {
	      for (int j = 0; j < N; j++) {
	         if (a[j] == i) System.out.print("* ");
	         else           System.out.print(". ");
	      }
	      System.out.println("");
	   }
   }
   
   public static int[] permute(int N, Random rand) {

	      int[] a = new int[N];

	      // insert integers 0..N-1
	      for (int i = 0; i < N; i++)
	         a[i] = i;

	      // shuffle
	      for (int i = 0; i < N; i++) {
	         int r = (i == 0 ? 0 : rand.nextInt(i + 1)); // int between [0..i]
	         int swap = a[r];
	         a[r] = a[i];
	         a[i] = swap;
	      }
	      
	      return a;
   }
   
   public static int[] permute(int N, RandomDataGenerator rand) {

      int[] a = new int[N];

      // insert integers 0..N-1
      for (int i = 0; i < N; i++)
         a[i] = i;

      // shuffle
      for (int i = 0; i < N; i++) {
         int r = (i == 0 ? 0 : rand.nextInt(0,i));     // int between [0..i]
         int swap = a[r];
         a[r] = a[i];
         a[i] = swap;
      }
      
      return a;
   }
}
