/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hexplode;

import permutation.Permutation;
import it.unimi.dsi.fastutil.objects.*;
import java.util.stream.Collectors;

/**
 *
 * @author Marco
 */
public class Tree {
    private ObjectArrayList<int[]> nodes;
    private int numPoint = 0;
    private int curID = 0;
    private boolean modifiable = true;
    private boolean closed = false;
    
    public Tree() {
        nodes = new ObjectArrayList<>();
    }
    
    public Tree(int[][] vals) {
        nodes= new ObjectArrayList<>();
        nodes.addElements(0, vals);
        numPoint = (int) nodes.stream().filter((n) -> (n[1] >= 0)).count();
    }
        
    public int getNumPoint() {
        return numPoint;
    }
    
    public int[][] getList() {
        return nodes.toArray(new int[0][0]);
    }
    
    private void sort() {
        nodes.sort((int[] o1, int[] o2) -> // <editor-fold defaultstate="collapsed" desc="Compare basing on the entries">
        {
            if(o1[0] > o2[0]) return 1;
            else if(o1[0] < o2[0]) return -1;
            else {
                if(o1[1] > o2[1]) return 1;
                else if(o1[1] < o2[1]) return -1;
                else {
                    if(o1[2] > o2[2]) return 1;
                    else if(o1[2] < o2[2]) return -1;
                    else {
                        if(o1[3] > o2[3]) return 1;
                        else if(o1[3] < o2[3]) return -1;
                        else return 0;
                    }
                }
            }
        }
        // </editor-fold>
            );
    }
    
    public void addLeaf(int from, int ID, int attack, int counterAttack) {
        if(modifiable) {
            if(ID != -1 && ID !=-2) numPoint++;
            nodes.add(new int[]{from, ID, attack, counterAttack});
        }
        else throw new UnsupportedOperationException("Cannot modify the tree when it's already been started exploring it");
    }
    
    public void addLeaf(int[] args) {
        if(modifiable) {
            if(args[1] != -1 && args[1] != -2) numPoint++;
            nodes.add(args);
        }
        else throw new UnsupportedOperationException("Cannot modify the tree when it's already been started exploring it");
    }
    
    public int explore(int attackMove) {
        if(closed) return -1;
        modifiable = false;
        //find the children of curID
        ObjectList<int[]> children = new ObjectArrayList<>(nodes.parallelStream().filter((n) -> (n[0] == curID)).collect(Collectors.toList()));
        
        int[] value = null;
        for(int[] val : children) if(val[2] == attackMove) {
            value = val;
            break;
        }
        if(value == null) return -1;
        //find the response for the given attack
        int res = value[3];
        //now that the correct counter attack has been found and stored, update the tree datas
        
        //if the node is an end leaf, close the tree
        if(value[1] == -1) closed = true;
        //if the found node is not a Gx, update the tree
        else if(value[1] != -2) {
            curID = value[2];
            //find all the Gxs that are children of curDepth and prevAttack and add them to the children of the newly found node
            ObjectList<int[]> Gxs = new ObjectArrayList<>(children.parallelStream().filter((val) -> (val[1] == -2)).collect(Collectors.toList()));
            Gxs.stream().forEach((Gx) -> {
                nodes.add(new int[] {curID, -2, Gx[2], Gx[3]});
            });
            //remove the old Gxs
            nodes.removeAll(Gxs);
        }
//        //else, delete the other symmetrical Gx
//        else nodes.remove(new int[] {curDepth, attackMove, value[4], 1, value[2], attackMove});
        return res;
    }
    
    public void applyPerm(Permutation p) {
        nodes.stream().map((node) -> {
            nodes.remove(node);
            return node;
        }).forEach((node) -> {
            nodes.add(new int[] {node[0], node[1], p.perm(node[2]), p.perm(node[3])});
        });
    }
    
    private void gc() {
        
    }
}
