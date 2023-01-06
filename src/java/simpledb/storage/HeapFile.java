package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.Debug;
import simpledb.common.Permissions;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;
import java.util.NoSuchElementException;

import java.io.*;
import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @author Sam Madden
 * @see HeapPage#HeapPage
 */
public class HeapFile implements DbFile {

    /**
     * Constructs a heap file backed by the specified file.
     *
     * @param f the file that stores the on-disk backing store for this heap
     *          file.
     */
    private File file;
    private TupleDesc tupledesc;
    public HeapFile(File f, TupleDesc td) {
        // TODO: some code goes here
        this.file = f;
        this.tupledesc = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // TODO: some code goes here
        return this.file;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // TODO: some code goes here
        return this.file.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // TODO: some code goes here
        return this.tupledesc;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        // TODO: some code goes here
        // some code goes here
        // int tableid = pid.getTableId();
        // int pgNo = pid.getPageNumber();
        // int pageSize = Database.getBufferPool().getPageSize();
        // byte[] rawPgData = HeapPage.createEmptyPageData();
        // // random access read from disk
        // try {
        //     FileInputStream in = new FileInputStream(file);
        //     in.skip(pgNo * pageSize);
        //     in.read(rawPgData);
        //     return new HeapPage(new HeapPageId(tableid, pgNo), rawPgData);
        // } catch (FileNotFoundException e) {
        //     throw new IllegalArgumentException("HeapFile: readPage: file not found");
        // } catch (IOException e) {
        //     throw new IllegalArgumentException("HeapFile: readPage: file not found");
        // }

        if (getId() == pid.getTableId()) {
    		int pgNo = pid.getPageNumber();
    		
    		if (pgNo>=0 && pgNo<numPages()) {
    			byte[] bytes = HeapPage.createEmptyPageData();
	    			
    			try {
    				RandomAccessFile raf = new RandomAccessFile(file, "r");
    				
    				try {
    					raf.seek(1L*BufferPool.getPageSize()*pid.getPageNumber());
						raf.read(bytes, 0, BufferPool.getPageSize());
						return new HeapPage(new HeapPageId(pid.getTableId(), pgNo), bytes);
    				} finally {
    					raf.close();
    				}
    			} catch (IOException e) {
    				throw new RuntimeException(e);
    			}
    		}
    	}
        throw new IllegalArgumentException("page not in the file");
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // TODO: some code goes here
        // not necessary for lab1
        PageId pid = page.getId();
        int pgNo = pid.getPageNumber();
        int pageSize = Database.getBufferPool().getPageSize();
        byte[] rawPgData = page.getPageData();
        RandomAccessFile dba = new RandomAccessFile(file, "rws");
        dba.skipBytes(pgNo * pageSize);
        dba.write(rawPgData);
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // TODO: some code goes here
        return (int) this.file.length() / Database.getBufferPool().getPageSize();
    }

    // see DbFile.java for javadocs
    public List<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // TODO: some code goes here
        // not necessary for lab1
        List<Page> ret = new ArrayList<Page>();
        for (int i = 0; i < numPages(); i++) {
            HeapPageId pid = new HeapPageId(getId(), i);
            HeapPage p = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
            if (p.getNumUnusedSlots() > 0) {
                p.insertTuple(t);
                ret.add(p);
                return ret;
            }
        }
        HeapPage p = new HeapPage(new HeapPageId(getId(), numPages()), HeapPage.createEmptyPageData());
        p.insertTuple(t);
        writePage(p);
        ret.add(p);
        return ret;
    }

    // see DbFile.java for javadocs
    public List<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // TODO: some code goes here
        // not necessary for lab1
        List<Page> ret = new ArrayList<Page>();
        HeapPageId pid = (HeapPageId) t.getRecordId().getPageId();
        if (pid.getTableId() == getId()) {
            HeapPage p = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
            p.deleteTuple(t);
            ret.add(p);
            return ret;
        }
        throw new DbException("HeapFile: deleteTuple: tuple.tableid != getId");
    }
    private class HeapFileIterator implements DbFileIterator {
        private Integer curPageId;
        private Iterator<Tuple> itr;
        private int numPage = 0;
        private TransactionId tid_;
        private int tableId;
        private HeapFileIterator(TransactionId tid) {
            this.numPage = numPages();
            this.tid_ = tid;
            this.tableId = getId();
            this.curPageId = null;
            this.itr = null;
        }

        @Override
        public void open() throws DbException, TransactionAbortedException {
            this.curPageId = 0;
            PageId pid = new HeapPageId(this.tableId, this.curPageId);
            this.itr = ((HeapPage) Database.getBufferPool().getPage(this.tid_, pid, Permissions.READ_ONLY)).iterator();
        }

        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            if (this.curPageId == null || this.curPageId >= this.numPage) {
                return false;
            }
            while (this.curPageId < this.numPage-1) {
                if (this.itr.hasNext()) {
                    return true;
                }
                else {
                    this.curPageId++;
                    PageId pid = new HeapPageId(this.tableId, this.curPageId);
                    this.itr = ((HeapPage) Database.getBufferPool().getPage(this.tid_, pid, Permissions.READ_ONLY)).iterator();
                }
            } 
            return this.itr.hasNext();
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            if (!hasNext()) {
                throw new NoSuchElementException("no more elements");
            }
            return this.itr.next();
        }

        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            close();
            open();
        }
        
        @Override
        public void close() {
            this.curPageId = null;
            this.itr = null;
        }

    }
    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // TODO: some code goes here
        return new HeapFileIterator(tid);
    }

}

