package simpledb.storage;

import simpledb.common.Database;
import simpledb.common.DbException;
import simpledb.common.DeadlockException;
import simpledb.common.Permissions;
import simpledb.transaction.TransactionAbortedException;
import simpledb.transaction.TransactionId;
import simpledb.transaction.Lock;
import simpledb.transaction.LockManager;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javafx.scene.chart.PieChart.Data;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 *
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /**
     * Bytes per page, including header.
     */
    private static final int DEFAULT_PAGE_SIZE = 4096;

    private static int pageSize = DEFAULT_PAGE_SIZE;

    /**
     * Default number of pages passed to the constructor. This is used by
     * other classes. BufferPool should use the numPages argument to the
     * constructor instead.
     */
    public static final int DEFAULT_PAGES = 50;

    private ConcurrentHashMap<PageId, Page> bp;
    private final int maxNumPages;
    private LockManager lockmanager_;

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // TODO: some code goes here
        this.bp = new ConcurrentHashMap<PageId, Page>(numPages);
        this.maxNumPages = numPages;
        this.lockmanager_ = new LockManager();
    }

    public static int getPageSize() {
        return pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
        BufferPool.pageSize = pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
        BufferPool.pageSize = DEFAULT_PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid  the ID of the transaction requesting the page
     * @param pid  the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public Page getPage(TransactionId tid, PageId pid, Permissions perm)
            throws TransactionAbortedException, DbException {
        // TODO: some code goes here
        boolean ret = false;
        long starttime = System.currentTimeMillis();
        long maxduration = new Random().nextInt(2000);
        while (!ret) {
            long curtime = System.currentTimeMillis();
            if (curtime - starttime > maxduration) {
                throw new TransactionAbortedException();
            }
            ret = lockmanager_.acquireLock(pid, perm, tid);
        }

        if (bp.get(pid) == null) {
            DbFile databaseFile = Database.getCatalog().getDatabaseFile(pid.getTableId());
            Page page = databaseFile.readPage(pid);
            if (bp.size() >= this.maxNumPages) {
                evictPage();
            }
            if (!q.contains(pid)) {
                bp.put(pid, page);
                q.offer(pid);
            }
        }
        return bp.get(pid);
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public void unsafeReleasePage(TransactionId tid, PageId pid) {
        // TODO: some code goes here
        // not necessary for lab1|lab2
        lockmanager_.releaseLock(pid, tid);
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) {
        // TODO: some code goes here
        // not necessary for lab1|lab2
        transactionComplete(tid, true);
    }

    /**
     * Return true if the specified transaction has a lock on the specified page
     */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // TODO: some code goes here
        // not necessary for lab1|lab2
        return lockmanager_.isHolding(tid, p);
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid    the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit) {
        // TODO: some code goes here
        // not necessary for lab1|lab2
        if (commit) {
            try {
                flushPages(tid);
            }
            catch (IOException e) {
                e.getMessage();
            }
        }
        else {
            Iterator<PageId> itrq = q.iterator();
            while (itrq.hasNext()) {
                PageId pid = itrq.next();
                TransactionId tid2 = ((HeapPage) bp.get(pid)).isDirty();
                if (tid2 != null && tid2.equals(tid)) {
                    removePage(pid);
                    try {
                        Page page = Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY);
                        page.markDirty(false,null);
                    }
                    catch (TransactionAbortedException | DbException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        lockmanager_.releaseTransLocks(tid);
    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other
     * pages that are updated (Lock acquisition is not needed for lab2).
     * May block if the lock(s) cannot be acquired.
     * <p>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid     the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t       the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // TODO: some code goes here
        // not necessary for lab1
        List<Page> pages = Database.getCatalog().getDatabaseFile(tableId).insertTuple(tid, t);
        for (Page p : pages) {
            p.markDirty(true, tid);
            if (!q.contains(p.getId())) {
                bp.put(p.getId(), p);
                q.offer(p.getId());
            }
        }
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     * <p>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid the transaction deleting the tuple.
     * @param t   the tuple to delete
     */
    public void deleteTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // TODO: some code goes here
        // not necessary for lab1
        List<Page> pages = Database.getCatalog().getDatabaseFile(t.getRecordId().getPageId().getTableId()).deleteTuple(tid, t);
        for (Page p : pages) {
            p.markDirty(false, tid);
            bp.put(p.getId(), p);
        }
    }

    private ConcurrentLinkedQueue<PageId> q = new ConcurrentLinkedQueue<PageId>();

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     * break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // TODO: some code goes here
        // not necessary for lab1
        for (Map.Entry<PageId, Page> i : bp.entrySet()) {
            flushPage(i.getKey());
        }
    }

    /**
     * Remove the specific page id from the buffer pool.
     * Needed by the recovery manager to ensure that the
     * buffer pool doesn't keep a rolled back page in its
     * cache.
     * <p>
     * Also used by B+ tree files to ensure that deleted pages
     * are removed from the cache so they can be reused safely
     */
    public synchronized void removePage(PageId pid) {
        // TODO: some code goes here
        // not necessary for lab1
        Iterator<PageId> itr_ = q.iterator();
        while (itr_.hasNext()) {
            if (itr_.next().equals(pid)) {
                q.remove();
                bp.remove(pid);
                break;
            }
        }
    }

    /**
     * Flushes a certain page to disk
     *
     * @param pid an ID indicating the page to flush
     */
    private synchronized void flushPage(PageId pid) throws IOException {
        // TODO: some code goes here
        // not necessary for lab1
        ((HeapFile)Database.getCatalog().getDatabaseFile(pid.getTableId())).writePage(bp.get(pid));
    }

    /**
     * Write all pages of the specified transaction to disk.
     */
    public synchronized void flushPages(TransactionId tid) throws IOException {
        // TODO: some code goes here
        // not necessary for lab1|lab2
        Iterator<PageId> itrq = q.iterator();
        while (itrq.hasNext()) {
            PageId pid = itrq.next();
            TransactionId tidDirty = ((HeapPage) bp.get(pid)).isDirty();
            if (tidDirty != null && tidDirty.equals(tid)) {
                flushPage(pid);
            }
        }
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized void evictPage() throws DbException {
        // TODO: some code goes here
        // not necessary for lab1
        Iterator<PageId> itr = q.iterator();
        while (itr.hasNext()) {
            PageId pid_evi = itr.next();
            HeapPage hp_evi = (HeapPage) bp.get(pid_evi);
            TransactionId tid_evi = hp_evi.isDirty();
            if (tid_evi != null) {
                continue;
            }
            bp.remove(pid_evi);
            q.remove(pid_evi);
            return;
        }
        throw new DbException("buffer pool is empty, cannot evict page");
    }

    public LockManager getLockManager() {
        return this.lockmanager_;
    }
}
