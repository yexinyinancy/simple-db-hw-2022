package simpledb.transaction;
import simpledb.common.Permissions;
import simpledb.storage.PageId;
import simpledb.common.Permissions;
import simpledb.transaction.TransactionId;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LockManager {
    private Map<PageId, List<Lock>> map;

    public LockManager() {
        map = new ConcurrentHashMap<PageId, List<Lock>>();
    }

    public synchronized boolean acquireLock (PageId pageid, Permissions permission, TransactionId transactionid) {
        List<Lock> locklist = map.get(pageid);
        if (locklist == null) {
            map.put(pageid, new ArrayList<Lock>());
            map.get(pageid).add(new Lock(permission, transactionid));
            return true;
        }
        else {
            if (locklist.size() == 1) {
                Lock firstlock = locklist.get(0);
                if (firstlock.getTransactionId().equals(transactionid)) {
                    if (firstlock.getPermission().equals(Permissions.READ_ONLY) && permission.equals(Permissions.READ_WRITE)) {
                        firstlock.setPermission(Permissions.READ_WRITE);
                    }
                    return true;
                }
                else {
                    if (firstlock.getPermission().equals(Permissions.READ_ONLY) && permission.equals(Permissions.READ_ONLY)) {
                        locklist.add(new Lock(permission, transactionid));
                        return true;
                    }
                    return false;
                }
            }
            else {
                if (permission.equals(Permissions.READ_WRITE)) {
                    return false;
                }
                for (Lock lock : locklist) {
                    if (lock.getTransactionId().equals(transactionid)) {
                        return true;
                    }
                }
                locklist.add(new Lock(permission, transactionid));
                return true;
            }
        }
    }

    public synchronized void releaseLock (PageId pageid, TransactionId transactionid) {
        if (isHolding(transactionid, pageid)) {
            List<Lock> locklist = map.get(pageid);
            for (Lock lock : locklist) {
                if (lock.getTransactionId().equals(transactionid)) {
                    locklist.remove(lock);
                    if (locklist.size() == 0) {
                        map.remove(pageid);
                    }
                    return;
                }
            }
        }
    }

    public synchronized void releaseTransLocks(TransactionId tid){
        for (Map.Entry<PageId, List<Lock>> entry : map.entrySet()) {
            List<Lock> locklist = entry.getValue();
            int len = locklist.size();
            for (int i = len - 1; i >= 0; i--) {
                Lock lock = locklist.get(i);
                if (lock.getTransactionId().equals(tid)) {
                    locklist.remove(i);
                    if (locklist.size() == 0) {
                        map.remove(entry.getKey());
                    }
                }
            }
        }
    }

    public synchronized boolean isHolding (TransactionId transactionid, PageId pageid) {
        List<Lock> locklist = map.get(pageid);
        if (locklist == null) {
            return false;
        }
        for (Lock lock : locklist) {
            if (lock.getTransactionId().equals(transactionid)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized String toString() {
        return map.toString();
    }

}
