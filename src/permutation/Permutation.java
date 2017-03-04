/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package permutation;

import java.util.Arrays;

/**
 *
 * @author Marco
 */
public class Permutation {
    private int n;
    private int[] perm;
    
    public Permutation(int x) {
        n = x;
        perm = new int[n];
        for(int i = 0; i < n; i++) perm[i] = i;
    }
    
    public Permutation(int x, int[] permutations) {
        n = x;
        perm = permutations;
    }
    
    public int n() {
        return n;
    }
    
    public int[] toArray() {
        return perm;
    }
    
    public void invert() {
        int[] newPerm = new int[n];
        for(int i = 0; i < n; i++) newPerm[perm[i]] = i;
        perm = newPerm;
    }
    
    public int perm(int i) {
        return perm[i];
    }
    
    public int invPerm(int j) {
        for(int i = 0; i < perm.length; i++) if(perm[i] == j) return i;
        throw new IllegalArgumentException("Cannot find element " + j);
    }
    
    public void swap(int i, int j) {
        int cache = perm[j];
        perm[j] = perm[i];
        perm[i] = cache;
    }
    
    public void cycle(int... i) {
        int cache = i[i.length - 1];
        for(int k = i.length - 2; k >= 0; k--) {
            perm[i[k + 1]] = perm[i[k]];
        }
        perm[i[0]] = cache;
    }
    
    public void set(int source, int dest) {
        if(source < dest) {
            int[] pos = new int[dest - source];
            for(int i = dest; i <= source; i++) {
                pos[i - dest] = i;
            }
            cycle(pos);
        }
        else if(source > dest) {
            int[] pos = new int[source - dest];
            for(int i = source; i >= dest; i--) {
                pos[source - i] = i;
            }
            cycle(pos);
        }
    }
    
    public void compose(Permutation g, boolean applyGFirst) {
        if(g.n == n) {
            if(applyGFirst) {
                int[] newPerm = new int[n];
                for(int i = 0; i < n; i++) newPerm[i] = perm[g.perm[i]];
                perm = newPerm;
            }
            else {
                int[] newPerm = new int[n];
                for(int i = 0; i < n; i++) newPerm[i] = g.perm[perm[i]];
                perm = newPerm;
            }
        }
        else throw new IllegalArgumentException("Cannot compose two permutations with " + n + " and " + g.n + " number of elements");
    }
    
    public Permutation copy() {
        return new Permutation(n, perm);
    }
    
    @Override
    public String toString() {
        return Arrays.toString(perm);
    }
}
