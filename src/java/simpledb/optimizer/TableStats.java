package simpledb.optimizer;

import simpledb.common.Database;
import simpledb.common.Type;
import simpledb.execution.Predicate;
import simpledb.execution.SeqScan;
import simpledb.storage.*;
import simpledb.transaction.Transaction;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;
import simpledb.storage.HeapFile;
import simpledb.common.DbException;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.*;

/**
 * TableStats represents statistics (e.g., histograms) about base tables in a
 * query.
 * <p>
 * This class is not needed in implementing lab1 and lab2.
 */
public class TableStats {

    private static final ConcurrentMap<String, TableStats> statsMap = new ConcurrentHashMap<>();

    static final int IOCOSTPERPAGE = 1000;

    public static TableStats getTableStats(String tablename) {
        return statsMap.get(tablename);
    }

    public static void setTableStats(String tablename, TableStats stats) {
        statsMap.put(tablename, stats);
    }

    public static void setStatsMap(Map<String, TableStats> s) {
        try {
            java.lang.reflect.Field statsMapF = TableStats.class.getDeclaredField("statsMap");
            statsMapF.setAccessible(true);
            statsMapF.set(null, s);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException | SecurityException e) {
            e.printStackTrace();
        }

    }

    public static Map<String, TableStats> getStatsMap() {
        return statsMap;
    }

    public static void computeStatistics() {
        Iterator<Integer> tableIt = Database.getCatalog().tableIdIterator();

        System.out.println("Computing table stats.");
        while (tableIt.hasNext()) {
            int tableid = tableIt.next();
            TableStats s = new TableStats(tableid, IOCOSTPERPAGE);
            setTableStats(Database.getCatalog().getTableName(tableid), s);
        }
        System.out.println("Done.");
    }

    /**
     * Number of bins for the histogram. Feel free to increase this value over
     * 100, though our tests assume that you have at least 100 bins in your
     * histograms.
     */
    static final int NUM_HIST_BINS = 100;

    /**
     * Create a new TableStats object, that keeps track of statistics on each
     * column of a table
     *
     * @param tableid       The table over which to compute statistics
     * @param ioCostPerPage The cost per page of IO. This doesn't differentiate between
     *                      sequential-scan IO and disk seeks.
     */
    private int tableid_;
    private int ioCostPerPage_;
    private HeapFile heapfile_;
    private int numTuples_;
    private IntHistogram[] hist_;

    public TableStats(int tableid, int ioCostPerPage) {
        // For this function, you'll have to get the
        // DbFile for the table in question,
        // then scan through its tuples and calculate
        // the values that you need.
        // You should try to do this reasonably efficiently, but you don't
        // necessarily have to (for example) do everything
        // in a single scan of the table.
        // TODO: some code goes here
        this.tableid_ = tableid;
        this.ioCostPerPage_ = ioCostPerPage;
        this.heapfile_ = ((HeapFile) Database.getCatalog().getDatabaseFile(tableid));
        int numfields = heapfile_.getTupleDesc().numFields();
        DbFileIterator itr = heapfile_.iterator(new TransactionId());
        int[] max = new int[numfields];
        Arrays.fill(max, Integer.MIN_VALUE);
        int[] min = new int[numfields];
        Arrays.fill(min, Integer.MAX_VALUE);
        int cnt = 0;
        try {
            itr.open();
            while (itr.hasNext()) {
                cnt++;
                Tuple tup = itr.next();
                for (int i = 0; i < numfields; i++) {
                    if (tup.getField(i).getType().equals(Type.INT_TYPE)) {
                        int val = ((IntField)tup.getField(i)).getValue();
                        max[i] = Math.max(max[i], val);
                        min[i] = Math.min(min[i], val);
                    }
                }
            }
            itr.rewind();
        }
        catch (DbException|TransactionAbortedException e) {
            itr.close();
            e.printStackTrace();
        }
        this.numTuples_ = cnt;
        this.hist_ = new IntHistogram[numfields];
        try {
            while (itr.hasNext()) {
                Tuple tup = itr.next();
                for (int i = 0; i < numfields; i++) {
                    if (tup.getField(i).getType().equals(Type.INT_TYPE)) {
                        hist_[i] = new IntHistogram(10, min[i], max[i]);
                    }
                }
            }
            itr.rewind();
        }
        catch (DbException|TransactionAbortedException e) {
            itr.close();
            e.printStackTrace();
        }
        try {
            while (itr.hasNext()) {
                Tuple tup = itr.next();
                for (int i = 0; i < numfields; i++) {
                    if (tup.getField(i).getType().equals(Type.INT_TYPE)) {
                        int val = ((IntField)tup.getField(i)).getValue();
                        hist_[i].addValue(val);
                    }
                    
                }
            }
        }
        catch (DbException|TransactionAbortedException e) {
            itr.close();
            e.printStackTrace();
        }
        itr.close();
    }

    /**
     * Estimates the cost of sequentially scanning the file, given that the cost
     * to read a page is costPerPageIO. You can assume that there are no seeks
     * and that no pages are in the buffer pool.
     * <p>
     * Also, assume that your hard drive can only read entire pages at once, so
     * if the last page of the table only has one tuple on it, it's just as
     * expensive to read as a full page. (Most real hard drives can't
     * efficiently address regions smaller than a page at a time.)
     *
     * @return The estimated cost of scanning the table.
     */
    public double estimateScanCost() {
        // TODO: some code goes here
        return (double) this.ioCostPerPage_  *  heapfile_.numPages();
    }

    /**
     * This method returns the number of tuples in the relation, given that a
     * predicate with selectivity selectivityFactor is applied.
     *
     * @param selectivityFactor The selectivity of any predicates over the table
     * @return The estimated cardinality of the scan with the specified
     *         selectivityFactor
     */
    public int estimateTableCardinality(double selectivityFactor) {
        // TODO: some code goes here
        return (int) (totalTuples() * selectivityFactor);
    }

    /**
     * The average selectivity of the field under op.
     *
     * @param field the index of the field
     * @param op    the operator in the predicate
     *              The semantic of the method is that, given the table, and then given a
     *              tuple, of which we do not know the value of the field, return the
     *              expected selectivity. You may estimate this value from the histograms.
     */
    public double avgSelectivity(int field, Predicate.Op op) {
        // TODO: some code goes here
        return 1.0;
    }

    /**
     * Estimate the selectivity of predicate <tt>field op constant</tt> on the
     * table.
     *
     * @param field    The field over which the predicate ranges
     * @param op       The logical operation in the predicate
     * @param constant The value against which the field is compared
     * @return The estimated selectivity (fraction of tuples that satisfy) the
     *         predicate
     */
    public double estimateSelectivity(int field, Predicate.Op op, Field constant) {
        // TODO: some code goes here
        if (constant.getType().equals(Type.INT_TYPE)) {
            return hist_[field].estimateSelectivity(op, ((IntField) constant).getValue());
        }
        else if (constant.getType().equals(Type.STRING_TYPE)) {
            StringHistogram stringhist = new StringHistogram(100);
            return stringhist.estimateSelectivity(op, ((StringField) constant).getValue());
        }
        return -1.0;
    }

    /**
     * return the total number of tuples in this table
     */
    public int totalTuples() {
        // TODO: some code goes here
        return this.numTuples_;
    }

}
