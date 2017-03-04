/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hexplode;

import java.util.Arrays;
import it.unimi.dsi.fastutil.objects.*;

/**
 *
 * @author Marco
 */
public class Intersection {
    //for performance optimization, replace IDs with sets (so that you can check more accurately if two Intersection effectively match)
//    private int[] IDs;
    //format: setID, setCard
    private final ObjectList<int[]> sets;
    private final int card;
    
    public Intersection(ObjectList<int[]> setDatas, int intersCard) {
        sets = setDatas;
        card = intersCard;
        sort();
    }
    
    private void sort() {
        sets.sort((int[] o1, int[] o2) -> // <editor-fold defaultstate="collapsed" desc="Compare basing on the entries">
        {
            if(o1[0] > o2[0]) return 1;
            else if(o1[0] < o2[0]) return -1;
            else {
                if(o1[1] > o2[1]) return 1;
                else if(o1[1] < o2[1]) return -1;
                else return 0;
            }
        });
        // </editor-fold>
    }
        
    protected int getCard() {
        return card;
    }
    
    protected ObjectList<int[]> getSets() {
        sort();
        return sets;
    }
    
    public boolean matches(Intersection ref) {
        sort();
        if(card == ref.card && sets.size() == ref.sets.size()) {
            for(int i = 0; i < sets.size(); i++) {
                if(sets.get(i)[0] != ref.sets.get(i)[0] || sets.get(i)[1] != sets.get(i)[1]) {
                    return false;
                }
            }
            return true;
        }
        else return false;
    }
    
    @Override
    public String toString() {
        return "{sets(ID and card):" + Arrays.deepToString(sets.toArray(new int[0][])) + ", interCard:" + card + "}";
    }
}
