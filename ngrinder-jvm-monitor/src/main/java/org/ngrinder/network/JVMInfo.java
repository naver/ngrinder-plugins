package org.ngrinder.network;

/**
 * JVMInfo class.
 *
 * @author JunHo Yoon, Geunwoo Son
 * @since 3.4
 */
public class JVMInfo {
	private long minorGCCount;
	private long fullGCCount;
	private long heapSize;
	private long usedHeapSize;
	private long permGenSize;
	private long usedPermGenSize;
	public static final String MONITOR_DATA_HEADER = "Heap Size,Used Heap Size,PermGen Size,Used PermGen Size,Minor GC Count,Full GC Count,Thread Count";
	private int threadCount;

	public String toMonitorDataLine() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.heapSize).append(",");
		sb.append(this.usedHeapSize).append(",");
		sb.append(this.permGenSize).append(",");
		sb.append(this.usedPermGenSize).append(",");
		sb.append(this.minorGCCount).append(",");
		sb.append(this.fullGCCount).append(",");
		sb.append(this.threadCount);
		return sb.toString();
	}

	public void setFullGCCount(long fullGCCount) {
		this.fullGCCount = fullGCCount;
	}

	public void setMinorGCCount(long minorGCCount) {
		this.minorGCCount = minorGCCount;
	}

	public void setHeapSize(long heapSize) {
		this.heapSize = heapSize;
	}

	public void setUsedHeapSize(long usedHeapSize) {
		this.usedHeapSize = usedHeapSize;
	}

	public void setPermGenSize(long permGenSize) {
		this.permGenSize = permGenSize;
	}

	public void setUsedPermGenSize(long usedPermGenSize) {
		this.usedPermGenSize = usedPermGenSize;
	}

	public void setThreadCount(int threadCount) {
		this.threadCount = threadCount;
	}

}
