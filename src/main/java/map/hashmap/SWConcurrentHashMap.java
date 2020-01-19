package map.hashmap;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap.CounterCell;
import java.util.concurrent.ConcurrentHashMap.EntrySetView;
import java.util.concurrent.ConcurrentHashMap.ForwardingNode;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.ConcurrentHashMap.Node;
import java.util.concurrent.ConcurrentHashMap.ValuesView;

public class SWConcurrentHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Serializable {

	private static final int MAXIMUM_CAPACITY = 1 << 30;// table最大
	private static final int DEFAULT_CAPACITY = 16;// 默认table大小
	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;// 数组的最大可能值，需要与toArray()相关方法关联
	private static final int DEFAULT_CONCURRENCY_LEVEL = 16;// 默认的并发级别，为了兼容以前版本遗留下来的
	private static final float LOAD_FACTOR = 0.75f;//负载因子
	static final int TREEIFY_THRESHOLD = 8;// 链表转化tree阈值，因为泊松分布，单个hash槽个数为8的概率小于百万分之1
	static final int UNTREEIFY_THRESHOLD = 6;// tree转化链表阈值
	static final int MIN_TREEIFY_CAPACITY = 64;// 最小转化为tree节点阈值
	private static final int MIN_TRANSFER_STRIDE = 16;// 每个转换的最小步数
	private static int RESIZE_STAMP_BITS = 16;// 用于在sizeCtl中生成分段的位数，32bit的数组至少为6
	private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;// help resize的最大线程数
	private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;// sizeCtl中记录size的偏移量，默认为32-16=16
	static final int MOVED = -1;// forwarding nodes的hash值 用于存储nextTable的引用，当table扩容的时候才会发生作用，作为一个占位符放在table中表示当前节点为null或则已经被移动。
	static final int TREEBIN = -2;// 红黑树的根节点hash值
	static final int RESERVED = -3;// reservation node的hash值
	static final int HASH_BITS = 0x7fffffff; // 普通节点的散列位

	static final int NCPU = Runtime.getRuntime().availableProcessors();// 可使用的CPU的数量

	transient volatile Node<K, V>[] table;// 存放节点的数组

	private transient volatile long baseCount;// 在没有竞争使用，通过CAS操作更新
	/**
	 * 控制标识符，用来控制table的初始化和扩容的操作，不同的值有不同的含义 当为负数时：
	 * -1代表正在初始化，
	 * -N代表有N-1个线程正在 进行扩容
	 * 当为0时，代表当时的table还没有被初始化 
	 * 当为正数时：表示初始化或下一次进行扩容的大小
	 */
	private transient volatile int sizeCtl;

	private transient volatile Node<K, V>[] nextTable; //默认为null，扩容时新生成的数组，其大小为原数组的两倍。

	private transient volatile int transferIndex;// 备用数据

	/**
	 * Spinlock (locked via CAS) used when resizing and/or creating CounterCells.
	 */
	private transient volatile int cellsBusy;

	/**
	 * Table of counter cells. When non-null, size is a power of 2.
	 */
	private transient volatile CounterCell[] counterCells;

	// views
	private transient KeySetView<K, V> keySet;
	private transient ValuesView<K, V> values;
	private transient EntrySetView<K, V> entrySet;

	// 计算hash
	static final int spread(int h) {
		return (h ^ (h >>> 16)) & HASH_BITS;
	}

	/**
	 * Returns a power of two table size for the given desired capacity. See Hackers
	 * Delight, sec 3.2
	 * 数组扩容
	 */
	private static final int tableSizeFor(int c) {
		int n = c - 1;
		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;
		return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
	}

	/**
	 * Returns x's Class if it is of the form "class C implements Comparable<C>",
	 * else null.
	 * 
	 */
	static Class<?> comparableClassFor(Object x) {
		if (x instanceof Comparable) {
			Class<?> c;
			Type[] ts, as;
			Type t;
			ParameterizedType p;
			if ((c = x.getClass()) == String.class) // bypass checks
				return c;
			if ((ts = c.getGenericInterfaces()) != null) {
				for (int i = 0; i < ts.length; ++i) {
					if (((t = ts[i]) instanceof ParameterizedType)
							&& ((p = (ParameterizedType) t).getRawType() == Comparable.class)
							&& (as = p.getActualTypeArguments()) != null && as.length == 1 && as[0] == c) // type arg is
																											// c
						return c;
				}
			}
		}
		return null;
	}

	/**
	 * Returns k.compareTo(x) if x matches kc (k's screened comparable class), else
	 * 0.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" }) // for cast to Comparable
	static int compareComparables(Class<?> kc, Object k, Object x) {
		return (x == null || x.getClass() != kc ? 0 : ((Comparable) k).compareTo(x));
	}

	static class Node<K, V> implements Map.Entry<K, V> {
		final int hash;
		final K key;
		volatile V val;
		volatile Node<K, V> next;

		Node(int hash, K key, V val, Node<K, V> next) {
			this.hash = hash;
			this.key = key;
			this.val = val;
			this.next = next;
		}

		public final K getKey() {
			return key;
		}

		public final V getValue() {
			return val;
		}

		public final int hashCode() {
			return key.hashCode() ^ val.hashCode();
		}

		public final String toString() {
			return key + "=" + val;
		}

		public final V setValue(V value) {
			throw new UnsupportedOperationException();
		}

		public final boolean equals(Object o) {
			Object k, v, u;
			Map.Entry<?, ?> e;
			return ((o instanceof Map.Entry) && (k = (e = (Map.Entry<?, ?>) o).getKey()) != null
					&& (v = e.getValue()) != null && (k == key || k.equals(key)) && (v == (u = val) || v.equals(u)));
		}

		/**
		 * Virtualized support for map.get(); overridden in subclasses.
		 */
		Node<K, V> find(int h, Object k) {
			Node<K, V> e = this;
			if (k != null) {
				do {
					K ek;
					if (e.hash == h && ((ek = e.key) == k || (ek != null && k.equals(ek))))
						return e;
				} while ((e = e.next) != null);
			}
			return null;
		}
	}
	/**
     * A node inserted at head of bins during transfer operations.
     */
    static final class ForwardingNode<K,V> extends Node<K,V> {
        final Node<K,V>[] nextTable;
        ForwardingNode(Node<K,V>[] tab) {
            super(MOVED, null, null, null);
            this.nextTable = tab;
        }

        Node<K,V> find(int h, Object k) {
            // loop to avoid arbitrarily deep recursion on forwarding nodes
            outer: for (Node<K,V>[] tab = nextTable;;) {
                Node<K,V> e; int n;
                if (k == null || tab == null || (n = tab.length) == 0 ||
                    (e = tabAt(tab, (n - 1) & h)) == null)
                    return null;
                for (;;) {
                    int eh; K ek;
                    if ((eh = e.hash) == h &&
                        ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                    if (eh < 0) {
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode<K,V>)e).nextTable;
                            continue outer;
                        }
                        else
                            return e.find(h, k);
                    }
                    if ((e = e.next) == null)
                        return null;
                }
            }
        }
    }
	static final class TreeNode<K, V> extends Node<K, V> {// 红黑树
		TreeNode<K, V> parent;
		TreeNode<K, V> left;
		TreeNode<K, V> right;
		TreeNode<K, V> prev;
		boolean red;

		TreeNode(int hash, K key, V val, Node<K, V> next, TreeNode<K, V> parent) {
			super(hash, key, val, next);
			this.parent = parent;
		}

		Node<K, V> find(int h, Object k) {
			return findTreeNode(h, k, null);
		}

		final TreeNode<K, V> findTreeNode(int h, Object k, Class<?> kc) {
			if (k != null) {
				TreeNode<K, V> p = this;
				do {
					int ph, dir;
					K pk;
					TreeNode<K, V> q;
					TreeNode<K, V> pl = p.left, pr = p.right;
					if ((ph = p.hash) > h) // hash大于左树
						p = pl;
					else if ((ph = p.hash) < h)// hash小于左树
						p = pr;
					else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
						return p;
					else if (pl == null)
						p = pr;
					else if (pr == null)
						p = pl;
					else if ((kc != null || (kc = comparableClassFor(k)) != null)
							&& (dir = compareComparables(kc, k, pk)) != 0)
						return q;
					else
						p = pl;
				} while (p != null);
			}
		}

	}

	@Override
	public V put(K key, V value) {
		return putVal(key, value, false);
	}

	final V putVal(K key, V value, boolean onlyIfAbsent) {
		// Concurrent不允许有null的key和value
		if (key == null || value == null)
			throw new NullPointerException();
		// 通过spread()函数，将key的hashCode值进行位运算，使得高位与低位都参与运算
		// 降低hash碰撞的概率
		int hash = spread(key.hashCode());
		int binCount = 0;
		for (Node<K, V>[] tab = table;;) {// 自旋
			Node<K, V> f;
			int n, i, fh;
			if (tab == null || (tab.length) == 0)
				tab = iniTable();// 初始化数组
			else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) { // 若散列位置为null，不加锁，直接通过CAS创建节点
				if (casTabAt(tab, i, null, new Node<K, V>(hash, key, value, null)))
					break;
			}
			// 如果发现该桶中有一个节点（forwarding nodes），则帮助扩容
			else if ((fh = f.hash) == MOVED)
				tab = helpTransfer(tab, f);
			else {
				V oldVal = null;
				// 如果上述条件均不满足，则要进行加锁操作，存在hash冲突，需要锁住链表或红黑树节点
				synchronized (f) {
					// 如果该节点为链表节点
					if (tabAt(tab, i) == f) {
						if (fh >= 0) {
							binCount = 1;
							for (Node<K, V> e = f;; ++binCount) {
								K ek;
								// 这里涉及到相同的key进行put操作时会覆盖原来的value
								if (e.hash == hash && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
									oldVal = e.val;
									if (!onlyIfAbsent)
										e.val = value;
									break;
								}
								Node<K, V> pred = e;
								if ((e = e.next) == null) {
									// 在链表尾部插入节点
									pred.next = new Node<K, V>(hash, key, value, null);
									break;
								}
							}
						}
						// 该节点为红黑树节点
						else if (f instanceof TreeBin) {
							Node<K, V> p;
							binCount = 2;
							// 将节点插入红黑树，涉及红黑树节点的旋转操作
							if ((p = ((TreeBin<K, V>) f).putTreeVal(hash, key, value)) != null) {
								oldVal = p.val;
								if (!onlyIfAbsent)
									p.val = value;
							}
						}
					}
				}
				// 如果链表的长度大于8，则就转换为红黑树
				if (binCount != 0) {
					if (binCount >= TREEIFY_THRESHOLD)
						treeifyBin(tab, i);
					if (oldVal != null)
						return oldVal;
					break;
				}
			}
		}
		// 统计size，并坚持是否需要扩容
		addCount(1L, binCount);
		return null;
	}

	// table初始化
	private final Node<K, V>[] iniTable() {
		Node<K, V>[] tab;
		int sc;
		while ((tab = table) == null || tab.length == 0) {
			if ((sc = sizeCtl) < 0)// 当sizeCtl<0，说明有其他线程正在初始化table，线程让出CPU，调用yield
				Thread.yield();
			// 尝试使用CAS操作将sizeCtl修改为-1，开始初始化table
			else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
				try {
					if ((tab = table) == null || tab.length == 0) {
						int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
						@SuppressWarnings("unchecked")
						Node<K, V>[] nt = (Node<K, V>[]) new Node<?, ?>[n];
						table = tab = nt;
						sc = n - (n >>> 2);// 记录扩容的大小
					}
				} finally {
					sizeCtl = sc;
				}
				break;
			}
		}
		return tab;
	}

	@SuppressWarnings("unchecked")
	static final <K, V> Node<K, V> tabAt(Node<K, V>[] tab, int i) {
		return (Node<K, V>) U.getObjectVolatile(tab, ((long) i << ASHIFT) + ABASE);
	}

	static final <K, V> boolean casTabAt(Node<K, V>[] tab, int i, Node<K, V> c, Node<K, V> v) {
		return U.compareAndSwapObject(tab, ((long) i << ASHIFT) + ABASE, c, v);
	}

	static final <K, V> void setTabAt(Node<K, V>[] tab, int i, Node<K, V> v) {
		U.putObjectVolatile(tab, ((long) i << ASHIFT) + ABASE, v);
	}

	/**
	 * Helps transfer if a resize is in progress.
	 */
	final Node<K, V>[] helpTransfer(Node<K, V>[] tab, Node<K, V> f) {
		Node<K, V>[] nextTab;
		int sc;
		if (tab != null && (f instanceof ForwardingNode) && (nextTab = ((ForwardingNode<K, V>) f).nextTable) != null) {
			int rs = resizeStamp(tab.length);
			while (nextTab == nextTable && table == tab && (sc = sizeCtl) < 0) {
				if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 || sc == rs + MAX_RESIZERS || transferIndex <= 0)
					break;
				if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
					transfer(tab, nextTab);
					break;
				}
			}
			return nextTab;
		}
		return table;
	}

	private final void transfer(Node<K, V>[] tab, Node<K, V>[] nextTab) {
		int n = tab.length, stride;
		// 若每个核处理的数量小于16，则强制赋值为16
		if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
			stride = MIN_TRANSFER_STRIDE;
		// 备用table是否没有被初始化
		if (nextTab == null) {
			try {
				@SuppressWarnings("unchecked")
				// 扩容为原来的2倍
				Node<K, V>[] nt = (Node<K, V>[]) new Node<?, ?>[n << 1];
				nextTab = nt;
			} catch (Throwable ex) { // try to cope with OOME
				sizeCtl = Integer.MAX_VALUE;
				return;
			}
			nextTable = nextTab;
			transferIndex = n;
		}
		int nextn = nextTab.length;
		// 连接点指针，用于标志位（fwd的hash值为-1，fwd.nextTable=nextTab)
		ForwardingNode<K, V> fwd = new ForwardingNode<K, V>(nextTab);
		// advance作为Hash桶操作完成的标志变量
		boolean advance = true;
		// finishing作为扩充完成的标志变量
		boolean finishing = false; // to ensure sweep before committing nextTab
		for (int i = 0, bound = 0;;) {
			Node<K, V> f;
			int fh;
			// 控制--i，用于遍历原hash表中的节点
			while (advance) {
				int nextIndex, nextBound;
				if (--i >= bound || finishing)
					advance = false;
				else if ((nextIndex = transferIndex) <= 0) {
					i = -1;
					advance = false;
				}
				// 用CAS操作计算得到transferIndex
				else if (U.compareAndSwapInt(this, TRANSFERINDEX, nextIndex,
						nextBound = (nextIndex > stride ? nextIndex - stride : 0))) {
					bound = nextBound;
					i = nextIndex - 1;
					advance = false;
				}
			}
			if (i < 0 || i >= n || i + n >= nextn) {
				int sc;
				// 如果扩充完成的标志变量已经为真，则结束线程
				if (finishing) {
					nextTable = null;
					table = nextTab;
					// sizeCtl阈值扩充为原来的1.5倍
					sizeCtl = (n << 1) - (n >>> 1);
					return;
				}
				// 使用CAS操作扩充阈值，在这里sizeCtl-1，说明新加入一个线程参与到扩容操作
				if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
					if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
						return;
					finishing = advance = true;
					i = n; // recheck before commit
				}
			}
			// 若遍历的节点为null，则放入到ForwadingNode标志节点中，表示该桶不在被处理
			else if ((f = tabAt(tab, i)) == null)
				advance = casTabAt(tab, i, null, fwd);
			// 如果f.hash == MOVED，表示遍历到了ForwadingNode节点，意味着该节点已经处理过了
			// 并发扩容的核心
			else if ((fh = f.hash) == MOVED)
				advance = true; // already processed
			else {
				// 使用synchronized给头节点加锁，保证线程安全
				synchronized (f) {
					// 开始节点复制工作
					if (tabAt(tab, i) == f) {
						// 创建两个节点头，用于拆分原Hash桶中的数据到两个新的Hash桶中
						Node<K, V> ln, hn;
						// 判断头结点的hash值是否大于0，若fh>=0，则可能是链表节点
						if (fh >= 0) {
							// 通过fh & n有效地将原Hash桶中的节点分为值为0和值为1的两类
							int runBit = fh & n;
							Node<K, V> lastRun = f;
							// 遍历找到原链表中的最后一段fh & n和runBit相同的链表节点，将其整段插入到新的链表中
							// lastRun为最后一段fh & n相同的链表节点的头结点
							for (Node<K, V> p = f.next; p != null; p = p.next) {
								int b = p.hash & n;
								if (b != runBit) {
									runBit = b;
									lastRun = p;
								}
							}
							// 根据runBit的值判断这段链表该插入到哪个新链表中。
							if (runBit == 0) {
								ln = lastRun;
								hn = null;
							} else {
								hn = lastRun;
								ln = null;
							}
							// 将其余节点插入两个新链表中，从尾部插入，先确定最后一个，然后指定next为最后一个，往前面插入
							for (Node<K, V> p = f; p != lastRun; p = p.next) {
								int ph = p.hash;
								K pk = p.key;
								V pv = p.val;
								if ((ph & n) == 0)
									ln = new Node<K, V>(ph, pk, pv, ln);
								else
									hn = new Node<K, V>(ph, pk, pv, hn);
							}
							// 将新链表分别插入新表中，将标志节点fwd插入原表中，链表数据拷贝完毕
							setTabAt(nextTab, i, ln);
							setTabAt(nextTab, i + n, hn);
							setTabAt(tab, i, fwd);
							advance = true;
						}
						// 如果待处理的Hash桶中的数据位树节点
						else if (f instanceof TreeBin) {
							TreeBin<K, V> t = (TreeBin<K, V>) f;
							// 创建lo与hi作为新树的两个根节点
							TreeNode<K, V> lo = null, loTail = null;
							TreeNode<K, V> hi = null, hiTail = null;
							int lc = 0, hc = 0;
							for (Node<K, V> e = t.first; e != null; e = e.next) {
								int h = e.hash;
								TreeNode<K, V> p = new TreeNode<K, V>(h, e.key, e.val, null, null);
								// 通过h & n == 0来将节点分为两类；同时维护树状结构和链表结构
								if ((h & n) == 0) {
									if ((p.prev = loTail) == null)
										lo = p;
									else
										loTail.next = p;
									loTail = p;
									++lc;
								} else {
									if ((p.prev = hiTail) == null)
										hi = p;
									else
										hiTail.next = p;
									hiTail = p;
									++hc;
								}
							}
							// 如果差分后的新树节点数小于阈值，则转换为链表
							ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) : (hc != 0) ? new TreeBin<K, V>(lo) : t;
							hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) : (lc != 0) ? new TreeBin<K, V>(hi) : t;
							// 将新链表分别插入新表中，将标志节点插入原表中，红黑树数据拷贝完成。
							setTabAt(nextTab, i, ln);
							setTabAt(nextTab, i + n, hn);
							setTabAt(tab, i, fwd);
							advance = true;
						}
					}
				}
			}
		}
	}

	@Override
	public V get(Object key) {
		Node<K, V>[] tab;
		Node<K, V> e, p;
		int n, eh;
		K ek;
		// 通过spread函数得到key值的再散列值
		int h = spread(key.hashCode());
		if ((tab = table) != null && (n = tab.length) > 0 && (e = tabAt(tab, (n - 1) & h)) != null) {
			// 如果恰好是该元素，就直接返回
			if ((eh = e.hash) == h) {
				if ((ek = e.key) == key || (ek != null && key.equals(ek)))
					return e.val;

			}
			// 如果hash为负值，表示此时正在扩容。这时查的是Forwarding Node的find方法来定位到nextTable来查找
			// 若查找到就返回
			else if (eh < 0)
				return (p = e.find(h, key)) != null ? p.val : null;
			// 若既不是首节点也不是forwarding node，则向下遍历
	        while ((e = e.next) != null) {
	            if (e.hash == h &&
	                ((ek = e.key) == key || (ek != null && key.equals(ek))))
	                return e.val;
	        }
	    }
		return null;
	}
	public int size() {
        long n = sumCount();
        return ((n < 0L) ? 0 :
                (n > (long)Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                (int)n);
    }
	final long  sumCount() {
		CounterCell[] as = counterCells; CounterCell a;
        long sum = baseCount;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
	}
	@Override
	public V remove(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean remove(Object key, Object value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V putIfAbsent(K key, V value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V replace(K key, V value) {
		// TODO Auto-generated method stub
		return null;
	}

}
