package org.ngrinder.network;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.ngrinder.monitor.share.domain.MBeanClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JVMMonitorClient class.
 *
 * @author JunHo Yoon, Geunwoo Son
 * @since 3.4
 */
public class JVMMonitorClient implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(JVMMonitorClient.class);
	private MBeanClient mBeanClient;
	private JVMInfo jvmInfo = new JVMInfo();
	private final String host;
	private ObjectName memObjName;
	private ObjectName threadObjName;
	private ObjectName fullGCObjName;
	private ObjectName minorGCObjName;
	private ObjectName permGenObjName;
	public static Set<String> oldGCNames = new HashSet<String>();
	public static Set<String> youngGCNames = new HashSet<String>();
	private static final String gcObjectNameQuery = "java.lang:type=GarbageCollector,name=*";
	private static final String memObjectNameString = "java.lang:type=Memory";
	private static final String permGenObjectNameString = "java.lang:type=MemoryPool,name=Perm Gen";
	private static final String threadObjectNameString = "java.lang:type=Threading";

	static {
		youngGCNames.add("Copy");
		youngGCNames.add("ParNew");
		youngGCNames.add("PS Scavenge");
		youngGCNames.add("Garbage collection optimized for short pausetimes Young Collector");
		youngGCNames.add("Garbage collection optimized for throughput Young Collector");
		youngGCNames.add("Garbage collection optimized for deterministic pausetimes Young Collector");

		oldGCNames.add("MarkSweepCompact");
		oldGCNames.add("PS MarkSweep");
		oldGCNames.add("ConcurrentMarkSweep");
		oldGCNames.add("Garbage collection optimized for short pausetimes Old Collector");
		oldGCNames.add("Garbage collection optimized for throughput Old Collector");
		oldGCNames.add("Garbage collection optimized for deterministic pausetimes Old Collector");
	}

	public JVMMonitorClient(String host, int port) {
		this.host = host;
		try {
			this.mBeanClient = new MBeanClient(host, port);
		} catch (IOException e) {
			LOGGER.info("Error to get JMX connection to :{}:{}", host, Integer.valueOf(port));
		}
	}

	public boolean isConnected() {
		return this.mBeanClient.isConnected();
	}

	public void init() {
		LOGGER.debug("Connecting to :{}", this.host);
		this.mBeanClient.connect();
		LOGGER.debug("Connection finished, isConnected :{}",
			Boolean.valueOf(this.mBeanClient.isConnected()));
		if (this.mBeanClient.isConnected()) {
			MBeanServerConnection server = this.mBeanClient.getMBeanServerConnection();
			try {
				Set<ObjectName> nameSet = server.queryNames(new ObjectName(gcObjectNameQuery), null);
				for (ObjectName objName : nameSet) {
					if (isMinorGC(objName.getCanonicalName()))
						this.minorGCObjName = objName;
					else if (isFullGC(objName.getCanonicalName())) {
						this.fullGCObjName = objName;
					}
				}
				this.memObjName = new ObjectName(memObjectNameString);
				this.permGenObjName = new ObjectName(permGenObjectNameString);
				this.threadObjName = new ObjectName(threadObjectNameString);
				LOGGER.debug("Object name resolution finished.");
			} catch (Exception e) {
				LOGGER.warn("Error to get object name from:{}, because:", this.host, e.getMessage());
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	private boolean isMinorGC(String objName) {
		for (String name : youngGCNames) {
			if (objName.contains(name)) {
				return true;
			}
		}
		return false;
	}

	private boolean isFullGC(String objName) {
		for (String name : oldGCNames) {
			if (objName.contains(name)) {
				return true;
			}
		}
		return false;
	}

	public void close() {
		this.mBeanClient.disconnect();
	}

	public JVMInfo getJVMInfo() {
		return this.jvmInfo;
	}

	public void update() {
		try {
			if (this.mBeanClient.isConnected()) {
				JVMInfo newStatus = new JVMInfo();
				try {
					String collectionCountAttrName = "CollectionCount";
					Long minorGCCount = (Long) this.mBeanClient.getAttribute(this.minorGCObjName,
						collectionCountAttrName);
					newStatus.setMinorGCCount(minorGCCount.longValue());
					Long fullGCCount = (Long) this.mBeanClient.getAttribute(this.fullGCObjName,
						collectionCountAttrName);
					newStatus.setFullGCCount(fullGCCount.longValue());
				} catch (Exception e) {
				}
				try {
					String memAttrName = "HeapMemoryUsage";
					CompositeData memData = (CompositeData) this.mBeanClient.getAttribute(
						this.memObjName, memAttrName);
					newStatus.setHeapSize(((Long) memData.get("committed")).longValue());
					newStatus.setUsedHeapSize(((Long) memData.get("used")).longValue());
				} catch (Exception e) {
				}
				try {
					String permGenAttrName = "Usage";
					CompositeData permGenData = (CompositeData) this.mBeanClient.getAttribute(
						this.permGenObjName, permGenAttrName);
					newStatus.setPermGenSize(((Long) permGenData.get("committed")).longValue());
					newStatus.setUsedPermGenSize(((Long) permGenData.get("used")).longValue());
				} catch (Exception e) {
				}
				String threadAttrName = "ThreadCount";
				Integer threadCount = (Integer) this.mBeanClient.getAttribute(this.threadObjName,
					threadAttrName);
				newStatus.setThreadCount(threadCount.intValue());
				this.jvmInfo = newStatus;
			}
		} catch (Exception e) {
			LOGGER.error("Error to get JMX data from :{}", this.host);
			LOGGER.error("Reason :{}", e);
		}
	}

}
