package org.brunel.data.auto;

import org.brunel.data.Data;
import org.brunel.data.Field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A single span of domains
 */
public class DomainSpan implements Comparable<DomainSpan> {

	/**
	 * Build a domain from a numeric range
	 *
	 * @param f the field to use
	 * @return (numeric) domain span
	 */
	static DomainSpan make(Field f, int index, boolean preferContinuous) {
		if (f.isNumeric()) {
			// If any position are counts or sums, we want to include zero
			boolean needZero = f.name.equals("#count") || "sum".equals(f.strProperty("summary"));
			SpanNumericInfo numeric = new SpanNumericInfo(f.min(), f.max(), f.isDate(), needZero);
			return new DomainSpan(index, preferContinuous, f.preferCategorical() ? f.categories() : null, numeric);
		} else {
			return new DomainSpan(index, preferContinuous, f.categories(), null);
		}
	}

	public final int index;                    // To preserve the order we added them in
	private final boolean preferContinuous;
	final Object[] categories;          // Categorical data
	final SpanNumericInfo numeric;        // Numeric info

	private DomainSpan(int index, boolean preferContinuous, Object[] categories, SpanNumericInfo numeric) {
		this.index = index;
		this.preferContinuous = preferContinuous;
		this.categories = categories;
		this.numeric = numeric;
	}

	/**
	 * Sorting
	 * Base order is date, numeric, categorical
	 * Within numerics, sort by min values. Numerics sort by size f categories
	 *
	 * @param o other to compare to
	 * @return sort order
	 */
	public int compareTo(DomainSpan o) {
		// Types comparison first
		int d = typeScore() - o.typeScore();
		if (d != 0) return d;

		// Order by low value, or nothing for non-numeric
		if (categories == null) return Data.compare(numeric.low, o.numeric.low);
		return index - o.index;
	}

	public boolean desiresZero() {
		return numeric != null && numeric.includeZeroDesired;
	}

	private int typeScore() {
		// Purely categorical goes last
		if (numeric == null) return 5;
		// Then time (preferring pure form first) followed by general numeric (preferring pure form)
		return (numeric.isDate ? 1 : 3) + (categories == null ? 0 : 1);
	}

	public Object[] content() {
		// we return numeric data if no categorical data, or we prefer the numeric data
		if (categories == null || preferContinuous && numeric != null) {
			return numeric.range();
		} else {
			return categories;
		}
	}

	/**
	 * Attempt to update this domain also to include another domain
	 *
	 * @param o the other domain to include
	 * @return the merged domain, or null if nothing could be done
	 */
	public DomainSpan merge(DomainSpan o) {
		Object[] mCats = mergeCategories(categories, o.categories);
		SpanNumericInfo mNum = numeric == null ? null : numeric.merge(o.numeric);

		if (mCats == null && mNum == null)                        // No match either for continuous or categories
			return null;

		// Successfully merged domain span
		return new DomainSpan(Math.min(index, o.index), preferContinuous && o.preferContinuous, mCats, mNum);
	}

	public boolean isDate() {
		return numeric != null && numeric.isDate;
	}

	private static Object[] mergeCategories(Object[] a, Object[] b) {
		if (a == null || b == null) return null;

		Set<Object> all = new HashSet<>();
		Collections.addAll(all, a);                                // Add all our categories to the set
		List<Object> ordered = new ArrayList<>();
		Collections.addAll(ordered, a);                            // Our categories go first
		for (Object o : b)                                        // Add new categories from the other list
			if (!all.contains(o)) ordered.add(o);
		return ordered.toArray(new Object[ordered.size()]);
	}

	public double relativeSize() {
		// Categories take up less space if there are fewer than 8 of them
		return categories == null ? 1.0 : Math.min(categories.length, 8) / 8.0;
	}
}
