/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package setgame;

import java.util.List;
import org.roaringbitmap.buffer.*;
import it.unimi.dsi.fastutil.ints.*;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Comparator;
import org.magicwerk.brownies.collections.*;
import org.roaringbitmap.RoaringBitmap;

/**
 *
 * @author Marco
 */

public class Game {
    //list of all blocks, stored off-heap
    protected final GapList<ImmutableRoaringBitmap> rb;
    //list of the playable elements, stored off-heap
    protected MutableRoaringBitmap elmMap;
    //number of total elements with which the game is played
    protected int numElms;
    
    public Game(int blocks, int elms, List<RoaringBitmap> blockBitmaps) {
        //create the list of off-heap bitmaps from the given one
        this.rb = new GapList<>(blocks);
        for(int i = 0; i < blocks; i++) {
            this.rb.add(toImmutableRoaringBitmap(blockBitmaps.get(i)));
        }
        //create the off-heap bitmap representing the playable elements of the game
        this.numElms = elms;
        resetPlayableElms();
    }
    
    private Game(GapList<ImmutableRoaringBitmap> bitmaps, MutableRoaringBitmap playableElms, int elms) {
        this.rb = bitmaps;
        this.elmMap = playableElms;
        this.numElms = elms;
    }
    
    //part of the method is copied from RoaringBitmap's javadoc
    private static ImmutableRoaringBitmap toImmutableRoaringBitmap(RoaringBitmap rb) {
        try {
            //improve compression
            rb.runOptimize();
            //next, create the off-heap ByteBuffer where the data will be stored
            ByteBuffer outbb = ByteBuffer.allocateDirect(rb.serializedSizeInBytes());
            //then serialize on a custom OutputStream
            rb.serialize(new DataOutputStream(new OutputStream(){
                ByteBuffer mBB;
                OutputStream init(ByteBuffer mbb) {
                    mBB = mbb;
                    return this;
                }
                
                @Override
                public void close() {}
                
                @Override
                public void flush() {}
                
                @Override
                public void write(int b) {
                    mBB.put((byte) b);
                }
                
                @Override
                public void write(byte[] b) {
                    mBB.put(b);
                }
                
                @Override
                public void write(byte[] b, int off, int l) {
                    mBB.put(b,off,l);
                }
            }.init(outbb)));
            //reposition
            outbb.rewind();
            //outbb will now contain a serialized, off-heap version of your bitmap
//            System.out.println(new ImmutableRoaringBitmap(outbb));
            //pass it to the ImmutableRoaringBitmap constructor
            return new ImmutableRoaringBitmap(outbb);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            return null;
        }
    }
    
    public int absElm(int relElm) {
        return this.elmMap.select(relElm);
    }
    
    public int relElm(int absElm) {
        return this.elmMap.rank(absElm) - 1;
    }
    
    public ImmutableRoaringBitmap getPlayableElements() {
        return this.elmMap.toImmutableRoaringBitmap();
    }
    
    public void setPlayableElements(RoaringBitmap elmMap) {
        this.elmMap = toImmutableRoaringBitmap(elmMap).toMutableRoaringBitmap();
    }
    
    public int getBlocksCount() {
        return this.rb.size();
    }
    
    public int getPlayableElmsCount() {
        return this.elmMap.getCardinality();
    }
    
    public int getTotalElmsCount() {
        return this.numElms;
    }
    
    public boolean get(int block, int absoluteElm) {
        return this.rb.get(block).contains(absoluteElm);
    }
    
    public void set(int block, int absoluteElm, boolean val) {
        RoaringBitmap cache = this.rb.get(block).toRoaringBitmap();
        if(val) cache.add(absoluteElm);
        else cache.remove(absoluteElm);
        this.rb.set(block, toImmutableRoaringBitmap(cache));
    }

    public void addElm(int absoluteElm) {
        this.elmMap.add(absoluteElm);
    }
    public void addAllElms(int... absoluteElms) {
        for(int elm : absoluteElms) this.elmMap.add(elm);
    }
    public void addAllElms(IntList absoluteElms) {
        absoluteElms.stream().forEach((elm) -> {
            this.elmMap.add(elm);
        });
    }
    
    //fast method, just updates the elmMap bitmap; then, when retrieving the blocks, they are ANDed with elmMap, so that the element is effectively removed from them
    public void removeElm(int absoluteElm) {
        this.elmMap.remove(absoluteElm);
    }
    public void removeAllElms(int... absoluteElms) {
        for(int elm : absoluteElms) this.elmMap.remove(elm);
    }
    public void removeAllElms(IntList absoluteElms) {
        absoluteElms.stream().forEach((elm) -> {
            this.elmMap.remove(elm);
        });
    }
    
    public void addBlock(int block, ImmutableRoaringBitmap blockBitmap) {
        this.rb.add(block, blockBitmap);
    }
    public void addBlock(int block, RoaringBitmap blockBitmap) {
        this.rb.add(block, toImmutableRoaringBitmap(blockBitmap));
    }
    
    public void removeBlock(int block) {
        this.rb.remove(block);
    }
    public void removeAllBlocks(IntList blocks) {
        Collections.sort(blocks, Comparator.reverseOrder());
        for(int i = 0; i < blocks.size(); i++) this.rb.remove(blocks.getInt(i));
    }
    
    public ImmutableRoaringBitmap getBlock(int block) {
        return ImmutableRoaringBitmap.and(this.rb.get(block), this.elmMap);
    }
    public GapList<ImmutableRoaringBitmap> getAllBlocks() {
        GapList<ImmutableRoaringBitmap> res = new GapList<>(this.rb.size());
        for(int i = 0; i < this.rb.size(); i++) {
            res.add(ImmutableRoaringBitmap.and(this.rb.get(i), this.elmMap));
        }
        return res;
    }
    
    public ImmutableRoaringBitmap andAll(int... blocks) {
        ImmutableRoaringBitmap[] blocksToAnd = new ImmutableRoaringBitmap[blocks.length];
        for(int i = 0; i < blocks.length; i++) blocksToAnd[i] = this.rb.get(blocks[i]);
        ImmutableRoaringBitmap cache = BufferFastAggregation.and(blocksToAnd);
        return ImmutableRoaringBitmap.and(cache, this.elmMap);
    }
    public ImmutableRoaringBitmap andAll(IntList blocks) {
        ImmutableRoaringBitmap[] blocksToAnd = new ImmutableRoaringBitmap[blocks.size()];
        for(int i = 0; i < blocks.size(); i++) blocksToAnd[i] = this.rb.get(blocks.getInt(i));
        ImmutableRoaringBitmap cache = BufferFastAggregation.and(blocksToAnd);
        return ImmutableRoaringBitmap.and(cache, this.elmMap);
    }
    
    public RoaringBitmap getElm(int elm) {
        RoaringBitmap res = new RoaringBitmap();
        for(int j = 0; j < this.rb.size(); j++) if(this.rb.get(j).contains(elm)) res.add(j);
        return res;
    }
    public GapList<RoaringBitmap> getAllPlayableElms() {
        GapList<RoaringBitmap> res = new GapList<>(getPlayableElmsCount());
        for(int i = 0; i < getPlayableElmsCount(); i++) {
            RoaringBitmap elm = new RoaringBitmap();
            for(int j = 0; j < this.rb.size(); j++) if(this.rb.get(j).contains(this.elmMap.select(i))) elm.add(j);
            res.add(elm);
        }
        return res;
    }
    
    public IntList getAllPlayableElmsDegrees() {
        IntList res = new IntArrayList(this.elmMap.getCardinality());
        for(int i = 0; i < this.elmMap.getCardinality(); i++) {
            int sum = 0;
            for(int j = 0; j < this.rb.size(); j++) {
                if(this.rb.get(j).contains(this.elmMap.select(i))) sum++;
            }
            res.add(sum);
        }
        return res;
    }
    
    public boolean isElmBelongingToAnyBlock(int absoluteElm) {
        for(int i = 0; i < this.rb.size(); i++) if(this.rb.get(i).contains(absoluteElm)) return true;
        return false;
    }
    
    public final void resetPlayableElms() {
        RoaringBitmap map = new RoaringBitmap();
        map.add(0, this.numElms);
        this.elmMap = toImmutableRoaringBitmap(map).toMutableRoaringBitmap();
    }
    
    public void trim() {
        for(int i = 0; i < this.rb.size(); i++) {
            RoaringBitmap cache = new RoaringBitmap();
            for(int j = 0; j < this.elmMap.getCardinality(); j++) {
                if(this.rb.get(i).contains(this.elmMap.select(j))) cache.add(j);
            }
            this.rb.set(i, toImmutableRoaringBitmap(cache));
        }
        this.numElms = this.elmMap.getCardinality();
        resetPlayableElms();
    }
    
    public Game copy() {
        return new Game(this.rb.copy(), this.elmMap.clone(), this.numElms);
    }
    
    @Override
    public String toString() {
        if(this.getBlocksCount() == 0 || this.getPlayableElmsCount() == 0) return "[" + this.getBlocksCount() + "x" + this.getPlayableElmsCount() + "]";
        String res = "";
        Game copy = this.copy();
        copy.trim();
        for(ImmutableRoaringBitmap row : copy.rb) {
            for(int i = 0; i < this.getPlayableElmsCount(); i++) {
                char num = (row.contains(i)) ? '1' : '0';
                res = res + num + " ";
            }
            res = res.substring(0, res.length() - 1);
            res = res + "\n" + "\n";
        }
        res = res.substring(0, res.length() - 2);
        return res;
    }
    
    public String toCompactString() {
        String res = this.getBlocksCount() + "&" + this.getPlayableElmsCount() + "&";
        for(ImmutableRoaringBitmap block : this.rb) {
            if(block.isEmpty()) res = res + "&";
            else res = res + block.toString().substring(1, block.getCardinality() * 2) + "&";
        }
        res = res.substring(0, res.length() - 1);
        return res;
    }
    
    public static Game fromCompactString(String s) {
        String[] rows = s.trim().split("&");
        int rs = Integer.parseInt(rows[0]);
        int cs = Integer.parseInt(rows[1]);
        List<RoaringBitmap> roars = new GapList<>(rs);
        for(int i = 2; i < rows.length; i++) {
            RoaringBitmap roar = new RoaringBitmap();
            if(rows[i].equals("e")) roars.add(roar);
            else {
                String[] vals = rows[i].split(",");
                for(int j = 0; j < vals.length; j++) roar.add(Integer.valueOf(vals[j]));
                roars.add(roar);
            }
        }
        return new Game(rs, cs, roars);
    }
}
