## Code Review

You are reviewing the following code submitted as part of a task to implement an item cache in a highly concurrent application. The anticipated load includes: thousands of reads per second, hundreds of writes per second, tens of concurrent threads.
Your objective is to identify and explain the issues in the implementation that must be addressed before deploying the code to production. Please provide a clear explanation of each issue and its potential impact on production behaviour.

```java
import java.util.concurrent.ConcurrentHashMap;

// Add Javadoc to the class and public methods.

// In the Javadoc explain the treadoffs of using this class in highly concurrent applications.
// Most importantly, if 2 threads are simultaniously trying to read and write on the same key
// there is no guarantee if the read thread will access the old value or the new.
// In return, the performance is higher than locking the map in write operations for every 
// other operation like it would be if we used Collections.synchronizedMap(new HashMap<>()).
// cache.put("A", "old");
// T1: cache.put("A", "new");
// T2: cache.get("A") might return "old" even though T1 started slightly earlier.
public class SimpleCache<K, V> {
    // Instead of instantiating the ConcurrentHashMap here, instantiate it in:
    // 1. Default constructor.
    // 2. A constructor that accepts int initialCapacity. In case the user has an idea of how large the 
    // cache would become, this can significantly improve the performance by reducing the frequency of 
    // resizing the map. If the requirement arises, constructors with floatingFactor and concurrencyLevel
    // can be added for further optimization and control.
    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    // Having a fixed TTL provides little value. We can call this defaultTtlMs which would serve
    // as the fallback TTL in case an entry doesn't have a TTL of its own.
    private final long ttlMs = 60000; // 1 minute

    // I'm on the fence about this but the current implementation doesn't expose this class
    // externally. I'm not sure if there would be a reason to do so but until such requirement 
    // arises, we should make this class private.
    public static class CacheEntry<V> {
        private final V value;
        // Nit: createTimestamp
        private final long timestamp;
        // Add this field: private Long ttlMs = null;

        // 1. Must check for null value. I suggest using @NotNull so that the user of the method
        // gets compile time warning.
        // 2. createTimestamp must be initiated inside the constructor to avoid exposing
        // unwanted and unpredictable behaviour caused by passing incorrect timestamp.
        // 3. Add a second constructor that accepts and saves a long ttlMs.
        public CacheEntry(V value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }

        public V getValue() {
            return value;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    // 1. This method must check for null key and value.
    // 2. Overload this method to accept a long ttlMs.
    // 3. A very common use case in caches is:
    // Get a value from cache;
    // cache hit --> continue;
    // cache miss --> get value from source;
    //            --> insert value in cache;
    // This can result in multiple writes getting blocked by each other
    // when in fact they want to put the same value in cache.
    // Use putIfAbsent method here so that the bucket doesn't get locked by
    // multiple threads. If the requirement arises, we can add a method called
    // upsert(K key, V value) for the cases where the user intentionally wants to put
    // a new value for that key regardless of it having a value or not.
    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis()));
    }

    // 1. Return Optional<V> instead of null in cache miss cases.
    // 2. This method should also not accept null keys the same way as put method.
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry != null) {
            if (System.currentTimeMillis() - entry.getTimestamp() < ttlMs) {
                // Important: return a copy of the value. if the value itself is returned,
                // it can be changed by another thread which is effectively a write operation 
                // that is not going the thread safe put method.
                return entry.getValue();
            } // If it's expired the entry should be removed from the cache.
              // Otherwise, the cache will keep growing and can cause:
              // 1. Performance degradation due to map resizing.
              // 2. Potential OutOfMemoryError.
              // 3. If combined with suggested use of putIfAbsent method, stale data will remain
              //    in cache indefinitely. 
        }
        return null;
    }
    
    // Add Optional<Long> getTtl(K key) method and calculate the remaining ttl of that key.
    // It's a common feature that caches have and enables user to prepopulate cache when an entry
    // is about to expire in use cases where cache misses are costly.
    
    // Add a remove(K key) method to remove items from cache. It is very likely that this operation would be needed.

    // This method will include the expired (zombie) items that are not deleted yet. There are 2 options depending on 
    // the intended use of the size method.
    // 1. In the following 3 circumstances: 
    //    a. Including zombie items in the size is acceptable. The accuracy is not required.
    //    b. The method is used very frequently. Traversing all entries to exclude zombies from count would cause 
    //        performance issues.
    //    c. It is intended to be used for monitoring the cache's memory consumption. Zombies consume memory too.
    //  It should stay the way it is but the documentation must clarify that zombie entries are included.
    // 2. If accuracy is important and the method is not called frequently.
    //    This method should iterate through entrySet, count the non-expired items, remove the expired ones and 
    //    return the count. Don't worry ConcurrentHashMap allows you to remove items while iterating through
    //    entrySet and even handles changes done by other threads, and it won't throw ConcurrentModificationException.
    public int size() {
        return cache.size();
    }
}
```
