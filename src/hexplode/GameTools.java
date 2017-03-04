/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hexplode;

import setgame.Game;
import java.util.List;
import it.unimi.dsi.fastutil.objects.*;
import it.unimi.dsi.fastutil.ints.*;
import org.apache.commons.lang3.ArrayUtils;
import org.mapdb.*;
import org.magicwerk.brownies.collections.*;
import org.roaringbitmap.*;
import org.roaringbitmap.buffer.*;
import it.unimi.dsi.fastutil.PriorityQueue;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;
import permutation.Permutation;
import java.io.File;

/**
 *
 * @author Marco
 */
public class GameTools {
    //parameter for specifying at which depth of the strategy tree (starting from 0) the methods isPos() and isPositive() should print their datas - for debug only,
    //default to -1
    private int param = -1;
    //variable used internally to assign IDs to nodes in the strategy tree
    private int counter = 0;
    //weight for the p-average, default to -1.0
    private double m = -1.0;
    //database on which are stored the minimal positive games
    private DB db = null;
    //HashMap that contains the records
    private HTreeMap table;
    //path to the database file
    private String dbPath = System.getProperty("user.dir") + "\\src\\hexplode\\storedData\\datas.db";
    //queue that contains the games to be tested for new minimal positive games
    private final List<Game> toCheck = new ObjectArrayList<>();
    
    public GameTools(String path, double weight, int depth) {
        dbPath = path;
        m = weight;
        param = depth;
    }
    
    public GameTools(String path) {
        dbPath = path;
    }
    
    public GameTools() {
        
    }
    
    public void cutElement(Game source, int pointColumn) {
        IntList del = new IntArrayList();
        for(int i = 0; i < source.getBlocksCount(); i++) if(source.get(i, pointColumn)) del.add(i);
        source.removeAllBlocks(del);
        source.removeElm(pointColumn);
    }
    
    public void secureElement(Game source, int pointColumn) {
        source.removeElm(pointColumn);
    }
    
    public void reduce(Game source) {
        //if a block contains another one, delete it
        // <editor-fold defaultstate="collapsed" desc="Check for included blocks">
        IntList del = new IntArrayList();
        GapList<ImmutableRoaringBitmap> blocks = source.getAllBlocks();
        //for each unordered couple of different rows:
        for(int i = 0; i < source.getBlocksCount(); i++) {
            for(int j = i + 1; j < source.getBlocksCount(); j++) {
                //check if one is included into the other
                ImmutableRoaringBitmap cache = ImmutableRoaringBitmap.and(blocks.get(i), blocks.get(j));
                if(ImmutableRoaringBitmap.xor(cache, blocks.get(i)).isEmpty()) if(!del.contains(j)) del.add(j);
                else if(ImmutableRoaringBitmap.xor(cache, blocks.get(j)).isEmpty()) {
                    if(!del.contains(i))del.add(i);
                    //if row i is useless, it has no point comparing other rows with it, so this cuts off on unnecessary tasks
                    break;
                }
            }
        }
        source.removeAllBlocks(del);
        //</editor-fold>
        //find minimal intersections with cardinality >= 2 and delete them and the blocks that contain them
        // <editor-fold defaultstate="collapsed" desc="Check for minimal intersections with cardinality >= 2">
        boolean changed;
        del.clear();
        
        GapList<RoaringBitmap> elms;
        do {
            changed = false;
            elms = source.getAllPlayableElms();
            //for each unordered couple of different elements:
            for(int i = 0; i < elms.size(); i++) {
                for(int j = i + 1; j < elms.size(); j++) {
                    //if i and j belong to the same blocks:
                    if(RoaringBitmap.xor(elms.get(i), elms.get(j)).isEmpty()) {
                        if(!del.contains(i))del.add(i);
                        if(!del.contains(j)) del.add(j);
                        
                        IntList deli = new IntArrayList();
                        for(int k = 0; k < source.getBlocksCount(); k++) if(source.get(k, source.absElm(i))) deli.add(k);
                        source.removeAllBlocks(deli);
                        
                        IntList delj = new IntArrayList();
                        for(int k = 0; k < source.getBlocksCount(); k++) if(source.get(k, source.absElm(j))) delj.add(k);
                        source.removeAllBlocks(delj);
                        //we can stop looking for matchings with i, since columns identical to i can be found comparing them with j
                        break;
                    }
                }
            }
            source.removeAllElms(del);
        } while(changed);
        //</editor-fold>
    }
    
    //this method returns the applied reductions
    public  ObjectList<int[]> reduceWithOR(Game source) {
        ObjectList<int[]> bads = new ObjectArrayList<>();
        ObjectList<int[]> res = new ObjectArrayList<>();
        boolean changed;
        GapList<ImmutableRoaringBitmap> blocks;
        do {
            changed = false;
            
            blocks = source.getAllBlocks();
            IntList rows = new IntArrayList();
            int[] couple = null;
            //for each unordered couple of different rows:
            for(int i = 0; i < blocks.size(); i++) {
                for(int j = i + 1; j < blocks.size(); j++) {
                    //if the two rows have same cardinality and differ only in two elements
                    if(blocks.get(i).getCardinality() == blocks.get(j).getCardinality()) {
                        ImmutableRoaringBitmap xor = ImmutableRoaringBitmap.xor(blocks.get(i), blocks.get(j));
                        if(xor.getCardinality() == 2) {
                            if(changed) {
                                if(xor.select(0) == couple[0] && xor.select(1) == couple[1]) {
                                    if(blocks.get(i).contains(couple[0])) {
                                        rows.add(i);
                                        rows.add(j);
                                    }
                                    else {
                                        rows.add(j);
                                        rows.add(i);
                                    }
                                    //there can't be a correspondance between i and another row, so this cuts off on unnecessary tasks (if the actual couple is bad,
                                    //it will be equally noticed)
                                    break;
                                }
                            }
                            else {
                                //this assignment is probably useless, check it
//                                int a = blocks.get(i).contains(xor.select(0)) ? xor.select(0) : xor.select(1); 
//                                int b = (a == xor.select(0)) ? xor.select(1) : xor.select(0);
                                //the two elements can't be equal, so the following stuff will always work
                                int[] cpl = new int[] {(xor.select(0) < xor.select(1)) ? xor.select(0) : xor.select(1), (xor.select(0) < xor.select(1)) ? xor.select(1) : xor.select(0)};
                                //find if the couple of backup elements is bad or not
                                boolean isBad = false;
                                for(int[] bad : bads) if(bad[0] == cpl[0] && bad[1] == cpl[1]) {
                                    isBad = true;
                                    break;
                                }
                                if(!isBad) {
                                    changed = true;
                                    couple = cpl;
                                    if(blocks.get(i).contains(couple[0])) {
                                        rows.add(i);
                                        rows.add(j);
                                    }
                                    else {
                                        rows.add(j);
                                        rows.add(i);
                                    }
                                    //there can't be a correspondance between i and another row, so this cuts off on unnecessary tasks (if the actual couple is bad,
                                    //it will be equally noticed)
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            
            if(changed) {
                boolean bad = false;
                //check that the couple isn't bad
                for(int i = 0; i < blocks.size(); i++) {
                    if(!rows.contains(i) && (blocks.get(i).contains(couple[0]) || blocks.get(i).contains(couple[1]))) {
                        bads.add(couple);
                        bad = true;
                        break;
                    }
                }
                //if the couple was good, continue the algorithm
                if(!bad) {
                    //for each valid pair of rows, substitute them with a single row
                    for(int i = 0; i < rows.size(); i = i+2) {
                        RoaringBitmap roar = blocks.get(rows.getInt(i)).toRoaringBitmap().clone();
                        roar.remove(couple[0]);
                        source.addBlock(source.getBlocksCount(), roar);
                    }
                    source.removeAllBlocks(rows);
                    //add the nodes to the list
                    res.add(couple);
                    res.add(new int[]{couple[1], couple[0]});
                    //delete the elements of the couple from the matrix (they aren't useful anymore)
                    source.removeAllElms(couple);
                }
            }
        } while(changed);
        return res;
    }
    
    public void openDB() {
        db = DBMaker.fileDB(new File(dbPath)).checksumHeaderBypass().fileMmapEnableIfSupported().closeOnJvmShutdown().make();
        openTable();
    }
    
    public void openTable() {
        table = db.hashMap("datas").createOrOpen();
    }
    
    public Record[] getFromDB(int rows, int cols) {
        String key = rows + "|" + cols;
        if(table.containsKey(key)) {
            int[][] dbInts = (int[][]) table.get(key);
            if(dbInts != null) {
                Record[] dbRecs = new Record[dbInts.length];
                for(int i = 0; i < dbRecs.length; i++) dbRecs[i] = new Record(dbInts[i]); 
                return dbRecs;
            }
            else return null;
        }
        else return null;
    }
    
    public boolean isInDB(Game source) {
        Record[] mins = getFromDB(source.getBlocksCount(), source.getPlayableElmsCount());
        if(mins != null) for (Record r : mins) {
            Game b = r.getGame();
            //if we have found it, return true
            if(isAutomorph(b, source)) return true;
        }
        return false;
    }
    
    public Tree getMinStrategy(Game source) {
        Record[] mins = getFromDB(source.getBlocksCount(), source.getPlayableElmsCount());
        if(mins != null) for (Record r : mins) {
            Game b = r.getGame();
            //if we have found it, return the strategy
            Permutation p = automorph(b, source);
            if(p != null) {
                Tree minTree = r.getTree();
                Tree tree = new Tree();
                for(int[] node : minTree.getList()) tree.addLeaf(node[0], node[1], source.absElm(p.perm(node[2])), source.absElm(p.perm(node[3])));
                return tree;
            }
        }
        return null;
    }
    
    //finds wheter the given game is positive or not; the algorhitm tries, in order, to:
    //-reduce the game deleting useless rows
    //-reduce the game using the OR rule
    //-find a correspondence with one of the ancestors listed in the database
    //-go ahead of one step in the game tree
    public Tree isPositive(Game source, boolean useDB, boolean updateDB) {
        if(useDB) {
            if(db == null || db.isClosed()) openDB();
            else openTable();
        }
        //reset counter for the calculation
        counter = 0;
        IntList del = new IntArrayList();
        Game s = source.copy();
        for(int i = 0; i < s.getPlayableElmsCount(); i++) if(!s.isElmBelongingToAnyBlock(i)) del.add(i);
        s.removeAllElms(del);
        Tree res = new Tree();
        if(isPos(s, res, 0, useDB) > -1) {
            if(updateDB) startSearch(toCheck);
            return res;
        }
        return null;
    }
    
    //internal method - WARNING: modifies the matrix
    private int isPos(Game source, Tree tree, int depth, boolean useDB) {
        int root = counter;
        ObjectList<int[]> nodes = new ObjectArrayList<>();
        //if there are no sets left, the game is surely negative
        if(source.getBlocksCount() == 0) return -1;
        //if there is an empty set, the game is surely positive (and it is reported that no moves had been made)
        for(ImmutableRoaringBitmap roar : source.getAllBlocks()){
            if(roar.isEmpty()) return 0;
        }
        //delete useless rows and elements
        reduce(source);
        //reduce the game using the OR rule
        ObjectList<int[]> red = reduceWithOR(source);
        red.stream().forEach((couple) -> {
            nodes.add(new int[] {root, -2, couple[0], couple[1]});
            if(depth == param) System.out.println("OR");
            if(depth == param) System.err.println(couple[0] + " -> " + couple[1]);
        });
        //if there is an empty set after OR reduction, the game is surely positive
        for(ImmutableRoaringBitmap roar : source.getAllBlocks()){
            if(roar.isEmpty()) {
                //commit to tree all changes made
                nodes.stream().forEach(tree::addLeaf);
                return 1;
            }
        }
        //if there are one or more elements common to all sets, the game is negative
        int[] allBlocks = new int[source.getBlocksCount()];
        for(int i = 0; i < source.getBlocksCount(); i++) allBlocks[i] = i;
        if(!source.andAll(allBlocks).isEmpty()) return -1;
        
        //first method: search minimal positive games that are automorph with the given one
        // <editor-fold defaultstate="collapsed" desc="Check the database">
        if(useDB) {
            //if we have found it, update the strategy
            Tree res = getMinStrategy(source);
            if(res != null) {
                if(depth == param) System.out.println("Found in database");
                //commit to tree all changes made
                for(int[] node : res.getList()) tree.addLeaf(node[0] + root, node[1], node[2], node[3]);
                return 1;
            }
        }
        //</editor-fold>
        
        //second method: brute-force
        //loop through the remaining elements and cut one of them each time
        
        for(int i = 0; i < source.getPlayableElmsCount(); i++) {
            Game source1 = source.copy();
            cutElement(source1, source.absElm(i));
            boolean pos = false;
            //obviously, if there's a set with a single element, that is the most sensible strategy to apply
            // <editor-fold defaultstate="collapsed" desc="Basic checking">
            for(ImmutableRoaringBitmap roar : source1.getAllBlocks()) if(roar.getCardinality() == 1) {
                if(depth == param) System.out.println("Basic");
                if(depth == param) System.err.println(source.absElm(i) + " -> " + roar.select(0));
                pos = true;
                nodes.add(new int[] {root, -1, source.absElm(i), roar.select(0)});
            }
            //</editor-fold>
            if(!pos) {
                //if there are more than one common elements the game is negative; if there is exactly one the thief must occupy that one, after that it depends on
                //the game which is obtained
                // <editor-fold defaultstate="collapsed" desc="Fast checking">
                int[] allBlocks1 = new int[source1.getBlocksCount()];
                for(int n = 0; n < source1.getBlocksCount(); n++) allBlocks1[n] = n;
                ImmutableRoaringBitmap intersection = source1.andAll(allBlocks1);
                if(!intersection.isEmpty()) {
                    if(intersection.getCardinality() > 1) {
                        return -1;
                    }
                    else {
                        Game source2 = source1.copy();
                        int move = intersection.select(0);
                        secureElement(source2, move);
                        counter++;
                        int res = isPos(source2, tree, depth + 1, useDB);
                        //if we have found it
                        if(res != -1) {
                            if(res == 0) counter--;
                            pos = true;
                            if(depth == param) System.out.println("Fast");
                            if(depth == param) System.err.println(source.absElm(i) + " -> " + move);
                            //update the tree
                            nodes.add(new int[] {root, (res == 0) ? -1 : counter, source.absElm(i), move});
                        }
                        //else, the game is negative
                        else {
                            counter--;
                            return -1;
                        }
                    }
                }
                // </editor-fold>
                //loop through the remaining elements, sorted by number of sets they belong to: if at least one, when occupied, leads to a positive game, then the
                //starting game is positive too
                // <editor-fold defaultstate="collapsed" desc="Long checking">
                else {
                    //<editor-fold defaultstate="collapsed" desc="Preparing the ordered list of possible moves">
                    IntList possibleMoves = new IntArrayList();
                    for(int col = 0; col < source1.getPlayableElmsCount(); col++) possibleMoves.add(source1.absElm(col));
                    boolean change;
                    do{
                        change = false;
                        for(int k = 0; k < possibleMoves.size() - 1; k++) if(av(m, source1, possibleMoves.get(k)) < av(m, source1, possibleMoves.get(k + 1))) {
                            int cache = possibleMoves.get(k);
                            possibleMoves.set(k, possibleMoves.get(k + 1));
                            possibleMoves.set(k + 1, cache);
                            change = true;
                        }
                    } while(change);
                    //</editor-fold>
                    for(int j = 0; j < possibleMoves.size(); j++) {
                        Game source2 = source1.copy();
                        secureElement(source2, possibleMoves.get(j));
                        counter++;
                        int res = isPos(source2, tree, depth + 1, useDB);
                        //if we have found it
                        if(res != -1) {
                            if(res == 0) counter--;
                            pos = true;
                            if(depth == param) System.out.println("Long");
                            if(depth == param) System.err.println(source.absElm(i) + " -> " + possibleMoves.get(j));
                            //update the tree
                            nodes.add(new int[] {root, (res == 0) ? -1 : counter, source.absElm(i), possibleMoves.get(j)});
                            //go to next attacker's move
                            break;
                        }
                        //else, reset the counter
                        else counter--;
                    }
                }
                // </editor-fold>
            }
            if(!pos) return -1;
        }
        //commit to tree all the changes made
        nodes.stream().forEach(tree::addLeaf);
        //the game was not in the DB, so enqueue it for ancestor searching
        toCheck.add(source);
        return 1;
    }
    
    //faster method, just tells if the game is positive or not
    public boolean isPositiveB(Game source, boolean useDB, boolean updateDB) {
        if(useDB) {
            if(db == null || db.isClosed()) openDB();
            else openTable();
        }
        IntList del = new IntArrayList();
        Game s = source.copy();
        for(int i = 0; i < s.getPlayableElmsCount(); i++) if(!s.isElmBelongingToAnyBlock(i)) del.add(i);
        s.removeAllElms(del);
        boolean res = isPosB(s, 0, useDB);
        if(updateDB) startSearch(toCheck);
        return res;
    }
    
    //internal method - WARNING: modifies the matrix
    public boolean isPosB(Game source, int depth, boolean useDB) {
        //if there are no sets left, the game is surely negative
        if(source.getBlocksCount() == 0) return false;
        //delete useless rows
        reduce(source);
        //reduce the game using the OR rule
        ObjectList<int[]> red = reduceWithOR(source);
        red.stream().forEach((couple) -> {
            if(depth == param) System.out.println("OR");
            if(depth == param) System.err.println(couple[0] + " -> " + couple[1]);
        });
        //if there is an empty set, the game is surely positive
        for(ImmutableRoaringBitmap roar : source.getAllBlocks()){
            if(roar.isEmpty()) return true;
        }
        //if there are one or more elements common to all sets, the game is negative
        int[] allBlocks = new int[source.getBlocksCount()];
        for(int i = 0; i < source.getBlocksCount(); i++) allBlocks[i] = i;
        if(!source.andAll(allBlocks).isEmpty()) return false;
        
        //first method: search minimal positive games that are automorph with the given one
        // <editor-fold defaultstate="collapsed" desc="Check the database">
        if(useDB) {
            //if we have found it, return true
            if(isInDB(source)) {
                if(depth == param) System.out.println("Found in database");
                return true;
            }
        }
        //</editor-fold>
        
        //second method: brute-force
        //loop through the remaining elements and cut one of them each time
        
        for(int i = 0; i < source.getPlayableElmsCount(); i++) {
            Game source1 = source.copy();
            cutElement(source1, source.absElm(i));
            boolean pos = false;
            //obviously, if there's a set with a single element, that is the most sensible strategy to apply
            // <editor-fold defaultstate="collapsed" desc="Basic checking">
            for(ImmutableRoaringBitmap roar : source1.getAllBlocks()) if(roar.getCardinality() == 1) {
                if(depth == param) System.out.println("Basic");
                if(depth == param) System.err.println(source.absElm(i) + " -> " + roar.select(0));
                pos = true;
            }
            //</editor-fold>
            if(!pos) {
                //if there are more than one common elements the game is negative; if there is exactly one the thief must occupy that one, after that it depends on
                //the game which is obtained
                // <editor-fold defaultstate="collapsed" desc="Fast checking">
                int[] allBlocks1 = new int[source1.getBlocksCount()];
                for(int n = 0; n < source1.getBlocksCount(); n++) allBlocks1[n] = n;
                ImmutableRoaringBitmap intersection = source1.andAll(allBlocks1);
                if(!intersection.isEmpty()) {
                    if(intersection.getCardinality() > 1) {
                        return false;
                    }
                    else {
                        Game source2 = source1.copy();
                        int move = intersection.select(0);
                        secureElement(source2, move);
                        boolean res = isPosB(source2, depth + 1, useDB);
                        //if we have found it
                        if(res) {
                            pos = true;
                            if(depth == param) System.out.println("Fast");
                            if(depth == param) System.err.println(source.absElm(i) + " -> " + move);
                        }
                        //else, the game is negative
                        else {
                            return false;
                        }
                    }
                }
                // </editor-fold>
                //loop through the remaining elements, sorted by number of sets they belong to: if at least one, when occupied, leads to a positive game, then the
                //starting game is positive too
                // <editor-fold defaultstate="collapsed" desc="Long checking">
                else {
                    //<editor-fold defaultstate="collapsed" desc="Preparing the ordered list of possible moves">
                    IntList possibleMoves = new IntArrayList();
                    for(int col = 0; col < source1.getPlayableElmsCount(); col++) possibleMoves.add(source1.absElm(col));
                    boolean change;
                    do{
                        change = false;
                        for(int k = 0; k < possibleMoves.size() - 1; k++) if(av(m, source1, possibleMoves.get(k)) < av(m, source1, possibleMoves.get(k + 1))) {
                            int cache = possibleMoves.get(k);
                            possibleMoves.set(k, possibleMoves.get(k + 1));
                            possibleMoves.set(k + 1, cache);
                            change = true;
                        }
                    } while(change);
                    //</editor-fold>
                    for(int j = 0; j < possibleMoves.size(); j++) {
                        Game source2 = source1.copy();
                        secureElement(source2, possibleMoves.get(j));
                        boolean res = isPosB(source2, depth + 1, useDB);
                        //if we have found it
                        if(res) {
                            pos = true;
                            if(depth == param) System.out.println("Long");
                            if(depth == param) System.err.println(source.absElm(i) + " -> " + possibleMoves.get(j));
                            //go to next attacker's move
                            break;
                        }
                    }
                }
                // </editor-fold>
            }
            if(!pos) return false;
        }
        //if the game was not in the DB, enqueue it for ancestor searching
        toCheck.add(source);
        return true;
    }
    
    //retrieves the permutation on the elements that leads from A to B
    public Permutation automorph(Game A, Game B) {
        if(A.getBlocksCount() == B.getBlocksCount() && A.getPlayableElmsCount() == B.getPlayableElmsCount()) {
            // <editor-fold defaultstate="collapsed" desc="First check">
//            System.out.println("got it");
            //checking the cardinality of sets...
            IntList arows = new IntArrayList(A.getAllBlocks().parallelStream().map((b) -> (b.getCardinality())).collect(Collectors.toList()));
            IntList brows = new IntArrayList(B.getAllBlocks().parallelStream().map((b) -> (b.getCardinality())).collect(Collectors.toList()));
            Collections.sort(brows);
            Collections.sort(arows);
            for(int i = 0; i < A.getBlocksCount(); i++) if(arows.get(i).intValue() != brows.get(i).intValue()) return null;
            //checking the number of sets whose each element belongs...
            List<Integer> acols = A.getAllPlayableElmsDegrees();
            List<Integer> bcols = B.getAllPlayableElmsDegrees();
            Collections.sort(bcols);
            Collections.sort(acols);
            for(int i = 0; i < A.getPlayableElmsCount(); i++) if(acols.get(i) != 0 && bcols.get(i) != 0 && acols.get(i).intValue() != bcols.get(i).intValue()) return null;
            // </editor-fold>
//            System.out.println("got it");
            //if both tests were positive, run the choose-and-chase algorihtm
            // <editor-fold defaultstate="collapsed" desc="Choose-and-chase algorhitm">
            int univId;
            //A part
            Queue<Integer> scan = new LinkedList<>();
            scan.add(0);
            IntList knownIDs = new IntArrayList();
            //B part
            Queue<Map<Integer, Integer>> matchings = new LinkedList<>();
            matchings.add(new HashMap<>());
            while(scan.size() > 0) {
                //choose
//                System.err.println(scan);
                //take next set from scan
                int curSet = scan.remove();
//                System.err.println(scan);
                //calculate the cookie of the set
                Cookie acookie = getCookie(A, curSet, knownIDs);
//                System.out.println(acookie);
                //set univId to the pointer of the set
                univId = curSet;
                //add curSet to knownIDs
                knownIDs.add(curSet);
                //add to scan the pointers to all of the children of the current set that are not in knownIDs (neither in scan); if no such one exists and the queue is empty,
                //add to scan the first pointer to a set not in knownIDs; if this is still impossible, do nothing (the algorithm will terminate after all the remaining sets
                //in scan have been visited)
                int[] children = getChildren(A, curSet, knownIDs);
//                System.out.println(Arrays.toString(children));
                if(children.length > 0) for(Integer c : children) if(!scan.contains(c)){
                    scan.add(c);
                }
                if(scan.isEmpty()){
                    for(int i = 0; i < A.getBlocksCount(); i++) if(!knownIDs.contains(i) && !scan.contains(i)) {
                        scan.add(i);
                        break;
                    }
                }
                //chase
                int size = matchings.size();
                //for size times:
                for(int i = 0; i < size; i++) {
                    //take next Map
                    Map curMatch = matchings.remove();
                    //(now it's deleted from the list, it will be readded if and only if there exist suitable continuations)
                    //for each set in B not present in curMatch:
                    for(int set = 0; set < B.getBlocksCount(); set++) if(!curMatch.containsKey(set)) {
                        //calculate the cookie of the set (relative to path)
                        Cookie bcookie = getCookie(B, set, curMatch);
//                        System.err.println(bcookie);
                        //if it matches with curSet's cookie, add to matchings a copy of curMatch with set's ID set to univId
                        if(bcookie.matches(acookie)) {
                            Map addMap = new HashMap(curMatch);
                            addMap.put(set, univId);
                            matchings.add(addMap);
                        }
                    }
//                    System.err.println("end");
                }
                if(matchings.isEmpty()) return null;
                //else, continue the algorithm
            }
//            System.out.println(knownIDs);
//            System.out.println("got it: " + matchings);
            // </editor-fold>
            //last check: for each HashMap in matchings, order the columns of the two matrices and see if they are the same; if so, return the first HashMap that 
            //passes the test
            // <editor-fold defaultstate="collapsed" desc="Last check">
            //order the columns of the first matrix (do it once)
//            System.out.println(A);
            Permutation pa = new Permutation(A.getPlayableElmsCount());
            GapList<RoaringBitmap> elmsa = A.getAllPlayableElms();
            boolean sorta;
            do {
                sorta = false;
                for(int i = 0; i < A.getPlayableElmsCount() - 1; i++) if(compare(elmsa.get(pa.perm(i)), elmsa.get(pa.perm(i + 1))) == -1) {
                    pa.swap(i, i+1);
                    sorta = true;
                }
            } while(sorta);
//            System.out.println(pa);
            //for each HashMap:
            for(Map<Integer, Integer> map : matchings) {
                //calculate permutation on the rows
                int[] pvals = new int[map.size()];
                for(int i = 0; i < map.size(); i++) pvals[i] = map.get(i);
                Permutation rows = new Permutation(map.size(), pvals);
                rows.invert();
//                System.out.println(rows);
                //order the columns of the second matrix
                Permutation pb = new Permutation(B.getPlayableElmsCount());
                GapList<RoaringBitmap> elmsb = B.getAllPlayableElms();
                boolean sortb;
                do {
                    sortb = false;
                    for(int i = 0; i < B.getPlayableElmsCount() - 1; i++) if(compare(elmsb.get(pb.perm(i)), elmsb.get(pb.perm(i + 1)), rows, rows) == -1) {
                        pb.swap(i, i+1);
                        sortb = true;
                    }
                } while(sortb);
//                System.out.println(pb);
                //check that the permutations work
                boolean works = true;
                for(int i = 0; i < A.getBlocksCount(); i++) {
                    for(int j = 0; j < A.getPlayableElmsCount(); j++) {
                        if(A.get(i, pa.perm(j)) != B.get(rows.perm(i), pb.perm(j))) {
                            works = false;
                            break;
                        }
                    }
                }
                //if so, return the permutation A -> B
                if(works) {
                    pa.invert();
                    pa.compose(pb, false);
                    pa.invert();
                    return pa;
                }
            }
            return null;
            // </editor-fold>
        }
        else return null;
    }
    
    //faster method, just checks if two matrices are automorph, doesn't calculate the strategy
    public boolean isAutomorph(Game A, Game B) {
        if(A.getBlocksCount() == B.getBlocksCount() && A.getPlayableElmsCount() == B.getPlayableElmsCount()) {
            // <editor-fold defaultstate="collapsed" desc="First check">
//            System.out.println("got it");
            //checking the cardinality of sets...
            IntList arows = new IntArrayList(A.getAllBlocks().parallelStream().map((b) -> (b.getCardinality())).collect(Collectors.toList()));
            IntList brows = new IntArrayList(B.getAllBlocks().parallelStream().map((b) -> (b.getCardinality())).collect(Collectors.toList()));
            Collections.sort(brows);
            Collections.sort(arows);
            for(int i = 0; i < A.getBlocksCount(); i++) if(arows.get(i).intValue() != brows.get(i).intValue()) return false;
            //checking the number of sets whose each element belongs...
            List<Integer> acols = A.getAllPlayableElmsDegrees();
            List<Integer> bcols = B.getAllPlayableElmsDegrees();
            Collections.sort(bcols);
            Collections.sort(acols);
            for(int i = 0; i < A.getPlayableElmsCount(); i++) if(acols.get(i) != 0 && bcols.get(i) != 0 && acols.get(i).intValue() != bcols.get(i).intValue()) return false;
            // </editor-fold>
//            System.out.println("got it");
            //if both tests were positive, run the choose-and-chase algorihtm
            // <editor-fold defaultstate="collapsed" desc="Choose-and-chase algorhitm">
            int univId;
            //A part
            Queue<Integer> scan = new LinkedList<>();
            scan.add(0);
            IntList knownIDs = new IntArrayList();
            //B part
            Queue<Map<Integer, Integer>> matchings = new LinkedList<>();
            matchings.add(new HashMap<>());
            while(scan.size() > 0) {
                //choose
//                System.err.println(scan);
                //take next set from scan
                int curSet = scan.remove();
//                System.err.println(scan);
                //calculate the cookie of the set
                Cookie acookie = getCookie(A, curSet, knownIDs);
//                System.out.println(acookie);
                //set univId to the pointer of the set
                univId = curSet;
                //add curSet to knownIDs
                knownIDs.add(curSet);
                //add to scan the pointers to all of the children of the current set that are not in knownIDs (neither in scan); if no such one exists and the queue is empty,
                //add to scan the first pointer to a set not in knownIDs; if this is still impossible, do nothing (the algorithm will terminate after all the remaining sets
                //in scan have been visited)
                int[] children = getChildren(A, curSet, knownIDs);
//                System.out.println(Arrays.toString(children));
                if(children.length > 0) for(Integer c : children) if(!scan.contains(c)){
                    scan.add(c);
                }
                if(scan.isEmpty()) {
                    for(int i = 0; i < A.getBlocksCount(); i++) if(!knownIDs.contains(i) && !scan.contains(i)) {
                        scan.add(i);
                        break;
                    }
                }
                //chase
                int size = matchings.size();
                //for size times:
                for(int i = 0; i < size; i++) {
                    //take next Map
                    Map curMatch = matchings.remove();
                    //(now it's deleted from the list, it will be readded if and only if there exist suitable continuations)
                    //for each set in B not present in curMatch:
                    for(int set = 0; set < B.getBlocksCount(); set++) if(!curMatch.containsKey(set)) {
                        //calculate the cookie of the set (relative to path)
                        Cookie bcookie = getCookie(B, set, curMatch);
//                        System.err.println(bcookie);
                        //if it matches with curSet's cookie, add to matchings a copy of curMatch with set's ID set to univId
                        if(bcookie.matches(acookie)) {
                            Map addMap = new HashMap(curMatch);
                            addMap.put(set, univId);
                            matchings.add(addMap);
                        }
                    }
//                    System.err.println("end");
                }
                if(matchings.isEmpty()) return false;
                //else, continue the algorithm
            }
//            System.out.println(knownIDs);
//            System.out.println("got it: " + matchings);
            // </editor-fold>
            //last check: for each HashMap in matchings, order the columns of the two matrices and see if they are the same; if so, return the first HashMap that 
            //passes the test
            // <editor-fold defaultstate="collapsed" desc="Last check">
            //order the columns of the first matrix (do it once)
//            System.out.println(A);
            Permutation pa = new Permutation(A.getPlayableElmsCount());
            GapList<RoaringBitmap> elmsa = A.getAllPlayableElms();
            boolean sorta;
            do {
                sorta = false;
                for(int i = 0; i < A.getPlayableElmsCount() - 1; i++) if(compare(elmsa.get(pa.perm(i)), elmsa.get(pa.perm(i + 1))) == -1) {
                    pa.swap(i, i+1);
                    sorta = true;
                }
            } while(sorta);
//            System.out.println(pa);
            //for each HashMap:
            for(Map<Integer, Integer> map : matchings) {
                //calculate permutation on the rows
                int[] pvals = new int[map.size()];
                for(int i = 0; i < map.size(); i++) pvals[i] = map.get(i);
                Permutation rows = new Permutation(map.size(), pvals);
                rows.invert();
//                System.out.println(rows);
                //order the columns of the second matrix
                Permutation pb = new Permutation(B.getPlayableElmsCount());
                GapList<RoaringBitmap> elmsb = B.getAllPlayableElms();
                boolean sortb;
                do {
                    sortb = false;
                    for(int i = 0; i < B.getPlayableElmsCount() - 1; i++) if(compare(elmsb.get(pb.perm(i)), elmsb.get(pb.perm(i + 1)), rows, rows) == -1) {
                        pb.swap(i, i+1);
                        sortb = true;
                    }
                } while(sortb);
//                System.out.println(pb);
                //check that the permutations work
                boolean works = true;
                for(int i = 0; i < A.getBlocksCount(); i++) {
                    for(int j = 0; j < A.getPlayableElmsCount(); j++) {
                        if(A.get(i, pa.perm(j)) != B.get(rows.perm(i), pb.perm(j))) {
                            works = false;
                            break;
                        }
                    }
                }
                //if so, return true
                if(works) return true;
            }
            return false;
            // </editor-fold>
        }
        else return false;
    }
    
    public Cookie getCookie(Game source, int row, IntList knownIDs) {
        return new Cookie(source.getBlock(row).getCardinality(), getInters(source, row, knownIDs));
    }
    
    public Cookie getCookie(Game source, int row, Map<Integer, Integer> IDs) {
        return new Cookie(source.getBlock(row).getCardinality(), getInters(source, row, IDs));
    }
    
    public ObjectList<Intersection> getInters(Game source, int row, IntList knownIDs) {
        //all the elements that belong to row and to another row at least
        ObjectList<RoaringBitmap> cols = new ObjectArrayList(source.getAllPlayableElms().stream().filter((c) -> (c.contains(row) && c.getCardinality() > 1)).collect(Collectors.toList()));
        //all the representatives of each different intersection (if two element belong to the same sets, they belong to their intersection and vice-versa)
        ObjectList<RoaringBitmap> distinct = new ObjectArrayList(cols.stream().distinct().collect(Collectors.toList()));
        
        ObjectList<Intersection> res = new ObjectArrayList<>();
        //for each representative, calculate the cardinality of the intersection and its datas
        for(RoaringBitmap roar : distinct) {
            int card = Collections.frequency(cols, roar);
            ObjectList<int[]> datas = new ObjectArrayList<>();
            for(int i = 0; i < roar.getCardinality(); i++) if(roar.select(i) != row) {
                int setID = knownIDs.contains(roar.select(i)) ? roar.select(i) : -1;
                int setCard = source.getBlock(roar.select(i)).getCardinality();
                datas.add(new int[]{setID, setCard});
            }
            Intersection inter = new Intersection(datas, card);
            res.add(inter);
        }
        return res;
    }
    
    public ObjectList<Intersection> getInters(Game source, int row, Map<Integer, Integer> IDs) {
        //all the elements that belong to row and to another row at least
        ObjectList<RoaringBitmap> cols = new ObjectArrayList(source.getAllPlayableElms().stream().filter((c) -> (c.contains(row) && c.getCardinality() > 1)).collect(Collectors.toList()));
        //all the representatives of each different intersection (if two element belong to the same sets, they belong to their intersection and vice-versa)
        ObjectList<RoaringBitmap> distinct = new ObjectArrayList(cols.stream().distinct().collect(Collectors.toList()));
        
        ObjectList<Intersection> res = new ObjectArrayList<>();
        //for each representative, calculate the cardinality of the intersection and its datas
        for(RoaringBitmap roar : distinct) {
            int card = Collections.frequency(cols, roar);
            ObjectList<int[]> datas = new ObjectArrayList<>();
            for(int i = 0; i < roar.getCardinality(); i++) if(roar.select(i) != row) {
                int setID = IDs.containsKey((roar.select(i))) ? IDs.get(roar.select(i)) : -1;
                int setCard = source.getBlock(roar.select(i)).getCardinality();
                datas.add(new int[]{setID, setCard});
            }
            Intersection inter = new Intersection(datas, card);
            res.add(inter);
        }
        return res;
    }
    
    public int[] getChildren(Game source, int row, IntList knownIDs) {
        IntList children = new IntArrayList();
        for(int i = 0; i < source.getBlocksCount(); i++) {
            if(i != row && !knownIDs.contains(i) && ImmutableRoaringBitmap.intersects(source.getBlock(row), source.getBlock(i))) children.add(i);
        }
        int[] res = children.toIntArray();
        return res;
    }
    
    public int compare(RoaringBitmap test, RoaringBitmap ref) {
        int min = test.getCardinality() < ref.getCardinality() ? test.getCardinality() : ref.getCardinality();
        for(int i = 0; i < min; i++) {
            if(test.select(i) < ref.select(i)) return 1;
            else if(test.select(i) > ref.select(i)) return -1;
        }
        if(test.getCardinality() == ref.getCardinality()) return 0;
        else if(test.getCardinality() > ref.getCardinality()) return 1;
        else return -1;
    }
    
    public int compare(RoaringBitmap test, RoaringBitmap ref, Permutation tp, Permutation rp) {
        if(rp.n() == tp.n()) {
            for(int i = 0; i < rp.n(); i++) {
                if(test.contains(tp.perm(i)) && !ref.contains(rp.perm(i))) return 1;
                else if(!test.contains(tp.perm(i)) && ref.contains(rp.perm(i))) return -1;
            }
            return 0;
        }
        else throw new IllegalArgumentException("The permutations given differ in length");
    }
    
    public double av(double p, Game source, int col) {
        double res = Math.pow(source.getAllBlocks().stream().filter((r) -> (r.contains(col))).mapToDouble((r) -> (Math.pow(r.getCardinality(), p))).sum() / source.getElm(col).getCardinality(), p);
        return res;
    }
    
    public void startSearch(List<Game> l) {
        Thread t;
        t = new Thread(() -> {
            l.parallelStream().forEach((Game s) -> {
                //find the minimal ancestors
                PriorityQueue<Game> positives = new ObjectArrayFIFOQueue<>();
                boolean pos1;
                positives.enqueue(s);
                // <editor-fold defaultstate="collapsed" desc="Cut off the blocks">
                do {
                    pos1 = false;
                    Game b = positives.first();
                    for(int i = 0; i < b.getBlocksCount(); i++) {
                        Game b1 = b.copy();
                        b1.removeBlock(i);
                        if(isPosB(b1.copy(), 0, true)) {
                            positives.enqueue(b1);
                            pos1 = true;
                        }
                    }
                    if(pos1) positives.dequeue();
                } while(pos1);
                //</editor-fold>
                // <editor-fold defaultstate="collapsed" desc="Add as many elements as possible">
                boolean pos2;
                do {
                    pos2 = false;
                    Game b = positives.first();
                    for(int i = 0; i < b.getBlocksCount(); i++) {
                        for(int j = 0; j < b.getPlayableElmsCount(); j++) if(!b.get(i, j)) {
                            Game b1 = b.copy();
                            b1.set(i, j, true);
                            if(isPosB(b1.copy(), 0, false)) {
                                positives.enqueue(b1);
                                pos2 = true;
                            }
                        }
                    }
                    if(pos2) positives.dequeue();
                } while(pos2);
                //</editor-fold>

                if(!positives.isEmpty()) {
                    while(!positives.isEmpty()) {
                        Game b = positives.dequeue();
                        b.trim();
                        String key = b.getBlocksCount() + "|" + b.getPlayableElmsCount();
                        //If not already in the db, add it
                        // <editor-fold defaultstate="collapsed" desc="Add to the DB">
                        if(!isInDB(b)) {
                            System.out.println(b);
                            //use the db but and update it (at the cost of re-examining this game, you can examine all the minimal games contained within it)
                            Tree tree = isPositive(b.copy(), true, false);
                            System.out.println();
                            Record rec = new Record(b, tree);
                            //if there are already games with the same number of blocks and elements, add rec to the array of the other games
                            if(table.containsKey(key)) {
                                int[][] oldRecs = (int[][]) table.get(key);
                                int[][] newRecs = ArrayUtils.add(oldRecs, rec.getCompact());
                                table.put(key, newRecs);
                            }
                            //else, add a new entry
                            else {
                                table.put(key, new int[][] {rec.getCompact()});
                            }
                        }
                        //</editor-fold>
                    }
                    //persist all changes to disk
                    db.commit();
                }
            });
            l.clear();
        });
        t.start();
    }
}
