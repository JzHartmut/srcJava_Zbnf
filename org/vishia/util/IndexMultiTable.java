package org.vishia.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.vishia.bridgeC.AllocInBlock;



/**This class contains sorted references of objects (values) with a sorting key (usual String or Integer) 
 * in one or more tables of a defined size. 
 * <ul>
 * <li>This class can be used similar like {@link java.util.TreeMap}. 
 *   In opposite to a {@link java.util.TreeMap} the key need not be unique for all values.
 *   More as one value can have the same key. The method {@link #get(Object)} defined in 
 *   {@link java.util.Map} searches the first Object with the given key. 
 *   The method {@link #put(Comparable, Object)} replaces an existing value with the same key like defined in Map
 *   where the methods {@link #add(Comparable, Object)}, {@link #addBefore(Comparable, Object, Object)}
 *   and {@link #append(Comparable, Object)} puts the new value with an existing key beside the last existing one.  
 * <li>There is a method {@link #iterator(Comparable)} which starts on the first occurrence of the search key
 *   or after that key which is one lesser as the key.
 *   It iterates to the end of the whole collection. The user can check the key in the returned {@link java.util.Map.Entry}
 *   whether the key is proper. In this kind a sorted view of a part of the content can be done.
 * <li>The container consist of one or more instances which have a limited size. If a table in one instance is filled,
 *   two new instances will be created in memory which contains the half amount of elements and the original
 *   instance is changed to a hyper table. The size of the instances are equal and less. It means, the search time
 *   is less and the memory requirement is constant. In this kind this class is able to use in C programming
 *   with a block heap of equal size blocks in an non-dynamic memory management system (long living
 *   fast realtime system).    
 * </ul>
 * This class contains the table of objects and the table of key of type Comparable. 
 * The tables are simple arrays of a fix size.
 * <br>
 * An instance of this class may be either a hyper table which have some children, 
 * or it is a end-table of keys and its references.
 * A end table may have a parent, which is a hyper table, and some sibling instances.
 * If there isn't so, only the end-table exists. It is, if the number of objects is less than 
 * the max capacity of one table, typical for less data. 
 * In that case, the data structure is very simple. Commonly a search process should regard 
 * the tree of tables. But the tree is not deep, because it is a square function. 
 * With a table of 100 entries, a 2-ary tree may contain up to 10000.
 * <br>
 * Objects with the same key can stored more as one time.
 *   
 * <br> 
 * This concept have some advantages:
 * <ul>
 * <li>A system of simple arrays for the key and a parallel array of the associated object
 *     allows fast search using java.util.Arrays.binarysearch(int[], int). 
 *     It is a simple algorithm. Only less memory objects are needed. 
 *     It doesn't need nodes for TreeMap etc. 
 *     It is a simple layout for fast embedded control applications. 
 * <li>But using only one array of keys and one array of objects, 
 *   <ul>
 *   <li>first, the number of objects are limited or a large portion of memory should be provided,
 *   <li>second, if a new object is inserted, and the amount of objects is large, 
 *       a larger calculation time of System.arraycopy() would be necessary.
 *   </ul>
 * <li>Therefore the portioning in more as one table needs no large memory objects. 
 *     If the number of objects is increased, a additional new memory object with limited size 
 *     is necessary - no resize of existing object.
 * <li>The calculation time for insertion an object is limited because one table is limited,
 *     also if the number of objects is large.
 * <li>If thread safety is necessary, only that table should be locked or exclusive copied, 
 *     which is touched, for a limited time too. So the number of clashes is less. 
 *     In the time this class doesn't support thread safety, but it is planned in two ways, 
 *     using synchronized and using the lock free atomic mechanism. 
 * <li>Using lock free programming, the change of data should be done in an copy of the table.
 *     This copy have less memory size.
 * <li>The limited number of memory space is convenient for the application in fast real time systems.
 * </ul>                     
 * 
 * @author Hartmut Schorrig
 *
 * @param <Type>
 */
public class IndexMultiTable<Key extends Comparable<Key>, Type> 
implements Map<Key,Type>, Iterable<Type>  //TODO: , NavigableMap<Key, Type>
{
  
  /**Version, history and license.
   * <ul>
   * <li>2014-01-12 Hartmut chg: {@link #sortin(int, Comparable, Object)} new Algorithm
   * <li>2014-01-12 Hartmut chg: toString better for viewing, with all keys 
   * <li>2013-12-02 Hartmut new: Implementation of {@link #remove(Object)} was missing, {@link #searchInTables(Comparable, boolean, IndexBox)}
   *   restructured. It returns the table and index, able to use for internal searching. 
   * <li>2013-09-15 Hartmut new: Implementation of {@link #delete(int)} and {@link EntrySetIterator#remove()}.
   * <li>2013-09-15 Hartmut chg: rename and public {@link #search(Comparable, boolean, boolean[])}.  
   * <li>2013-09-07 Hartmut chg: {@link #put(Comparable, Object)} should not create more as one object
   *   with the same key. Use add to do so.
   * <li>2013-08-07 Hartmut improved.
   * <li>2013-04-21 Hartmut created, derived from {@link IndexMultiTableInteger}.
   * <li>2009-05-08 Hartmut corr/bugfix: IteratorImpl(IndexMultiTableInteger<Type> firstTable, int startKey) used in iterator(startkey):
   *                    If a non-exakt start key is found, the iterator starts from the key after it. 
   *                    If no data are available, hasnext() returns false now. TODO test if one hyperTable contains the same key as the next, some more same keys!
   * <li>2009-04-28 Hartmut new: iterator(key) to start the iterator from a current position. It is the first position with the given key. 
   *                    corr: Some empty methods are signed with xxxName, this methods were come from planned but not implemented interface
   *                    meditated: implement ListInterface instead Interface to supply getPrevious() to iterate starting from a key forward and backward.
   *                    corr: binarySearch self implemented, regarding the first occurrence of a key.
   * <li>2009-04-26 Hartmut meditated: Several key types in one file isn't good! The search routines are optimal only if the key type is fix.
   *                                There are some adequate classes necessary for int, long keys.
   *                    docu
   *                    chg: Using of class AllocInBlock to get the size of a block.
   *                    corr: Now more deepness as 2 tables is programmed and tested. Older versions support only 2 tables.
   *                          It was 1000000 entries max. with a table size of 1000.
   * <li>2009-03-01 Hartmut new: IndexMultiTableInteger(int size, char type): in preparation of using, functionality not ready yet.
   *                    planned: Type may be int, long, String, size are able to choose. The arrays should be assigned not as embedded instances in C. 
   *                    new: method get() does anything, not tested in all cases.
   * <li>2007-06-00 Hartmut Created.                  
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  public static final int version = 20130807;

  static int identParent_ = 100;
  
  final int identParent = ++identParent_; 
  
  final Key minKey__;
  
  final Key maxKey__;
  
  final Provide<Key> provider;
  
  private enum KindofAdd{ addOptimized, addLast, addBefore, replace};
  
  /**The maximal nr of elements in a block, maximal value of sizeBlock.
   * It is the same value as obj.length or key.length. */
  protected final static int maxBlock = AllocInBlock.restSizeBlock(IndexMultiTable.class, 160) / 8; //C: 8=sizeof(int) + sizeof(Object*) 

  /**Array of keys, there are sorted ascending. The same key can occure some times. */ 
  //private final Comparable<Key>[] key = new Comparable[maxBlock];
  
  protected final Key[] aKeys; // = new Key[maxBlock];

  /**Array of objects appropritate to the keys. */
  protected final Object[] aValues = new Object[maxBlock];

  /**The parent if it is a child table. */
  private IndexMultiTable<Key, Type> parent;

  /**actual number of objects stored in this table. */
  private int sizeBlock;

  /**actual number of objects stored in the whole table tree. */
  private int sizeAll;

  private boolean shouldCheck = false;
  
  /**True, than {@link #aValues} contains instances of this class too. */
  protected boolean isHyperBlock;
  
  /**modification access counter for Iterator. */
  //@SuppressWarnings("unused")
  protected int modcount;
  
  /**Index of this table in its parent. */
  private int ixInParent;
 
  
  
  /**This class is the Iterator for the outer class. Every {@link #iterator()}-call
   * produces one instance. It is possible to create some instances simultaneously.
   * @author HSchorrig
   *
   * @param <Type>
   */
  protected final class IteratorImpl implements Iterator<Type>
  {

    /**The helper contains the values of the iterator. Because there are a tree of tables,
     * the current IteratorHelper of the current table depths is referenced.
     * A IteratorHelper contains a parent and child reference. If the end of table
     * of the current depth level is reached, the parent of the current helper is referenced here
     * than, the next table is got, than the child is initialized and referenced here.  
     * 
     */
    public IndexMultiTable.IteratorHelper<Key, Type> helper;
    
    /**True if hasNext. The value is valid only if {@link bHasNextProcessed} is true.*/
    private boolean bHasNext = false;
      
    /**true if hasnext-test is done. */
    private boolean bHasNextProcessed = false;
    
    @SuppressWarnings("unused")
    private int modcountxxx;
    
    /**Only for test. */
    private Key lastkey; 
    
    protected IteratorImpl(IndexMultiTable<Key, Type> firstTable)
    { helper = new IteratorHelper<Key, Type>(null);
      helper.table = firstTable;
      helper.idx = -1;
      lastkey = minKey__;
    }
    
    
    /**Ctor for the Iterator with a range.
     * @param firstTable
     * @param startKey
     * @param endKey
     */
    IteratorImpl(IndexMultiTable<Key, Type> firstTable, Key startKey)
    { helper = new IteratorHelper<Key, Type>(null);
      helper.table = firstTable;
      helper.idx = -1;
      lastkey = minKey__;
      while(helper.table.isHyperBlock)
      { //call it recursively with sub index.
        int idx = binarySearchFirstKey(helper.table.aKeys, 0, helper.table.sizeBlock, startKey); //, sizeBlock, key1);
        if(idx < 0)
        { /**an non exact found, accept it.
           * use the table with the key lesser than the requested key
           */
          idx = -idx-2; //insertion point -1 
          if(idx < 0)
          { /**Use the first table if the first key in the first table is greater. */
            idx = 0; 
          }
        }
        //idx -=1;  //?
        helper.idx = idx;
        @SuppressWarnings("unchecked")
        IndexMultiTable<Key, Type> childTable = (IndexMultiTable<Key, Type>)helper.table.aValues[helper.idx];
        helper.childHelper = new IteratorHelper<Key, Type>(helper); 
        
        helper.childHelper.table = childTable;
        helper = helper.childHelper;  //use the sub-table to iterate.          
      }
      int idx = binarySearchFirstKey(helper.table.aKeys, 0, helper.table.sizeBlock,  startKey); //, sizeBlock, key1);
      if(idx < 0)
      { /**an non exact found, accept it.
         * start from the element with first key greater than the requested key
         */
        idx = -idx-1;  
      }
      helper.idx = idx;
      /**next_i() shouldn't called, because the helper.idx is set with first occurrence. */
      bHasNextProcessed = true;
      /**next() returns true always, except if the idx is 0 and the table contains nothing. */
      bHasNext =  idx < helper.table.sizeBlock;
    }
    
    
    public boolean hasNext()
    { if(!bHasNextProcessed)
      { next_i();  //call of next set bHasNext!
      }
      return bHasNext;
    }

    
    
    
    
    /**Implements the standard behavior for {@link java.util.Iterator#next()}.
     * For internal usage the {@link #helper} is set.
     * With them 
     */
    @SuppressWarnings("unchecked")
    public Type next()
    { if(!bHasNextProcessed)
      {  next_i();
      }
      if(bHasNext)
      { bHasNextProcessed = false;  //call it at next access!
        IndexMultiTable<Key, Type> table = helper.table;
        assert(compare(table.aKeys[helper.idx],lastkey) >= 0);  //test
        if(compare(table.aKeys[helper.idx],lastkey) < 0) throw new RuntimeException("assert");
        if(compare(table.aKeys[helper.idx],lastkey) < 0)
          stop();
        lastkey = table.aKeys[helper.idx];
        return (Type)table.aValues[helper.idx];
      }
      else return null;
    }

    Key getKeyForNext(){ return lastkey; }
    
    /**executes the next(), on entry {@link bHasNextProcessed} is false.
     * If the table is a child table and its end is reached, this routine is called recursively
     * with the now current parent, typical the parent contains a child table
     * because the table is a hyper table. Than the child helper is initialized
     * and reused, and this routine will be called a third time, now with the new child:
     * <pre>
     * child: end of table; parent: next table; child: test next table.
     * </pre>
     * If the tree of tables is deeper than two, and the end of a child and also the parent table
     * is reached, this routine is called recursively more as three times.
     * The maximum of recursively call depends on the deepness of the table tree.
     */
    @SuppressWarnings("unchecked")
    private void next_i()
    {
      bHasNext = ++helper.idx < helper.table.sizeBlock;  //next in current table.
      if(bHasNext)
      { if(helper.table.isHyperBlock)
        { //
          IndexMultiTable<Key, Type> childTable = (IndexMultiTable<Key, Type>)helper.table.aValues[helper.idx];
          if(helper.childHelper == null)
          { //no child yet. later reuse the instance of child.
            helper.childHelper = new IteratorHelper(helper); 
          }
          helper.childHelper.idx = -1;  //increment as first action.
          helper.childHelper.table = childTable;
          helper = helper.childHelper;  //use the sub-table to iterate.          
        }
        else
        { //else: bHasNext is true.
          bHasNextProcessed = true;
        }
      }
      else
      { if(helper.parentIter != null)
        { //no next, but it is a sub-table. This sub-table is ended.
          //a next obj may be exist in the sibling table.
          helper.table = null;  //the child helper is unused now.
          helper = helper.parentIter; //to top of IteratorHelper, test there.
          /*Because bHasNextProcessed is false, this routine is called recursively, see method description. */
        }
        else
        { //else: bHasNext is false, it is the end.
          bHasNextProcessed = true;
        }
      }
      if(!bHasNextProcessed)
      { next_i();
      }
    }
    
    
    
    public void remove()
    {
      // TODO Auto-generated method stub
      
    }

    private void stop()
    { //debug
    }
    
    
  }

  
  
  
  /**This class contains the data for a {@link IndexMultiTable.IteratorImpl}
   * for one table.
   * 
   *
   */ 
  private static class IteratorHelper<Key extends Comparable<Key>, Type>
  {
    /**If not null, this helper is associated to a deeper level of table, the parent
     * contains the iterator value of the higher table.*/
    protected final IteratorHelper<Key, Type> parentIter;
    
    /**If not null, an either an empty instance for a deeper level of tables is allocated already 
     * or the child is used actual. The child is used, if the child or its child 
     * is the current IteratorHelper stored on {@link IteratorImpl#helper}. */ 
    protected IteratorHelper<Key, Type> childHelper;
    
    /**Current index in the associated table. */ 
    protected int idx;
    
    /**The associated table, null if the instance is not used yet. */
    IndexMultiTable<Key, Type> table;
    
    IteratorHelper(IteratorHelper<Key, Type> parentIter)
    { this.parentIter = parentIter;
      this.table = null;
      idx = -1;
    }
  }
  
  
  
  /**constructs an empty instance without data. */
  public IndexMultiTable(Provide<Key> provider)
  { //this(1000, 'I');
    this.provider = provider;
    this.aKeys = provider.createSortKeyArray(maxBlock);
    this.minKey__ = provider.getMinSortKey();
    this.maxKey__ = provider.getMaxSortKey();
    for(int idx = 0; idx < maxBlock; idx++){ aKeys[idx] = maxKey__; }
    sizeBlock = 0;
    ixInParent = -1;
  }
  

  
  
  
  
  /**constructs an empty instance without data with a given size and key type. 
   * @param sizeAll The size of one table.
   * @param type one of char I L or s for int, long or String key.
   */
  /*
  public IndexMultiTable(int size, char type)
  { //TODO: allocate the fields etc. with given size. 
    for(int idx = 0; idx < maxBlock; idx++){ key[idx] = maxKey__; }
    sizeBlock = 0;
    
  }
  */
  
  
  
  public void shouldCheck(boolean val){ shouldCheck = val; }
  

  
  /**Puts the (key - value) pair to the container. An existing value with the same key will be replaced
   * like described in the interface. If more as one value with this key are existing, the first one
   * will be replaced only. 
   * <br>
   * See {@link #add(Comparable, Object)} and {@link #append(Comparable, Object)}.
   * @param key
   * @param value
   * @return The last value with this key if existing.
   */
  @Override public Type put(Key key, Type value){
    return putOrAdd(key, value, null, KindofAdd.replace);
  }

  
  /**Adds the (key - value) pair to the container. All existing values with the same key will be retained.
   * If one or some values with the same key are contained in the container already, the new value is placed
   * in a non-defined order with that values. The order depends from the search algorithm. It is the fastest
   * variant to sort in a new value. This method should be used if the order of values with the same key
   * are not regardless.
   * <br>
   * See {@link #put(Comparable, Object)} and {@link #append(Comparable, Object)}.
   * @param key
   * @param value
   * @return The last value with this key if existing.
   */
  public void add(Key key, Type value){
    putOrAdd(key, value, null, KindofAdd.addOptimized);
  }

  
  /**Appends the (key - value) pair to the container. All existing values with the same key will be retained.
   * If one or some values with the same key are contained in the container already, the new value is placed
   * in order after all other. The values with the same key are sorted by its append order.
   * If some more values with the same key are existing, the searching can be need some calculation time.
   * But the time is less if the table size is less. 
   * This method should be used if the order of values with the same key is need.
   * <br>
   * See {@link #put(Comparable, Object)} and {@link #append(Comparable, Object)}.
   * @param key
   * @param value
   * @return The last value with this key if existing.
   */
  public void append(Key key, Type obj){
    if(key.equals("ckgro") && sizeAll == 19)
      Assert.stop();
    putOrAdd(key, obj, null, KindofAdd.addLast);
  }

  
  /**Adds the (key - value) pair to the container. All existing values with the same key will be retained.
   * The value is placed before the given next value which must have the same key. If the nextValue is not found,
   * the key is placed in a non deterministic order.
   * If some more values with the same key are existing, the searching can be need some calculation time.
   * This method should be used only if that order of values with the same key is need.
   * <br>
   * See {@link #put(Comparable, Object)}, {@link #add(Comparable, Object)} and {@link #append(Comparable, Object)}.
   * @param key
   * @param value
   * @return The last value with this key if existing.
   */
  public void addBefore(Key key, Type value, Type valueNext){
    putOrAdd(key, value, valueNext, KindofAdd.addBefore);
  }

  
  
  /**Put a object in the table. The key may be ambiguous, a new object with the same key is placed
   * after an containing object with this key. If the table is full, a new table will be created internally.
   *  
   * 
   */
  private Type putOrAdd(Key sortKey, Type value, Type valueNext, KindofAdd kind)
  { //NOTE: returns inside too.
    check();
    Type lastObj = null;
    if(isHyperBlock && sizeBlock == maxBlock){
      //split the block because it may be insufficient. If it is insufficient,
      //the split from child to parent does not work. split yet.
      if(parent !=null){
        IndexMultiTable<Key, Type> sibling = splitIntoSibling(-1, null, null);
        if(compare(sibling.aKeys[0],sortKey) <=0){
          //do it in the sibling.
          return sibling.putOrAdd(sortKey, value, valueNext, kind);
        }
      } else {
        splitTopLevel(-1, null, null);
      }
    }
    check();
    //place object with same key after the last object with the same key.
    int idx = Arrays.binarySearch(aKeys, sortKey); //, sizeBlock, key1);
    if(idx < 0) {
      //not found
      idx = -idx-1;  //NOTE: sortin after that map, which index starts with equal or lesser index.
      if(isHyperBlock)
      { //call it recursively with sub index.
        //the block with the range 
        idx -=1;
        if(idx<0)
        { //a index less than the first block is getted.
          //sortin it in the first block.
          idx = 0;
          IndexMultiTable<Key, Type> parents = this;
          while(parents != null)
          { //if(key1 < key[0])
            if(compare(sortKey,aKeys[0]) <0)
            { aKeys[0] = sortKey; //correct the key, key1 will be the less of child.
            }
            parents = parents.parent;
          }
          //NOTE: if a new child will be created, the key[0] is set with new childs key.
        }
        @SuppressWarnings("unchecked")
        IndexMultiTable<Key, Type> childTable = (IndexMultiTable<Key, Type>)aValues[idx];
        lastObj = childTable.putOrAdd(sortKey, value, valueNext, kind); 
      }
      else {
        //no hyperblock, has leaf data:
        if(idx <0)
        { idx = -idx -1;
          sortin(idx, sortKey, value);  //idx+1 because sortin after found position.            
          check();
        }
        else
        { sortin(idx, sortKey, value);  //idx+1 because sortin after found position.            
          check();
        }
      }
      check();
    }
    else
    { //if key1 is found, sorting after the last value with that index.
      switch(kind){
        case replace: {
          //should replace.
          if(isHyperBlock){
            @SuppressWarnings("unchecked")
            IndexMultiTable<Key, Type> childTable = (IndexMultiTable<Key, Type>)aValues[idx];
            lastObj = childTable.putOrAdd(sortKey, value, valueNext, kind);
          } else {
            lastObj = (Type)aValues[idx];
            aValues[idx] = value;   //replace the existing one.
          }
        } break;
        case addBefore: {
          boolean ok = searchAndSortin(sortKey, value, idx, valueNext);
          if(!ok){
            searchbackAndSortin(sortKey, value, idx, valueNext);
          }
        } break;
        case addLast: {
          assert(valueNext ==null);
          searchLastAndSortin(sortKey, value, idx);
        } break;
        case addOptimized: {
          if(isHyperBlock){
            @SuppressWarnings("unchecked")
            IndexMultiTable<Key, Type> childTable = (IndexMultiTable<Key, Type>)aValues[idx];
            childTable.putOrAdd(sortKey, value, valueNext, kind);
          } else {
            sortin(idx, sortKey, value);
          }
        } break;
      }//switch
    }
    return lastObj;
  }

  

  
  /**Sorts in the given value after all other values with the same key.
   * <ul>
   * <li>If this table has a parent table and the parent table or its parent has a next child with the same key,
   *   then this method is started in the parent. It searches the last key in the parent firstly. Therewith
   *   it is faster to search the end of entries with this key.
   * <li>Elsewhere the last key is searched in this table. Only this table and maybe its children contains the key.
   * <li>If this table is not a hyper block, the value is {@link #sortin(int, Comparable, Object)} after the last
   *   found key.
   * <li>If this table is a hyper block, the last child table with the same key is entered with this method
   *   to continue in the child.     
   * </ul>
   * @param sortkey The key for sort values.
   * @param value
   * @param ixstart The start index where a this key is found.
   * @return
   */
  private boolean searchLastAndSortin(Key sortkey, Type value, int ixstart){
    boolean cont = true;
    int ix = ixstart;
    IndexMultiTable<Key, Type> parent1 = parent, child1 = this;
    while(parent1 !=null){
      if( child1.ixInParent +1 < parent1.sizeBlock 
        && compare(parent1.aKeys[child1.ixInParent+1], sortkey)==0) {
        //the next sibling starts with the same key, look in the parent!
        //Note that it is recursively, starts with the highest parent with that property,
        //walk trough the parent firstly, therefore it is fast.
        return parent1.searchLastAndSortin(sortkey, value, ixInParent+1);
      }
      else if(child1.ixInParent == parent1.sizeBlock){
        //it is possible that the parent's parent have more same keys:
        child1 = parent1; parent1 = parent1.parent;  //may be null, then abort
      } else {
        parent1 = null; //forces finish searching parent.
      }
    }
    while(cont && ix < sizeBlock){
      if((++ix) == sizeBlock             //end of block reached. The sibling does not contain the key because parent is tested.
        || compare(aKeys[ix],sortkey) != 0  //next child has another key 
        ){
        if(isHyperBlock){
          ix-=1;  //it should be stored in the last hyper block, not in the next one. 
          @SuppressWarnings("unchecked")
          IndexMultiTable<Key, Type> childTable = (IndexMultiTable<Key, Type>)aValues[ix];
          childTable.searchLastAndSortin(sortkey, value, 0);
          cont = false;
        } else {
          cont = false;
          sortin(ix, sortkey, value);   //sortin after 
        }
      }
    }
    return !cont;
  }
  
  
  
  
  /**Sorts in the given value before the given element with the same key forward.
   * Starting from the given position all elements where iterate.
   * @param sortkey The key for sort values.
   * @param value
   * @param ixstart The start index where a this key is found.
   * @param valueNext the requested next value.
   * @return
   */
  private boolean searchAndSortin(Key sortkey, Type value, int ixstart, Type valueNext){
    boolean cont = true;
    boolean ok = false;
    int ix = ixstart;
    while(cont && ix < sizeBlock){
      if(isHyperBlock){
        @SuppressWarnings("unchecked")
        IndexMultiTable<Key, Type> childTable = (IndexMultiTable<Key, Type>)aValues[ix];
        ok = childTable.searchAndSortin(sortkey, value, ix, valueNext);
        cont = !ok;
        if(cont){
          ix +=1;  //continue with next.
        }
      } else {
        if(ix < (sizeBlock -1) || aValues[ix+1] == valueNext){
          sortin(ix, sortkey, value);
          ok = true;
          cont = false;
        }
        else if((++ix) < sizeBlock && compare(aKeys[ix],sortkey) != 0){
          cont = false;
        }
      }
    }
    return ok;
  }
  
  
  /**Sorts in the given value before the given element with the same key backward.
   * Starting from the given position all elements where iterate. TODO not ready yet.
   * @param sortkey The key for sort values.
   * @param value
   * @param ixstart The start index where a this key is found.
   * @param valueNext the requested next value.
   * @return
   */
  private boolean searchbackAndSortin(Key sortkey, Type value, int ixstart, Type valueNext){
    return false;
  }
  
  
  
  
  /**inserts the given element into the table at given position.
   * If the table is less, it will be split either in an additional sibling 
   * or, if it is the top level table, into two new tables under the top table.
   * Split is done with {@link #splitIntoSibling(int, Comparable, Object)} or {@link #splitTopLevel(int, Comparable, Object)}.
   * If the table is split, the value is inserted in the correct table.
   * @param ix The index position for the actual table where the value should be sorted in.
   * @param sortkey sorting string to insert.
   * @param value value to insert.
   */
  private void sortin(int ix, Key sortkey, Object value)
  { check();
    if(sizeBlock == maxBlock)
    { //divide the block:
      if(isHyperBlock)
        stop();
      if(parent != null)
      { //it has a hyper block, use it!
        //create a new sibling of this.
        splitIntoSibling(ix, sortkey, value);
        check();
      }
      else
      { //The top level block, it can be splitted only.
        //divide the content of the current block in 2 blocks.
        splitTopLevel(ix, sortkey, value);
        check();
      }
    }
    else
    { //shift all values 1 to right, regard ixInParent if it is a child table.
      if(ix < sizeBlock)
      { //move all following items to right:
        movein(this, this, ix, ix+1, sizeBlock - ix);
      }
      sizeBlock +=1;
      aKeys[ix] = sortkey;
      aValues[ix] = value;
      if(value instanceof IndexMultiTable<?, ?>){
        @SuppressWarnings("unchecked")
        IndexMultiTable<Key,Type> childTable = (IndexMultiTable<Key,Type>)value;
        childTable.ixInParent = ix;
        childTable.parent = this;
      }
    }
    check();
    sizeAll +=1;
  }
  
  
  
  private void splitTopLevel(int idx, Key key1, Object obj1){
    IndexMultiTable<Key, Type> left = new IndexMultiTable<Key, Type>(provider);
    IndexMultiTable<Key, Type> right = new IndexMultiTable<Key, Type>(provider);
    left.parent = right.parent=this;
    left.shouldCheck = right.shouldCheck = shouldCheck;
    left.isHyperBlock = right.isHyperBlock = isHyperBlock;
    left.ixInParent = 0;
    right.ixInParent = 1;
    //the current block is now a hyper block.
    this.isHyperBlock = true;
    int newSize = sizeBlock/2;
    if(idx > newSize){
      left.sizeAll = movein(this, left, 0, 0, newSize);
      left.sizeBlock = newSize;
      right.sizeAll = movein(this, right, newSize, 0, idx - newSize);
      int ix1 = idx - newSize;
      right.aKeys[ix1] = key1;
      right.aValues[ix1] = obj1;
      if(obj1 instanceof IndexMultiTable<?,?>){
        @SuppressWarnings("unchecked")
        IndexMultiTable<Key,Type> childTable = (IndexMultiTable<Key,Type>)obj1;
        childTable.ixInParent = ix1;
        childTable.parent = right;
      }
      right.sizeAll += movein(this, right, idx, ix1+1, sizeBlock - idx);
      right.sizeBlock = sizeBlock - newSize +1;
      aValues[0] = left;
      aValues[1] = right;
      left.check();
      right.check();
    } else {
      if(idx >=0){
        left.sizeAll = movein(this, left, 0, 0, idx);
        left.aKeys[idx] = key1;
        left.aValues[idx] = obj1;
        if(obj1 instanceof IndexMultiTable<?,?>){
          @SuppressWarnings("unchecked")
          IndexMultiTable<Key,Type> childTable = (IndexMultiTable<Key,Type>)obj1;
          childTable.ixInParent = idx;
          childTable.parent = left;
        }
        left.sizeAll += 1;
        left.sizeAll += movein(this, left, idx, idx+1, newSize - idx);
        left.sizeBlock = newSize +1;
      } else {
        left.sizeAll = movein(this, left, 0, 0, newSize);
        left.sizeBlock = newSize;
      }
      right.sizeAll = movein(this, right, newSize, 0, sizeBlock - newSize);
      right.sizeBlock = sizeBlock - newSize;
      aValues[0] = left;
      aValues[1] = right;
      left.check();
      right.check();
    }
    aKeys[0] = left.aKeys[0]; //minKey__;  //because it is possible to sort in lesser keys.
    aKeys[1] = right.aKeys[0];
    sizeBlock = 2;
    clearRestArray(this);
    check();
  }
  
  
  
  /**
   * @param idx if <0 then do not sortin a key, obj1
   * @param key1
   * @param obj1
   */
  private IndexMultiTable<Key, Type> splitIntoSibling(int idx, Key key1, Object obj1){
    IndexMultiTable<Key, Type> sibling = new IndexMultiTable<Key, Type>(provider);
    sibling.parent = parent;
    sibling.shouldCheck = shouldCheck;
    sibling.isHyperBlock = isHyperBlock;
    sibling.ixInParent = this.ixInParent +1;
    //sortin divides the parent in 2 tables if it is full.
    int newSize = sizeBlock/2;
    if(idx > newSize){
      //new element moved into the sibling.
      int ix1 = idx - newSize;
      sibling.sizeAll = movein(this, sibling, newSize, 0, idx - newSize);
      sibling.aKeys[ix1] = key1;
      sibling.aValues[ix1] = obj1;
      if(obj1 instanceof IndexMultiTable<?,?>){
        @SuppressWarnings("unchecked")
        IndexMultiTable<Key,Type> childTable = (IndexMultiTable<Key,Type>)obj1;
        childTable.ixInParent = ix1;
        childTable.parent = sibling;
      }
      sibling.sizeAll +=1;
      sibling.sizeAll += movein(this, sibling, idx, ix1 +1, sizeBlock - idx);
      sibling.sizeBlock = sizeBlock - newSize +1;
      sizeBlock = newSize;
      sizeAll -= sibling.sizeAll;
      //sibling.sizeAll +=1;  //the new element.
      clearRestArray(this);
      parent.sortin(sibling.ixInParent, sibling.aKeys[0], sibling);  //sortin the empty table in parent.      
      this.check();
      sibling.check();
      parent.check();
    } else {
      //new element moved into this.
      sibling.sizeAll = movein(this, sibling, newSize, 0, sizeBlock - newSize);
      sibling.sizeBlock = sizeBlock - newSize; 
      if(idx >=0){
        if(idx < newSize){
          movein(this, this, idx, idx+1, newSize -idx);
        }
        this.aKeys[idx] = key1;
        this.aValues[idx] = obj1;
        if(obj1 instanceof IndexMultiTable<?,?>){
          @SuppressWarnings("unchecked")
          IndexMultiTable<Key,Type> childTable = (IndexMultiTable<Key,Type>)obj1;
          childTable.ixInParent = idx;
          childTable.parent = this;
        }
        sizeBlock = newSize +1;
        sizeAll = sizeAll - sibling.sizeAll +1;
      } else {
        sizeBlock = newSize;
        sizeAll = sizeAll - sibling.sizeAll;
      }
      clearRestArray(this);
      parent.sortin(sibling.ixInParent, sibling.aKeys[0], sibling);  //sortin the empty table in parent.      
      this.check();
      sibling.check();
      parent.check();
    }
    return sibling;
  }
  
  
  
 
  
  /**Moves some elements of the src table in the dst table. Note that this method does not need any information
   * of this. It is a static method. Only because Key and Src should be known - the same like the calling instance -
   * this method is not static.
   * @param src The source table
   * @param dst The destination table
   * @param ixSrc Position in src
   * @param ixDst Position in dst
   * @param nrof number of elements to move from src to dst.
   * @return the number of elements moved inclusively all elements in children, to build {@link #sizeAll}
   */
  private int movein(IndexMultiTable<Key,Type> src, IndexMultiTable<Key,Type> dst, int ixSrc, int ixDst, int nrof){
    int sizeRet = nrof;
    int ix2 = ixDst + nrof - 1;
    for(int ix1 = ixSrc + nrof-1; ix1 >= ixSrc; --ix1){
      Object value = src.aValues[ix1];
      if(value instanceof IndexMultiTable<?,?>) {
        @SuppressWarnings("unchecked")
        IndexMultiTable<Key,Type> childTable = (IndexMultiTable<Key,Type>)value;
        childTable.ixInParent = ix2;
        childTable.parent = dst;
        sizeRet += childTable.sizeAll -1;  //-1: 1 element is counted initial
      } 
      dst.aValues[ix2] = value;
      dst.aKeys[ix2] = src.aKeys[ix1];
      ix2 -=1;
    }
    return sizeRet;
  }
  
  
  /**Cleanup the part if {@link #aKeys} and {@link #aValues} which are not used.
   * Note: {@link #sizeBlock} have to be set correctly.
   * @param dst The table where clean up is done.
   */
  private void clearRestArray(IndexMultiTable<Key,Type> dst){
    Key maxKey = provider.getMaxSortKey();
    for(int ix = dst.sizeBlock; ix < maxBlock; ix++)
    { dst.aKeys[ix] = dst.maxKey__; 
      dst.aValues[ix] = null;
    }
    
  }
  
  
  
  
  /**separates the src into two arrays with the half size and sort in the object.
   * Yet not used.
   * @param idx Primordially index of the obj in the src array. 
   * @param key1 The key value
   * @param obj1 The object
   * @param src The src table
   * @param left The left table. It may be the same as src.
   * @param right The right table.
   */
  private void sortInSeparated2arrays
  ( final int idx, Key key1, Object obj1
  , IndexMultiTable<Key, Type> src 
  , IndexMultiTable<Key, Type> left 
  , IndexMultiTable<Key, Type> right
  )
  {
    left.isHyperBlock = src.isHyperBlock; right.isHyperBlock = src.isHyperBlock;  //copy it. 
            
    final int idxH = maxBlock / 2;
    if(idx < idxH)
    { /**sortin the obj1 in the left table. */
      System.arraycopy(src.aKeys, idxH, right.aKeys, 0, src.sizeBlock - idxH);
      System.arraycopy(src.aValues, idxH, right.aValues, 0, src.sizeBlock - idxH);

      System.arraycopy(src.aKeys, 0, left.aKeys, 0, idx);
      System.arraycopy(src.aValues, 0, left.aValues, 0, idx);
      System.arraycopy(src.aKeys, idx, left.aKeys, idx+1, idxH-idx);
      System.arraycopy(src.aValues, idx, left.aValues, idx+1, idxH-idx);
      left.aKeys[idx] = key1;
      left.aValues[idx] = obj1;

    }  
    else
    { /**sortin the obj1 in the right table. */
      System.arraycopy(src.aKeys, 0, left.aKeys, 0, idxH);
      System.arraycopy(src.aValues, 0, left.aValues, 0, idxH);
      
      int idxR = idx-idxH; //valid for right block.
      System.arraycopy(src.aKeys, idxH, right.aKeys, 0, idxR);
      System.arraycopy(src.aValues, idxH, right.aValues, 0, idxR);
      System.arraycopy(src.aKeys, idx, right.aKeys, idxR+1, src.sizeBlock - idx);
      System.arraycopy(src.aValues, idx, right.aValues, idxR+1, src.sizeBlock - idx);
      right.aKeys[idxR] = key1;
      right.aValues[idxR] = obj1;
    }
    /**Set the sizeBlock and clear the content after copy of all block data,
     * because it is possible that src is equal left or right!
     */
    if(idx < idxH)
    { left.sizeBlock = idxH +1;
      right.sizeBlock = maxBlock - idxH;
    }
    else
    { left.sizeBlock = idxH;
      right.sizeBlock = maxBlock - idxH +1;
    }
    for(int idxFill = left.sizeBlock; idxFill < maxBlock; idxFill++)
    { left.aKeys[idxFill] = maxKey__; 
      left.aValues[idxFill] = null;
    }
    for(int idxFill = right.sizeBlock; idxFill < maxBlock; idxFill++)
    { right.aKeys[idxFill] = maxKey__; 
      right.aValues[idxFill] = null;
    }
    src.check();
    left.check();
    right.check();
  }



  
  /**Deletes the element on ix in the current table.
   * @param ix
   */
  protected void delete(int ix){
    Key keydel = aKeys[ix];
    sizeBlock -=1;
    if(ix < sizeBlock){
      System.arraycopy(aKeys, ix+1, aKeys, ix, sizeBlock-ix);
      System.arraycopy(aValues, ix+1, aValues, ix, sizeBlock-ix);
    }
    aKeys[sizeBlock] = maxKey__;
    aValues[sizeBlock] = null;   //prevent dangling references!
    if(sizeBlock == 0 && parent !=null){
      //this sub-table is empty
      ////
      int ixParent = binarySearchFirstKey(parent.aKeys, 0, parent.sizeBlock, keydel); //, sizeBlock, key1);
      if(ixParent < 0)
      { ixParent = -ixParent-1;  
      }
      parent.delete(ixParent);  //call recursively.
      //it has delete the child table. The table may be referenced by an iterator still.
      //But the iterator won't detect hasNext() and it continoues on its parent iterator too. 
    }
  }
  
  


  /**separates the src into two arrays with the half size .
   * 
   * @param src The src table
   * @param left The left table. It may be the same as src.
   * @param right The right table.
   * @return the first key of the right table.
   */
  private Key separateIn2arrays
  ( IndexMultiTable<Key, Type> src 
  , IndexMultiTable<Key, Type> left 
  , IndexMultiTable<Key, Type> right
  )
  {
    left.isHyperBlock = src.isHyperBlock; right.isHyperBlock = src.isHyperBlock;  //copy it. 
            
    final int idxH = maxBlock / 2;
  
    System.arraycopy(src.aKeys, idxH, right.aKeys, 0, src.sizeBlock - idxH);
    System.arraycopy(src.aValues, idxH, right.aValues, 0, src.sizeBlock - idxH);

    System.arraycopy(src.aKeys, 0, left.aKeys, 0, idxH);
    System.arraycopy(src.aValues, 0, left.aValues, 0, idxH);
    /**Set the sizeBlock and clear the content after copy of all block data,
     * because it is possible that src is equal left or right!
     */
    left.sizeBlock = idxH;
    for(int idxFill = idxH; idxFill < maxBlock; idxFill++)
    { left.aKeys[idxFill] = maxKey__; 
      left.aValues[idxFill] = null;
    }
    right.sizeBlock = maxBlock - idxH;
    for(int idxFill = right.sizeBlock; idxFill < maxBlock; idxFill++)
    { right.aKeys[idxFill] = maxKey__; 
      right.aValues[idxFill] = null;
    }
    src.check();
    left.check();
    right.check();
    return right.aKeys[0];
  }







  /**Delete all content. 
   * @see java.util.Map#clear()
   */
  public void clear()
  {
    for(int ix=0; ix<sizeBlock; ix++){
      if(isHyperBlock){ 
        @SuppressWarnings("unchecked")
        IndexMultiTable subTable = (IndexMultiTable)aValues[ix];
        subTable.clear();
      }
      aValues[ix] = null;
      aKeys[ix] = maxKey__; 
    }
    sizeBlock = 0;
    isHyperBlock = false;
  }







  @SuppressWarnings("unchecked")
  public boolean containsKey(Object key)
  { boolean[] found = new boolean[1];
    return search((Key)key, true, found) !=null || found[0];
  }







  public boolean containsValue(Object arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }







  public Set<java.util.Map.Entry<Key, Type>> entrySet()
  {
    return entrySet;
  }


  @SuppressWarnings({ "unchecked" })
  @Override public Type get(Object keyArg){
    assert(keyArg instanceof Comparable<?>);
    IndexBox ixRet = new IndexBox();
    IndexMultiTable<Key, Type> table = searchInTables((Key)keyArg, true, ixRet);
    if(table !=null){
      return (Type)table.aValues[ixRet.ix];
    } else return null;
  }



  /**Searches the object with exact this key or the object which's key is the nearest lesser one.
   * For example if given is "Bx, By, Bz" as keys and "Bya" is searched, the value with the key "By" is returned.
   * The user should check the returned object whether it is matching to the key or whether it is able to use.
   * The found key is unknown outside of this routine but the key or adequate properties should able to get 
   * from the returned value.
   * @param key
   * @return
   */
  public Type search(Key key){ 
    return search(key, false, null);
  }

  
  
  /**Assures that if val1 is a String, the key is converted toString() before comparison.
   * @param val1
   * @param key
   * @return
   */
  protected int compare(Comparable<Key> val1, Key key){
    int cmp;
    if(val1 instanceof CharSequence){
      //prevent String.compareTo(AnyOtherCharSequence) because only String.compareTo(String) works:
      //but enables comparison of any other key type.
      CharSequence key1 = key instanceof CharSequence ? (CharSequence)key : key.toString();
      cmp = StringFunctions.compare((CharSequence)val1, key1);  
    } else {
      cmp = val1.compareTo(key);  //compare CharSequence, not only Strings
    }
    return cmp;
  }


  /**Searches the key in the tables.
   * @param keyArg The key
   * @param exact if true then returns null and retFound[0] = false if the key was not found
   *   if false then returns the first value at or after the key, see {@link #search(Comparable)}.
   * @param retFound If null then not used. If not null then it must initialized with new boolean[1].
   *   retFound[0] is set to true or false if the key was found or not.
   *   Note: If the key is found and the value for this key is null, retFound[0] is set to true.
   *   Only with this the {@link #containsKey(Object)} works probably. 
   * @return The exact found value or the non exact found value with key before. 
   *   null if the key is lesser than all other keys (it should the first position).
   *   null if the value for this key is null.
   *   null if exact = true and the key is not found.
   */
  public Type search(Key keyArg, boolean exact, boolean[] retFound)
  { 
    IndexBox ixRet = new IndexBox();
    IndexMultiTable<Key, Type> table = searchInTables(keyArg, exact, ixRet);
    if(table !=null){
      if(retFound !=null){ retFound[0] = ixRet.found; }
      @SuppressWarnings("unchecked")
      Type ret = (Type)table.aValues[ixRet.ix];
      return ret;
    } else return null;
  }
  
  
  /**Searches the key in the tables.
   * @param key1 The key
   * @param exact if true then returns null and retFound[0] = false if the key was not found
   *   if false then returns the first value at or after the key, see {@link #search(Comparable)}.
   * @param ixFound should be create newly or initialize. 
   *   If the key was found, the found is set to true. If the key is not found, the found is not
   *   touched. It should be false initially. If the key is found and the value for this key is null, found true.
   *   Only with this the {@link #containsKey(Object)} works probably.
   *   ix is set in any case if this method does not return null. 
   * @return The table where the element is found. 
   *   null if the key is lesser than all other keys (it should the first position).
   *   null if the value for this key is null.
   *   null if exact = true and the key is not found.
   */
  @SuppressWarnings( "unchecked")
  private  IndexMultiTable<Key, Type> searchInTables(Key key1, boolean exact, IndexBox ixFound)
  { IndexMultiTable<Key, Type> table = this;
    //place object with same key after the last object with the same key.
    while(table.isHyperBlock)
    { int idx = binarySearchFirstKey(table.aKeys, 0, table.sizeBlock, key1); //, sizeBlock, key1);
      if(idx < 0)
      { //an non exact found index is possible if it is an Hyper block.
        idx = -idx-2;  //NOTE: access to the lesser element before the insertion point.
      }
      if(idx<0)
      { return null;
      }
      else
      { assert(idx < table.sizeBlock);
        table = ((IndexMultiTable<Key, Type>)(table.aValues[idx]));
      }
    }
    int idx = binarySearchFirstKey(table.aKeys, 0, table.sizeBlock, key1); //, sizeBlock, key1);
    { if(idx < 0){
        if(exact) return null;
        else {
          //ixFound.found remain false
          idx = -idx -2;   //NOTE: access to the lesser element before the insertion point.
        }
      } else {
        ixFound.found = true; 
      }
      if(idx >=0)
      { ixFound.ix = idx;
        return table;
      }
      else  
      { //not found, before first.
        return null;
      }  
    }
  }




  IndexMultiTable<Key, Type> nextSibling(){
    IndexMultiTable<Key, Type> sibling = null;
    if(parent !=null){
      if(ixInParent < sizeBlock-1){
        @SuppressWarnings("unchecked")
        IndexMultiTable<Key, Type> sibling1 = (IndexMultiTable<Key, Type>)parent.aValues[ixInParent+1];
        sibling = sibling1;
      } else {
        Assert.check(false);
      }
    } else {
      Assert.check(false);
    }
    return sibling;
  }



  public boolean isEmpty()
  {
    // TODO Auto-generated method stub
    return false;
  }







  public Set<Key> keySet()
  {
    // TODO Auto-generated method stub
    return null;
  }


















  public int size()
  { return sizeAll;
  }







  @Override public Collection<Type> values()
  {
    //Should return an implementation of Collection which deals with the inner data.
    //see iterator(), entrySet
    // TODO Auto-generated method stub
    return null;
  }











  public Iterator<Type> iterator()
  {
    return new IteratorImpl(this);
  }



  public Iterator<Type> iterator(Key fromKey)
  {
    return new IteratorImpl(this, fromKey);
  }



  /**
   * @param keyArg
   * @return
   */
  @SuppressWarnings({ "unchecked" })
  @Override public Type remove(Object keyArg){
    assert(keyArg instanceof Comparable<?>);
    IndexBox ixRet = new IndexBox();
    IndexMultiTable<Key, Type> table = searchInTables((Key)keyArg, true, ixRet);
    if(table !=null){
      Type ret = (Type)table.aValues[ixRet.ix];
      table.delete(ixRet.ix);
      return ret;    
    } else return null;
  }

  
  void stop()
  { //debug
  }
  
  
  
  @SuppressWarnings("unchecked")
  void check()
  { if(shouldCheck){
      if(parent!=null){
        assert1(parent.aValues[ixInParent] == this);
      }
      if(sizeBlock >=1){ assert1(aValues[0] != null); }
      for(int ii=1; ii < sizeBlock; ii++)
      { assert1(compare(aKeys[ii-1],aKeys[ii]) <= 0);
        assert1(aValues[ii] != null);
        if(aValues[ii] == null)
          stop();
      }
      if(isHyperBlock)
      { for(int ii=0; ii < sizeBlock; ii++)
        { assert1(aValues[ii] instanceof IndexMultiTable<?,?>);
          IndexMultiTable<Key, Type> childtable = (IndexMultiTable<Key, Type>)aValues[ii]; 
          assert1(aKeys[ii].equals(childtable.aKeys[0])); 
          assert1(childtable.ixInParent == ii);
        }
      }
      for(int ii=sizeBlock; ii < maxBlock; ii++)
      { assert1(aKeys[ii] == maxKey__);
        assert1(aValues[ii] == null);
      }
    }  
  }
  
  
  /**Checks the consistency of the table. This method is only proper for test of the algorithm
   * and assurance of correctness. 
   * The check starts with the top table. It iterates over all children in order of the tables and checks:
   * <ul>
   * <li>{@link #ixInParent} and {@link #parent} of a child hyper table.
   * <li>equality of the {@link #aKeys}[ixInParent] in the parent and {@link #aKeys}[0] in the child hyper table.
   * <li>Order of all sort keys in all tables.
   * <li>All not used entries in {@link #aKeys} should have the {@link Provide#getMaxSortKey()}
   *   and all not used entries in {@link #aValues} should have null.
   * </ul> 
   * If any error is found, an RuntimeException is invoked. It is recommended to test in a debugger.
   * If the algorithm in this class is correct, the exception should not be invoked.
   */ 
  public void checkTable(){ checkTable(null, null, -1, provider.getMinSortKey() );}
  
  
  /**Checks the consistency of the table. This method is only proper for test of the algorithm
   * and assurance of correctness.  
   * @param parentP The parent of this table or null for the top table.
   * @param keyParentP The key of this table in the parents entry. null for top table.
   * @param ixInParentP The position of this table in the parent's table. -1 for top table.
   * @param keylastP The last key from the walking through the last child, minimal key for top table.
   * @return The last found key in order of tables.
   */
  private Key checkTable(IndexMultiTable<Key, Type> parentP, Key keyParentP, int ixInParentP, Key keylastP){
    Key keylast = keylastP;
    assert1(parentP == null || keyParentP.equals(aKeys[0]));
    assert1(this.parent == parentP);
    assert1(this.ixInParent == ixInParentP);
    for(int ix = 0; ix < sizeBlock; ++ix){
      assert1(compare(aKeys[ix], keylast) >= 0);
      if(isHyperBlock){
        assert1(aValues[ix] instanceof IndexMultiTable<?,?>);
        @SuppressWarnings("unchecked")
        IndexMultiTable<Key,Type> childTable = (IndexMultiTable<Key,Type>)aValues[ix];
        keylast = childTable.checkTable(this, aKeys[ix], ix, keylast);
      } else {
        assert1(!(aValues[ix] instanceof IndexMultiTable<?,?>));
        keylast = aKeys[ix];
      }
    }
    for(int ix=sizeBlock; ix < maxBlock; ix++)
    { assert1(aKeys[ix] == maxKey__);
      assert1(aValues[ix] == null);
    }
    return keylast;
  }
  
  
  
  
  
  
  
  void assert1(boolean cond)
  {
    if(!cond)
    { stop();
      throw new RuntimeException("IndexMultiTable - is corrupted;");
    }  
  }

  
  
  /**Binaray search of the element, which is the first with the given key.
   * The algorithm is copied from {@link java.util.Arrays}.binarySearch0(Object[], int, int, key) and modified.
   * @param a
   * @param fromIndex
   * @param toIndex
   * @param key
   * @return
   */
  int binarySearchFirstKey(Comparable<Key>[] a, int fromIndex, int toIndex, Key key) 
  {
    int low = fromIndex;
    int high = toIndex - 1;
    int mid =0;
    boolean equal = false;
    while (low <= high) 
    {
      mid = (low + high) >> 1;
      Comparable<Key> midVal = a[mid];
      //Comparable<Key> midValLeft = mid >fromIndex ? a[mid-1] : minKey__;  
      int cmp = compare(midVal, key);
      if ( cmp < 0)
      { low = mid + 1;
        //equal = false;
      }
      else { // if(cmp >=0){
        high = mid - 1;   //search in left part also if key before mid is equal
        equal = equal || cmp ==0;  //one time equal set, it remain set.
      }
      /*
      else
      { { return mid;  //midValLeft is lesser, than it is the first element with key!
        }
      }
      */
    }
    if(equal) return low > mid ? low : mid;  //one time found, then it is low or mid 
    else return -(low + 1);  // key not found.
  }


  @Override
  public void putAll(Map<? extends Key, ? extends Type> m)
  {
    for(Map.Entry<? extends Key, ? extends Type> e: m.entrySet()){
      put(e.getKey(), e.getValue());
    }
  }
  
  
  @Override
  public String toString(){
    StringBuilder u = new StringBuilder();
    if(parent !=null){
      u.append("#").append(parent.identParent);
    }
    if(isHyperBlock){ u.append(':'); } else { u.append('='); }
    toString(u);
    return u.toString();
  }
  
  
  private void toString(StringBuilder u){
    if(sizeBlock ==0){
      u.append("..emptyIndexMultiTable...");
    }
    else if(isHyperBlock){
      for(int ii=0; ii<sizeBlock; ++ii){
        IndexMultiTable<?,?> subTable = (IndexMultiTable<?,?>)aValues[ii];
        subTable.toString(u);
      }
    } else { 
      for(int ii=0; ii<sizeBlock; ++ii){
        u.append(aKeys[ii]).append(", ");
      }
    }
    
  }
  
  
  
  /**This interface is necessary to provide tables and the minimum and maximum value for any user specific type.
   * For the standard type String use {@link IndexMultiTable#providerString}.
   * @param <Key>
   */
  public interface Provide<Key>{
    /**Creates an array of the Key type with given size. */
    Key[] createSortKeyArray(int size);
    
    /**Returns the minimal value of the key. It is a static value. */
    Key getMinSortKey();
    
    /**Returns the maximal value of the key. It is a static value. */
    Key getMaxSortKey();
  }

  /**Provider for String keys. Used for {@link #IndexMultiTable(Provide)}. */
  public static final IndexMultiTable.Provide<String> providerString = new IndexMultiTable.Provide<String>(){

    @Override public String[] createSortKeyArray(int size){ return new String[size]; }

    @Override public String getMaxSortKey(){ 
      return "\uffff\uffff\uffff\uffff\uffff\uffff\uffff\uffff\uffff"; 
    }

    @Override public String getMinSortKey(){ return " "; }
  };
  

  Set<Map.Entry<Key, Type>> entrySet = new Set<java.util.Map.Entry<Key, Type>>()
  {

    @Override
    public boolean add(Map.Entry<Key, Type> e)
    {
      put(e.getKey(), e.getValue());
      return true;
    }

    @Override
    public boolean addAll(Collection<? extends java.util.Map.Entry<Key, Type>> c)
    { for(Map.Entry<Key, Type> e: c){
        put(e.getKey(), e.getValue());
      }
      return true;
    }

    @Override
    public void clear()
    {
      IndexMultiTable.this.clear();
    }

    @Override
    public boolean contains(Object o)
    { return IndexMultiTable.this.containsValue(o);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    { boolean ok = true;
      for(Object obj: c){
        if(!IndexMultiTable.this.containsValue(obj)){
          ok = false;
        }
      }
      return ok;
    }

    @Override
    public boolean isEmpty()
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Iterator<java.util.Map.Entry<Key, Type>> iterator()
    { return IndexMultiTable.this.new EntrySetIterator();
    }

    @Override
    public boolean remove(Object o)
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public int size()
    {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public Object[] toArray()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public <T> T[] toArray(T[] a)
    { 
      // TODO Auto-generated method stub
      return null;
    }
    
  };
  
  
  
  protected class EntrySetIterator implements Iterator<Map.Entry<Key, Type>>
  {

    private final IteratorImpl tableIter = (IteratorImpl)IndexMultiTable.this.iterator();
    
    @Override public boolean hasNext()
    {
      return tableIter.hasNext();
    }

    @Override public Map.Entry<Key, Type> next()
    {
      IteratorHelper<Key, Type> helper = tableIter.helper;
      Type value = tableIter.next();
      Key key = tableIter.getKeyForNext();
      return new Entry(key, value); 
    }

    @Override
    public void remove()
    {
      tableIter.helper.table.delete(tableIter.helper.idx);
      tableIter.helper.idx -=1;  //maybe -1 if first was deleted.
      //IndexMultiTable.IteratorHelper<Key, Type> helperTest = tableIter.helper;
      while(tableIter.helper.parentIter !=null && tableIter.helper.table.sizeBlock ==0){
        tableIter.helper = tableIter.helper.parentIter;
        tableIter.helper.idx -=1;  //on idx it is the next, it has deleted the child table!
      }
    }
    
  };
  
  
  protected class Entry implements Map.Entry<Key, Type>{
    final Type value; final Key key;
    Entry(Key key, Type value){ this.key = key; this.value = value; }
    @Override public Key getKey()
    { return key;
    }
    @Override
    public Type getValue()
    { return value;
    }
    
    @Override
    public Type setValue(Type value)
    { throw new IllegalArgumentException("IndexMultiTable.Entry does not support setValue()");
    }
    
    @Override public String toString(){ return "[ " + key + ", " + value + " ]"; }
  }
  
  
  
  class IndexBox{
    int ix;
    boolean found;
  }
  
  
}
