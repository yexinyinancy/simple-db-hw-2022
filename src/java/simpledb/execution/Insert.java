package simpledb.execution;

import java.security.Permission;
import java.security.Permissions;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.storage.BufferPool;
import simpledb.storage.Field;
import simpledb.storage.IntField;
import simpledb.storage.Tuple;
import simpledb.storage.TupleDesc;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;
import simpledb.common.Type;
import java.io.IOException;
import simpledb.common.Utility;
/**
 * Inserts tuples read from the child operator into the tableId specified in the
 * constructor
 */
public class Insert extends Operator {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param t       The transaction running the insert.
     * @param child   The child operator from which to read tuples to be inserted.
     * @param tableId The table in which to insert tuples.
     * @throws DbException if TupleDesc of child differs from table into which we are to
     *                     insert.
     */
    private TransactionId t_;
    private OpIterator child_;
    private int tableId_;

    public Insert(TransactionId t, OpIterator child, int tableId)
            throws DbException {
        // TODO: some code goes here
        this.t_ = t;
        this.child_ = child;
        this.tableId_ = tableId;
    }

    public TupleDesc getTupleDesc() {
        // TODO: some code goes here
        // return child_.getTupleDesc();
        return this.td;
    }

    public void open() throws DbException, TransactionAbortedException {
        // TODO: some code goes here
        super.open();
        child_.open();
    }

    public void close() {
        // TODO: some code goes here
        child_.close();
        super.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // TODO: some code goes here
        close();
        open();
    }

    /**
     * Inserts tuples read from child into the tableId specified by the
     * constructor. It returns a one field tuple containing the number of
     * inserted records. Inserts should be passed through BufferPool. An
     * instances of BufferPool is available via Database.getBufferPool(). Note
     * that insert DOES NOT need check to see if a particular tuple is a
     * duplicate before inserting it.
     *
     * @return A 1-field tuple containing the number of inserted records, or
     *         null if called more than once.
     * @see Database#getBufferPool
     * @see BufferPool#insertTuple
     */
    private TupleDesc td = new TupleDesc(new Type[]{Type.INT_TYPE});
    private boolean f = false;
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // TODO: some code goes here
        int cnt = 0;
        while (child_.hasNext()) {
            Tuple t = child_.next();
            try {
                Database.getBufferPool().insertTuple(t_, tableId_, t);
                cnt++;
            }
            catch (IOException e) {
                throw new DbException("Insert: Error: IOException when Inserting");
            }
        }
        if (!f) {
            Tuple ret = new Tuple(td);
            ret.setField(0, new IntField(cnt));
            f = true;
            return ret;
        }
        return null;
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
