package simpledb.execution;

import simpledb.common.DbException;
import simpledb.common.Type;
import simpledb.execution.Aggregator.Op;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;

import java.util.NoSuchElementException;

import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;

/**
 * The Aggregation operator that computes an aggregate (e.g., sum, avg, max,
 * min). Note that we only support aggregates over a single column, grouped by a
 * single column.
 */
public class Aggregate extends Operator {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     * <p>
     * Implementation hint: depending on the type of afield, you will want to
     * construct an {@link IntegerAggregator} or {@link StringAggregator} to help
     * you with your implementation of readNext().
     *
     * @param child  The OpIterator that is feeding us tuples.
     * @param afield The column over which we are computing an aggregate.
     * @param gfield The column over which we are grouping the result, or -1 if
     *               there is no grouping
     * @param aop    The aggregation operator to use
     */
    private OpIterator child_;
    private int afield_;
    private int gfield_;
    private Aggregator.Op aop_;

    private OpIterator it_;
    private Aggregator agg;

    public Aggregate(OpIterator child, int afield, int gfield, Aggregator.Op aop) {
        // TODO: some code goes here
        child_ = child;
        afield_ = afield;
        gfield_ = gfield;
        aop_ = aop;
    }

    /**
     * @return If this aggregate is accompanied by a groupby, return the groupby
     *         field index in the <b>INPUT</b> tuples. If not, return
     *         {@link Aggregator#NO_GROUPING}
     */
    public int groupField() {
        // TODO: some code goes here
        return gfield_ == -1 ? Aggregator.NO_GROUPING : gfield_;
    }

    /**
     * @return If this aggregate is accompanied by a group by, return the name
     *         of the groupby field in the <b>OUTPUT</b> tuples. If not, return
     *         null;
     */
    public String groupFieldName() {
        // TODO: some code goes here
        return gfield_ == -1 ? null : child_.getTupleDesc().getFieldName(gfield_);
    }

    /**
     * @return the aggregate field
     */
    public int aggregateField() {
        // TODO: some code goes here
        return afield_;
    }

    /**
     * @return return the name of the aggregate field in the <b>OUTPUT</b>
     *         tuples
     */
    public String aggregateFieldName() {
        // TODO: some code goes here
        return child_.getTupleDesc().getFieldName(afield_);
    }

    /**
     * @return return the aggregate operator
     */
    public Aggregator.Op aggregateOp() {
        // TODO: some code goes here
        return aop_;
    }

    public static String nameOfAggregatorOp(Aggregator.Op aop) {
        return aop.toString();
    }

    public void open() throws NoSuchElementException, DbException,
            TransactionAbortedException {
        // TODO: some code goes here
        super.open();
        child_.open();
        if (gfield_ == -1) {
            if (child_.getTupleDesc().getFieldType(afield_).equals(Type.INT_TYPE)) {
                agg = new IntegerAggregator(-1, null, afield_, aop_);
            }
            else {
                agg = new StringAggregator(-1, null, afield_, aop_);
            }
        }
        else if (child_.getTupleDesc().getFieldType(afield_).equals(Type.INT_TYPE)) {
            agg = new IntegerAggregator(gfield_, child_.getTupleDesc().getFieldType(gfield_), afield_, aop_);
        }
        else if (child_.getTupleDesc().getFieldType(afield_).equals(Type.STRING_TYPE)) {
            agg = new StringAggregator(gfield_, child_.getTupleDesc().getFieldType(gfield_), afield_, aop_);
        }
        while (child_.hasNext()) {
            agg.mergeTupleIntoGroup(child_.next());
        }
        OpIterator itr = agg.iterator();
        itr = agg.iterator();
        itr.open();
        it_ = itr;
    }
    
    /**
     * Returns the next tuple. If there is a group by field, then the first
     * field is the field by which we are grouping, and the second field is the
     * result of computing the aggregate. If there is no group by field, then
     * the result tuple should contain one field representing the result of the
     * aggregate. Should return null if there are no more tuples.
     */
    
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // TODO: some code goes here
        if (it_.hasNext()) {
            return it_.next();
        }
        return null;
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // TODO: some code goes here
        close();
        open();
    }

    /**
     * Returns the TupleDesc of this Aggregate. If there is no group by field,
     * this will have one field - the aggregate column. If there is a group by
     * field, the first field will be the group by field, and the second will be
     * the aggregate value column.
     * <p>
     * The name of an aggregate column should be informative. For example:
     * "aggName(aop) (child_td.getFieldName(afield))" where aop and afield are
     * given in the constructor, and child_td is the TupleDesc of the child
     * iterator.
     */
    public TupleDesc getTupleDesc() {
        // TODO: some code goes here
        if (gfield_ == -1) {
            return child_.getTupleDesc();
        }
        return new TupleDesc(new Type[]{child_.getTupleDesc().getFieldType(gfield_), Type.INT_TYPE}, 
            new String[]{child_.getTupleDesc().getFieldName(gfield_), child_.getTupleDesc().getFieldName(afield_)});
    }

    public void close() {
        // TODO: some code goes here
        it_.close();
        child_.close();
        super.close();
    }

    @Override
    public OpIterator[] getChildren() {
        // TODO: some code goes here
        return new OpIterator[]{child_};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // TODO: some code goes here
        child_ = children[0];
    }

}
