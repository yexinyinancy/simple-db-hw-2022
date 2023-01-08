package simpledb.optimizer;

import java.util.*;

import simpledb.execution.Predicate;
import simpledb.execution.Predicate.Op;

/**
 * A class to represent a fixed-width histogram over a single integer-based field.
 */
public class IntHistogram {

    /**
     * Create a new IntHistogram.
     * <p>
     * This IntHistogram should maintain a histogram of integer values that it receives.
     * It should split the histogram into "buckets" buckets.
     * <p>
     * The values that are being histogrammed will be provided one-at-a-time through the "addValue()" function.
     * <p>
     * Your implementation should use space and have execution time that are both
     * constant with respect to the number of values being histogrammed.  For example, you shouldn't
     * simply store every value that you see in a sorted list.
     *
     * @param buckets The number of buckets to split the input value into.
     * @param min     The minimum integer value that will ever be passed to this class for histogramming
     * @param max     The maximum integer value that will ever be passed to this class for histogramming
     */
    private int buckets_;
    private int min_;
    private int max_;
    private double interval_;
    private int total_ = 0;
    private HashMap<Integer, Integer> boundary_ = new HashMap<Integer, Integer>();

    public IntHistogram(int buckets, int min, int max) {
        // TODO: some code goes here
        this.buckets_ = buckets;
        this.min_ = min;
        this.max_ = max;
        this.interval_ = ((double) max_ - min_ + 1) / buckets_;
        for (int i = 0; i < buckets_; i++) {
            boundary_.put(i, 0);
        }
    }

    /**
     * Add a value to the set of values that you are keeping a histogram of.
     *
     * @param v Value to add to the histogram
     */
    public void addValue(int v) {
        // TODO: some code goes here
        if (v < min_ || v > max_) {
            return;
        }
        if (v == max_) {
            boundary_.put(buckets_ - 1, boundary_.get(buckets_ - 1) + 1);
            total_++;
            return;
        }
        int slotIdx = (int) Math.ceil((double) (v - min_ + 1) / interval_) - 1; 
        boundary_.put(slotIdx, boundary_.get(slotIdx) + 1);
        total_++;
    }

    /**
     * Estimate the selectivity of a particular predicate and operand on this table.
     * <p>
     * For example, if "op" is "GREATER_THAN" and "v" is 5,
     * return your estimate of the fraction of elements that are greater than 5.
     *
     * @param op Operator
     * @param v  Value
     * @return Predicted selectivity of this particular operator and value
     */
    public double estimateSelectivity(Predicate.Op op, int v) {
        // TODO: some code goes here
        int slotIdx = (int) Math.ceil((double) (v - min_ + 1) / interval_) - 1; 
        if (v == max_) {
            slotIdx = buckets_ - 1;
        }
        if (op.equals(Op.EQUALS)) {
            if (v < min_ || v > max_) {
                return 0.0;
            }
            return boundary_.get(slotIdx) / interval_ / total_;
        }
        else if (op.equals(Op.GREATER_THAN) || op.equals(Op.GREATER_THAN_OR_EQ)) {
            if (v < min_) {
                return 1.0;
            }
            if (v > max_) {
                return 0.0;
            }
            if (v == min_ && op.equals(Op.GREATER_THAN_OR_EQ)) {
                return 1.0;
            }
            double ret = ((slotIdx + 1) * interval_ + min_ - 1 - v) / interval_ * boundary_.get(slotIdx);
            if (op.equals(Op.GREATER_THAN_OR_EQ) && (slotIdx + 1) * interval_ + min_ - 1 == v) {
                int ma = (int)Math.floor((slotIdx + 1) * interval_ + min_ - 1);
                int mi = (int)Math.floor(slotIdx * interval_ + min_ - 1) + 1;
                int cc = ma - mi + 1;
                ret += boundary_.get(slotIdx) / cc;
            }
            for (int i = slotIdx + 1; i < buckets_; i++) {
                ret += boundary_.get(i);
            }
            return ret / total_;
        }
        else if (op.equals(Op.LESS_THAN) || op.equals(Op.LESS_THAN_OR_EQ)) {
            if (v < min_) {
                return 0.0;
            }
            if (v > max_) {
                return 1.0;
            }
            if (v == min_ && op.equals(Op.LESS_THAN)) {
                return 0.0;
            }
            double ret = (v - (slotIdx * interval_ + min_ - 1) ) / interval_ * boundary_.get(slotIdx);
            for (int i = 0; i < slotIdx; i++) {
                ret += boundary_.get(i);
            }
            return ret / total_;
        }
        else if (op.equals(Op.NOT_EQUALS)) {
            if (v < min_ || v > max_) {
                return 1.0;
            }
            return 1.0 - boundary_.get(slotIdx) / interval_ / total_;
        }
        return -1.0;
    }

    /**
     * @return the average selectivity of this histogram.
     *         <p>
     *         This is not an indispensable method to implement the basic
     *         join optimization. It may be needed if you want to
     *         implement a more efficient optimization
     */
    public double avgSelectivity() {
        // TODO: some code goes here
        return 1.0;
    }

    /**
     * @return A string describing this histogram, for debugging purposes
     */
    public String toString() {
        // TODO: some code goes here
        return boundary_.toString();
    }
}
