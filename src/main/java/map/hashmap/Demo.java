package map.hashmap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.util.comparator.ComparableComparator;

public class Demo {
	public static void main(String[] args) {
		List<String> list = Arrays.asList("1","22","333","4444","55555");
//		Collections.sort(list, new Comparator<String>() {
//			@Override
//			public int compare(String o1, String o2) {
//				return o2.length()-o1.length();
//			}
//		});
		Collections.sort(list, (s1,s2)->(s1.length()-s2.length()));
		System.out.println(list);
	}
}
