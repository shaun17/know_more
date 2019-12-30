package map.hashmap;

import static org.hamcrest.CoreMatchers.instanceOf;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashMap.Node;
import java.util.HashMap.TreeNode;

public class SWHashMap<K, V> implements Map<K, V> {
	static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // 默认初始化长度 1<<4 = 10000 = 16
	static final int MAXIMUM_CAPACITY = 1 << 30; // max length
	static final float DEFAULT_LOAD_FACTOR = 0.75f; // 默认加载系数 0.75 当数组长度达到0.75的时候扩容
	static final int TREEIFY_THRESHOLD = 8; // 当链表超多8，会树化
	static final int UNTREEIFY_THRESHOLD = 6; // 当链表长度小于6 将转为链表结构
	static final int MIN_TREEIFY_CAPACITY = 64; // 当链表转换为tree之前，进行一次判断，判断
												// 键值对的数量查过64将链表进行转换为tree，因为避免hash运算的不合理分布导致大连数据都落在一条链上
	// 基本的hash桶，用于组成链表

	static class Node<K, V> implements Map.Entry<K, V> {
		final int hash;// hash值
		final K key;
		V value;
		Node<K, V> next; // 执行下一个节点

		Node(int hash, K key, V value, Node<K, V> next) {
			this.hash = hash;
			this.key = key;
			this.value = value;
			this.next = next;
		}

		public K getKey() {
			return key;
		}

		public V getValue() {
			return value;
		}

		public V setValue(V newvalue) {
			V oldValue = this.value;
			value = newvalue;
			return value;
		}

		public String toString() {
			return key + "=" + value;
		}

		// 重新定一个了hash算法，使用key的hashCode异或value的hashcode
		// 相同为0，不相同则为1
		public final int hashCode() {
			return Objects.hashCode(key) ^ Objects.hashCode(value);
		}

		public final boolean queals(Object o) {
			if (o == this) {
				return true;
			}
			if (o instanceof Map.Entry) {
				Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
				if (Objects.equals(key, e.getKey()) && Objects.equals(value, e.getValue()))
					return true;
			}
			return false;
		}

	}

	// 重写了hash算法，将计算出来的hash仔或异一下
	// h >>> 16位 hash值向右移动16位置 不管正负，高位补0.
	// ^ 相同为1 不同为0
	static final int hash(Object key) {
		int h;
		return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
	}

	/**
	 * Returns x's Class if it is of the form "class C implements Comparable<C>",
	 * else null.
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

	/**
	 * Returns a power of two size for the given target capacity. 返会给定目标的两倍冥 |= 为或运算
	 * n 或运算 n>>>1 并将结果赋值给n 即map扩容方法，2倍扩容 TODO 2倍扩容
	 */
	static final int tableSizeFor(int cap) {
		int n = cap - 1;
		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;
		return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
	}

	// 为桶的长度，每次扩容即2倍
	transient Node<K, V>[] table;
	// 存放map的entrySet for keySet and values
	transient Set<Map.Entry<K, V>> entrySet;
	// map的size
	transient int size;
	// 下次扩容的大小 capacity*loadFactor
	int threshold;
	// 自定义的负载因数
	final float loadFactor;
	/**
	 * The number of times this HashMap has been structurally modified Structural
	 * modifications are those that change the number of mappings in the HashMap or
	 * otherwise modify its internal structure (e.g., rehash). This field is used to
	 * make iterators on Collection-views of the HashMap fail-fast. (See
	 * ConcurrentModificationException).
	 */
	transient int modCount;

	// 自定以map初始化大小和负载因数
	public SWHashMap(int initialCapacity, float loadFactor) {
		if (initialCapacity < 0) {
			throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
		}
		if (initialCapacity > MAXIMUM_CAPACITY)
			initialCapacity = MAXIMUM_CAPACITY;
		if (loadFactor <= 0 || Float.isNaN(loadFactor))
			throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
		// 自定义负载因数
		this.loadFactor = loadFactor;
		// 扩容后大小
		this.threshold = tableSizeFor(initialCapacity);
	}

	// 自定义初始化大小
	public SWHashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR);
	}

	public SWHashMap() {
		this.loadFactor = DEFAULT_LOAD_FACTOR;
	}

	public SWHashMap(Map<? extends K, ? extends V> map) {
		this.loadFactor = DEFAULT_LOAD_FACTOR;
		putMapEntreies(map, false);
	}

	// TODO implements Map.putAll amd Map constructor
	final void putMapEntreies(Map<? extends K, ? extends V> m, boolean evict) {
		int s = m.size();
		if (s > 0) {
			if (table == null) {
				float ft = ((float) s / loadFactor) + 1.0F;
				int t = ((ft < (float) MAXIMUM_CAPACITY) ? (int) ft : MAXIMUM_CAPACITY);
				if (t > threshold)
					threshold = tableSizeFor(t);
				else if (s > threshold)
					resize();
				for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
					K key = e.getKey();
					V value = e.getValue();
					putVal(hash(key), key, value, false, evict);
				}
			}
		}

	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return size;
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return size == 0;
	}

	@Override
	public V get(Object key) {
		Node<K, V> e;
		return (e = getNode(hash(key), key)) == null ? null : e.value;
	}

	public Node<K, V> getNode(int hash, Object key) {
		Node<K, V>[] tab;
		Node<K, V> first, e;
		int n;
		K k;
		if ((tab = table) != null && (n = tab.length) > 0 && (first = tab[n - 1 & hash]) != null)
			if (first.hash == hash && ((k = first.key) == key || (key != null && key.equals(k))))
				return first;
		if ((e = first.next) != null) {
			if (first instanceof TreeNode) {
				return ((TreeNode<K, V>) first).getTreeNode(hash, key);
				do {
					if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
						return e;
				} while ((e = e.next) != null);
			}
		}
		return null;
	}

	@Override
	public boolean containsKey(Object key) {
		return getNode(hash(key), key) != null;
	}

	@Override
	public V put(K key, V value) {
		return putVal(hash(key), key, value, false, true);
	}

	final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
		Node<K, V>[] tab;
		Node<K, V> p;
		int n, i;
		if ((tab = table) == null || (n = tab.length) == 0)// 若数组为null，重置长度
			n = (tab = resize()).length;
		if ((p = tab[i = (n - 1) & hash]) == null)// 位置刚还为null 将Node创建直接放在该位置
			tab[i] = newNode(hash, key, value, null);
		else {// 该位置有元素
			Node<K, V> e;
			K k;
			if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
				//如果key相等，直接将新的node将老的替换
				e = p;
			else if (p instanceof TreeNode)//若为树结构
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
			else {
				for (int binCount = 0; ; ++binCount) {//遍历链上的数据
					if((e = p.next)==null) {
						p.next= newNode(hash, key, value, null);//若节点next为null，接在后面
						if(binCount >= TREEIFY_THRESHOLD - 1)
							treeifyBin(tab, hash);
	                    break;
					}
					if(e.hash ==hash && ((k = e.key) == key || (key != null && key.equals(k)))))
						break;
					p=e;//都不满足 则继续往遍历，next一直往下
				}
			}
			if(e != null) { // existing mapping for key
				V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                afterNodeAccess(e);
                return oldValue;
			}
		}
		++modCount;//增加修改次数
		if (++size > threshold)//当大于阈值 重置大小
            resize();
		afterNodeInsertion(evict);
        return null;
	}

	// 重置数组大小
	final Node<K, V>[] resize() {
		Node<K, V>[] oldTab = table;
		int oldCap = (oldTab == null) ? 0 : oldTab.length;// 旧容量
		int oldThr = threshold;// 旧的扩容阈值
		int newCap, newThr;
		if (oldCap > 0) {
			// 当就数组的长度大于最大值，无法扩容，返回旧数组
			if (oldCap >= MAXIMUM_CAPACITY) {
				threshold = Integer.MAX_VALUE;
				return oldTab;

			} else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY && oldCap >= DEFAULT_INITIAL_CAPACITY)
				// newCap扩容为oldCap的2倍<<1
				newThr = oldThr << 1; // 阈值一倍
		} else if (oldThr > 0) {
			newCap = oldThr;// 设置初始化扩容阈值
		} else { // 当初始阈值为0 使用默认值
			newCap = DEFAULT_INITIAL_CAPACITY;
			newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
		}
		if (newThr == 0) {
			float ft = (float) newCap * loadFactor;
			newThr = (newCap < MAXIMUM_CAPACITY && ft < (float) MAXIMUM_CAPACITY ? (int) ft : Integer.MAX_VALUE);
		}

		threshold = newThr;
		@SuppressWarnings({ "rawtypes", "unchecked" })
		// 此时newCap已经是扩容2倍后当大小，此时重新计算hash分布node
		Node<K, V>[] newTab = (Node<K, V>[]) new Node[newCap];
		if (oldTab != null) {
			for (int j = 0; j < oldCap; ++j) {// 遍历旧数组的全部节点
				Node<K, V> e;
				if ((e = oldTab[j]) != null) {
					oldTab[j] = null;
					if (e.next == null) {// 如果该节点为最后一个节点
						newTab[e.hash & (newCap - 1)] = e;// 使用hash值与容量长度-1 确定节点新位置
					} else if (e instanceof TreeNode) // 如果是tree结构
						((TreeNode<K, V>) e).split(this, newTab, j, oldCap);
					else {
						Node<K, V> loHead = null, loTail = null;
						Node<K, V> hiHead = null, hiTail = null;
						Node<K, V> next;
						do {
							next = e.next;
							// e.hash&oldCap 为偶数的一队 为奇数的一队
							// 仍然以链表的方式
							if ((e.hash & oldCap) == 0) {
								if (loTail == null)
									loHead = e;
								else
									loTail.next = e;
								hiTail = e;
							} else {
								if (hiTail == null)
									hiHead = e;
								else
									hiTail.next = e;
								hiTail = e;
							}
						} while ((e = next) != null);
						// 最后判断，将loHead放到新数组 原先的老位置
						if (loTail != null) {
							loTail.next = null;
							newTab[j] = loHead;
						}
						// 最后将hiHead放到新数组的新位置，在老位置+扩容的容量
						if (hiTail != null) {
							hiTail.next = null;
							newTab[j + oldCap] = hiHead;
						}
					}
				}
			}
		}
		return newTab;
	}

	@Override
	public boolean containsValue(Object value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public V remove(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<K> keySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<V> values() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		// TODO Auto-generated method stub
		return null;
	}

	/* ------------------------------------------------------------ */
	// Tree bins

	/**
	 * Entry for Tree bins. Extends LinkedHashMap.Entry (which in turn extends Node)
	 * so can be used as extension of either regular or linked node.
	 */
	static final class TreeNode<K, V> extends LinkedHashMap.Entry<K, V> {
		TreeNode<K, V> parent; // red-black tree links
		TreeNode<K, V> left;
		TreeNode<K, V> right;
		TreeNode<K, V> prev; // needed to unlink next upon deletion
		boolean red;

		TreeNode(int hash, K key, V val, Node<K, V> next) {
			super(hash, key, val, next);
		}

		/**
		 * Returns root of tree containing this node.
		 */
		final TreeNode<K, V> root() {
			for (TreeNode<K, V> r = this, p;;) {
				if ((p = r.parent) == null)
					return r;
				r = p;
			}
		}

		/**
		 * Ensures that the given root is the first node of its bin.
		 */
		static <K, V> void moveRootToFront(Node<K, V>[] tab, TreeNode<K, V> root) {
			int n;
			if (root != null && tab != null && (n = tab.length) > 0) {
				int index = (n - 1) & root.hash;
				TreeNode<K, V> first = (TreeNode<K, V>) tab[index];
				if (root != first) {
					Node<K, V> rn;
					tab[index] = root;
					TreeNode<K, V> rp = root.prev;
					if ((rn = root.next) != null)
						((TreeNode<K, V>) rn).prev = rp;
					if (rp != null)
						rp.next = rn;
					if (first != null)
						first.prev = root;
					root.next = first;
					root.prev = null;
				}
				assert checkInvariants(root);
			}
		}

		/**
		 * Finds the node starting at root p with the given hash and key. The kc
		 * argument caches comparableClassFor(key) upon first use comparing keys.
		 */
		final TreeNode<K, V> find(int h, Object k, Class<?> kc) {
			TreeNode<K, V> p = this;
			do {
				int ph, dir;
				K pk;
				TreeNode<K, V> pl = p.left, pr = p.right, q;
				if ((ph = p.hash) > h)
					p = pl;
				else if (ph < h)
					p = pr;
				else if ((pk = p.key) == k || (k != null && k.equals(pk)))
					return p;
				else if (pl == null)
					p = pr;
				else if (pr == null)
					p = pl;
				else if ((kc != null || (kc = comparableClassFor(k)) != null)
						&& (dir = compareComparables(kc, k, pk)) != 0)
					p = (dir < 0) ? pl : pr;
				else if ((q = pr.find(h, k, kc)) != null)
					return q;
				else
					p = pl;
			} while (p != null);
			return null;
		}

		/**
		 * Calls find for root node.
		 */
		final TreeNode<K, V> getTreeNode(int h, Object k) {
			return ((parent != null) ? root() : this).find(h, k, null);
		}

		/**
		 * Tie-breaking utility for ordering insertions when equal hashCodes and
		 * non-comparable. We don't require a total order, just a consistent insertion
		 * rule to maintain equivalence across rebalancings. Tie-breaking further than
		 * necessary simplifies testing a bit.
		 */
		static int tieBreakOrder(Object a, Object b) {
			int d;
			if (a == null || b == null || (d = a.getClass().getName().compareTo(b.getClass().getName())) == 0)
				d = (System.identityHashCode(a) <= System.identityHashCode(b) ? -1 : 1);
			return d;
		}

		/**
		 * Forms tree of the nodes linked from this node.
		 * 
		 * @return root of tree
		 */
		final void treeify(Node<K, V>[] tab) {
			TreeNode<K, V> root = null;
			for (TreeNode<K, V> x = this, next; x != null; x = next) {
				next = (TreeNode<K, V>) x.next;
				x.left = x.right = null;
				if (root == null) {
					x.parent = null;
					x.red = false;
					root = x;
				} else {
					K k = x.key;
					int h = x.hash;
					Class<?> kc = null;
					for (TreeNode<K, V> p = root;;) {
						int dir, ph;
						K pk = p.key;
						if ((ph = p.hash) > h)
							dir = -1;
						else if (ph < h)
							dir = 1;
						else if ((kc == null && (kc = comparableClassFor(k)) == null)
								|| (dir = compareComparables(kc, k, pk)) == 0)
							dir = tieBreakOrder(k, pk);

						TreeNode<K, V> xp = p;
						if ((p = (dir <= 0) ? p.left : p.right) == null) {
							x.parent = xp;
							if (dir <= 0)
								xp.left = x;
							else
								xp.right = x;
							root = balanceInsertion(root, x);
							break;
						}
					}
				}
			}
			moveRootToFront(tab, root);
		}

		/**
		 * Returns a list of non-TreeNodes replacing those linked from this node.
		 */
		final Node<K, V> untreeify(HashMap<K, V> map) {
			Node<K, V> hd = null, tl = null;
			for (Node<K, V> q = this; q != null; q = q.next) {
				Node<K, V> p = map.replacementNode(q, null);
				if (tl == null)
					hd = p;
				else
					tl.next = p;
				tl = p;
			}
			return hd;
		}

		/**
		 * Tree version of putVal.
		 */
		final TreeNode<K, V> putTreeVal(HashMap<K, V> map, Node<K, V>[] tab, int h, K k, V v) {
			Class<?> kc = null;
			boolean searched = false;
			TreeNode<K, V> root = (parent != null) ? root() : this;
			for (TreeNode<K, V> p = root;;) {
				int dir, ph;
				K pk;
				if ((ph = p.hash) > h)
					dir = -1;
				else if (ph < h)
					dir = 1;
				else if ((pk = p.key) == k || (k != null && k.equals(pk)))
					return p;
				else if ((kc == null && (kc = comparableClassFor(k)) == null)
						|| (dir = compareComparables(kc, k, pk)) == 0) {
					if (!searched) {
						TreeNode<K, V> q, ch;
						searched = true;
						if (((ch = p.left) != null && (q = ch.find(h, k, kc)) != null)
								|| ((ch = p.right) != null && (q = ch.find(h, k, kc)) != null))
							return q;
					}
					dir = tieBreakOrder(k, pk);
				}

				TreeNode<K, V> xp = p;
				if ((p = (dir <= 0) ? p.left : p.right) == null) {
					Node<K, V> xpn = xp.next;
					TreeNode<K, V> x = map.newTreeNode(h, k, v, xpn);
					if (dir <= 0)
						xp.left = x;
					else
						xp.right = x;
					xp.next = x;
					x.parent = x.prev = xp;
					if (xpn != null)
						((TreeNode<K, V>) xpn).prev = x;
					moveRootToFront(tab, balanceInsertion(root, x));
					return null;
				}
			}
		}

		/**
		 * Removes the given node, that must be present before this call. This is
		 * messier than typical red-black deletion code because we cannot swap the
		 * contents of an interior node with a leaf successor that is pinned by "next"
		 * pointers that are accessible independently during traversal. So instead we
		 * swap the tree linkages. If the current tree appears to have too few nodes,
		 * the bin is converted back to a plain bin. (The test triggers somewhere
		 * between 2 and 6 nodes, depending on tree structure).
		 */
		final void removeTreeNode(HashMap<K, V> map, Node<K, V>[] tab, boolean movable) {
			int n;
			if (tab == null || (n = tab.length) == 0)
				return;
			int index = (n - 1) & hash;
			TreeNode<K, V> first = (TreeNode<K, V>) tab[index], root = first, rl;
			TreeNode<K, V> succ = (TreeNode<K, V>) next, pred = prev;
			if (pred == null)
				tab[index] = first = succ;
			else
				pred.next = succ;
			if (succ != null)
				succ.prev = pred;
			if (first == null)
				return;
			if (root.parent != null)
				root = root.root();
			if (root == null || root.right == null || (rl = root.left) == null || rl.left == null) {
				tab[index] = first.untreeify(map); // too small
				return;
			}
			TreeNode<K, V> p = this, pl = left, pr = right, replacement;
			if (pl != null && pr != null) {
				TreeNode<K, V> s = pr, sl;
				while ((sl = s.left) != null) // find successor
					s = sl;
				boolean c = s.red;
				s.red = p.red;
				p.red = c; // swap colors
				TreeNode<K, V> sr = s.right;
				TreeNode<K, V> pp = p.parent;
				if (s == pr) { // p was s's direct parent
					p.parent = s;
					s.right = p;
				} else {
					TreeNode<K, V> sp = s.parent;
					if ((p.parent = sp) != null) {
						if (s == sp.left)
							sp.left = p;
						else
							sp.right = p;
					}
					if ((s.right = pr) != null)
						pr.parent = s;
				}
				p.left = null;
				if ((p.right = sr) != null)
					sr.parent = p;
				if ((s.left = pl) != null)
					pl.parent = s;
				if ((s.parent = pp) == null)
					root = s;
				else if (p == pp.left)
					pp.left = s;
				else
					pp.right = s;
				if (sr != null)
					replacement = sr;
				else
					replacement = p;
			} else if (pl != null)
				replacement = pl;
			else if (pr != null)
				replacement = pr;
			else
				replacement = p;
			if (replacement != p) {
				TreeNode<K, V> pp = replacement.parent = p.parent;
				if (pp == null)
					root = replacement;
				else if (p == pp.left)
					pp.left = replacement;
				else
					pp.right = replacement;
				p.left = p.right = p.parent = null;
			}

			TreeNode<K, V> r = p.red ? root : balanceDeletion(root, replacement);

			if (replacement == p) { // detach
				TreeNode<K, V> pp = p.parent;
				p.parent = null;
				if (pp != null) {
					if (p == pp.left)
						pp.left = null;
					else if (p == pp.right)
						pp.right = null;
				}
			}
			if (movable)
				moveRootToFront(tab, r);
		}

		/**
		 * Splits nodes in a tree bin into lower and upper tree bins, or untreeifies if
		 * now too small. Called only from resize; see above discussion about split bits
		 * and indices.
		 *
		 * @param map   the map
		 * @param tab   the table for recording bin heads
		 * @param index the index of the table being split
		 * @param bit   the bit of hash to split on
		 */
		final void split(HashMap<K, V> map, Node<K, V>[] tab, int index, int bit) {
			TreeNode<K, V> b = this;
			// Relink into lo and hi lists, preserving order
			TreeNode<K, V> loHead = null, loTail = null;
			TreeNode<K, V> hiHead = null, hiTail = null;
			int lc = 0, hc = 0;
			for (TreeNode<K, V> e = b, next; e != null; e = next) {
				next = (TreeNode<K, V>) e.next;
				e.next = null;
				if ((e.hash & bit) == 0) {
					if ((e.prev = loTail) == null)
						loHead = e;
					else
						loTail.next = e;
					loTail = e;
					++lc;
				} else {
					if ((e.prev = hiTail) == null)
						hiHead = e;
					else
						hiTail.next = e;
					hiTail = e;
					++hc;
				}
			}

			if (loHead != null) {
				if (lc <= UNTREEIFY_THRESHOLD)
					tab[index] = loHead.untreeify(map);
				else {
					tab[index] = loHead;
					if (hiHead != null) // (else is already treeified)
						loHead.treeify(tab);
				}
			}
			if (hiHead != null) {
				if (hc <= UNTREEIFY_THRESHOLD)
					tab[index + bit] = hiHead.untreeify(map);
				else {
					tab[index + bit] = hiHead;
					if (loHead != null)
						hiHead.treeify(tab);
				}
			}
		}

		/* ------------------------------------------------------------ */
		// Red-black tree methods, all adapted from CLR

		static <K, V> TreeNode<K, V> rotateLeft(TreeNode<K, V> root, TreeNode<K, V> p) {
			TreeNode<K, V> r, pp, rl;
			if (p != null && (r = p.right) != null) {
				if ((rl = p.right = r.left) != null)
					rl.parent = p;
				if ((pp = r.parent = p.parent) == null)
					(root = r).red = false;
				else if (pp.left == p)
					pp.left = r;
				else
					pp.right = r;
				r.left = p;
				p.parent = r;
			}
			return root;
		}

		static <K, V> TreeNode<K, V> rotateRight(TreeNode<K, V> root, TreeNode<K, V> p) {
			TreeNode<K, V> l, pp, lr;
			if (p != null && (l = p.left) != null) {
				if ((lr = p.left = l.right) != null)
					lr.parent = p;
				if ((pp = l.parent = p.parent) == null)
					(root = l).red = false;
				else if (pp.right == p)
					pp.right = l;
				else
					pp.left = l;
				l.right = p;
				p.parent = l;
			}
			return root;
		}

		static <K, V> TreeNode<K, V> balanceInsertion(TreeNode<K, V> root, TreeNode<K, V> x) {
			x.red = true;
			for (TreeNode<K, V> xp, xpp, xppl, xppr;;) {
				if ((xp = x.parent) == null) {
					x.red = false;
					return x;
				} else if (!xp.red || (xpp = xp.parent) == null)
					return root;
				if (xp == (xppl = xpp.left)) {
					if ((xppr = xpp.right) != null && xppr.red) {
						xppr.red = false;
						xp.red = false;
						xpp.red = true;
						x = xpp;
					} else {
						if (x == xp.right) {
							root = rotateLeft(root, x = xp);
							xpp = (xp = x.parent) == null ? null : xp.parent;
						}
						if (xp != null) {
							xp.red = false;
							if (xpp != null) {
								xpp.red = true;
								root = rotateRight(root, xpp);
							}
						}
					}
				} else {
					if (xppl != null && xppl.red) {
						xppl.red = false;
						xp.red = false;
						xpp.red = true;
						x = xpp;
					} else {
						if (x == xp.left) {
							root = rotateRight(root, x = xp);
							xpp = (xp = x.parent) == null ? null : xp.parent;
						}
						if (xp != null) {
							xp.red = false;
							if (xpp != null) {
								xpp.red = true;
								root = rotateLeft(root, xpp);
							}
						}
					}
				}
			}
		}

		static <K, V> TreeNode<K, V> balanceDeletion(TreeNode<K, V> root, TreeNode<K, V> x) {
			for (TreeNode<K, V> xp, xpl, xpr;;) {
				if (x == null || x == root)
					return root;
				else if ((xp = x.parent) == null) {
					x.red = false;
					return x;
				} else if (x.red) {
					x.red = false;
					return root;
				} else if ((xpl = xp.left) == x) {
					if ((xpr = xp.right) != null && xpr.red) {
						xpr.red = false;
						xp.red = true;
						root = rotateLeft(root, xp);
						xpr = (xp = x.parent) == null ? null : xp.right;
					}
					if (xpr == null)
						x = xp;
					else {
						TreeNode<K, V> sl = xpr.left, sr = xpr.right;
						if ((sr == null || !sr.red) && (sl == null || !sl.red)) {
							xpr.red = true;
							x = xp;
						} else {
							if (sr == null || !sr.red) {
								if (sl != null)
									sl.red = false;
								xpr.red = true;
								root = rotateRight(root, xpr);
								xpr = (xp = x.parent) == null ? null : xp.right;
							}
							if (xpr != null) {
								xpr.red = (xp == null) ? false : xp.red;
								if ((sr = xpr.right) != null)
									sr.red = false;
							}
							if (xp != null) {
								xp.red = false;
								root = rotateLeft(root, xp);
							}
							x = root;
						}
					}
				} else { // symmetric
					if (xpl != null && xpl.red) {
						xpl.red = false;
						xp.red = true;
						root = rotateRight(root, xp);
						xpl = (xp = x.parent) == null ? null : xp.left;
					}
					if (xpl == null)
						x = xp;
					else {
						TreeNode<K, V> sl = xpl.left, sr = xpl.right;
						if ((sl == null || !sl.red) && (sr == null || !sr.red)) {
							xpl.red = true;
							x = xp;
						} else {
							if (sl == null || !sl.red) {
								if (sr != null)
									sr.red = false;
								xpl.red = true;
								root = rotateLeft(root, xpl);
								xpl = (xp = x.parent) == null ? null : xp.left;
							}
							if (xpl != null) {
								xpl.red = (xp == null) ? false : xp.red;
								if ((sl = xpl.left) != null)
									sl.red = false;
							}
							if (xp != null) {
								xp.red = false;
								root = rotateRight(root, xp);
							}
							x = root;
						}
					}
				}
			}
		}

		/**
		 * Recursive invariant check
		 */
		static <K, V> boolean checkInvariants(TreeNode<K, V> t) {
			TreeNode<K, V> tp = t.parent, tl = t.left, tr = t.right, tb = t.prev, tn = (TreeNode<K, V>) t.next;
			if (tb != null && tb.next != t)
				return false;
			if (tn != null && tn.prev != t)
				return false;
			if (tp != null && t != tp.left && t != tp.right)
				return false;
			if (tl != null && (tl.parent != t || tl.hash > t.hash))
				return false;
			if (tr != null && (tr.parent != t || tr.hash < t.hash))
				return false;
			if (t.red && tl != null && tl.red && tr != null && tr.red)
				return false;
			if (tl != null && !checkInvariants(tl))
				return false;
			if (tr != null && !checkInvariants(tr))
				return false;
			return true;
		}
	}

}
