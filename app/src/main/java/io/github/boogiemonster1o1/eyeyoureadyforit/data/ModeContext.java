package io.github.boogiemonster1o1.eyeyoureadyforit.data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.LongStream;

@SuppressWarnings("unused")
public final class ModeContext implements Comparable<ModeContext>, Iterable<Long>, Cloneable, Serializable {
	private static final long serialVersionUID = 0L;
	private final long mode;
	private final int count;

	public ModeContext(long mode, int count) {
		this.mode = mode;
		this.count = count;
	}

	public long getMode() {
		return this.mode;
	}

	public int getCount() {
		return this.count;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mode, count);
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) return false;
		else if (o.getClass() != this.getClass()) return false;
		return ((ModeContext) o).getMode() == this.getMode() && ((ModeContext) o).getCount() == this.getCount();
	}

	@Override
	public String toString() {
		return "ModeContext{mode=" + this.getMode() + ", count=" + this.getCount() + "}";
	}

	@Override
	public int compareTo(ModeContext other) {
		if (other.getCount() == this.getCount()) {
			return 0;
		} else {
			return other.getCount() > this.getCount() ? -1 : 1;
		}
	}

	@Override
	public Iterator<Long> iterator() {
		return longStream().iterator();
	}

	public long[] array() {
		long[] arr = new long[this.getCount()];
		Arrays.fill(arr, this.getMode());
		return arr;
	}

	public LongStream longStream() {
		return Arrays.stream(this.array());
	}

	@Override
	public ModeContext clone() {
		try {
			return (ModeContext) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError("IMPOSSIBLE");
		}
	}
}
