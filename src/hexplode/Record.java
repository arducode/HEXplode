/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hexplode;

import setgame.Game;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.magicwerk.brownies.collections.GapList;
import org.roaringbitmap.RoaringBitmap;

/**
 *
 * @author Marco
 */
public class Record {
    private final int[] compact;
    
    public Record(int[] comp) {
        compact = comp;
    }
    
    public Record(Game g, Tree t) {
        int blocks = g.getBlocksCount();
        int elms = g.getPlayableElmsCount();
        int[] game = new int[blocks * elms + 2];
        game[0] = blocks;
        game[1] = elms;
        for(int i = 0; i < blocks; i++) {
            for(int j = 0; j < elms; j++) {
                if(g.get(i, g.absElm(j))) game[i * elms + j + 2] = 1;
                else game[i * elms + j + 2] = 0;
            }
        }
        
        int[][] ftr = t.getList();
        int[] tree = new int[ftr.length * 4];
        for(int i = 0; i < ftr.length; i++) {
            tree[i*4] = ftr[i][0];
            tree[i*4+1] = ftr[i][1];
            tree[i*4+2] = ftr[i][2];
            tree[i*4+3] = ftr[i][3];
        }
        compact = ArrayUtils.addAll(game, tree);
    }
    
    public int[] getCompact() {
        return compact;
    }
    
    public Game getGame() {
        int rows = compact[0];
        int cols = compact[1];
        int[] game = Arrays.copyOf(compact, rows * cols + 2);
        GapList<RoaringBitmap> list = new GapList<>(rows);
        for(int i = 0; i < rows; i++) {
            RoaringBitmap cache = new RoaringBitmap();
            for(int j = 0; j < cols; j++) {
                if(game[i * cols + j + 2] == 1) cache.add(j);
            }
            list.add(cache);
        }
        Game m = new Game(rows, cols, list);
        return m;
    }
    
    public Tree getTree() {
        int rows = compact[0];
        int cols = compact[1];
        int[] tree = Arrays.copyOfRange(compact, rows * cols + 2, compact.length);
        int[][] ftr = new int[tree.length / 4][4];
        for(int i = 0; i < ftr.length; i++) {
            ftr[i][0] = tree[i*4];
            ftr[i][1] = tree[i*4+1];
            ftr[i][2] = tree[i*4+2];
            ftr[i][3] = tree[i*4+3];
        }
        Tree t = new Tree(ftr);
        return t;
    }
    
    public int[] getDim() {
        return new int[] {compact[0], compact[1]};
    }
}
