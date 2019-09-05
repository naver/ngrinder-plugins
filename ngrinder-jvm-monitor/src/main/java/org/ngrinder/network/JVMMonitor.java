package org.ngrinder.network;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.grinder.common.GrinderProperties;
import net.grinder.statistics.ImmutableStatisticsSet;

import org.apache.commons.io.IOUtils;
import org.ngrinder.common.util.Preconditions;
import org.ngrinder.extension.OnTestSamplingRunnable;
import org.ngrinder.model.PerfTest;

import org.ngrinder.service.IConfig;
import org.ngrinder.service.IPerfTestService;
import org.ngrinder.service.IScheduledTaskService;
import org.ngrinder.service.ISingleConsole;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * OnTestSamplingRunnable implements class
 * *
 * @author JunHo Yoon, Geunwoo Son
 * @since 3.4
 */

public class JVMMonitor extends Plugin {
	private static final Logger LOGGER = LoggerFactory.getLogger(JVMMonitor.class);

	public JVMMonitor(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Extension
	public static class JVMMonitorExtension implements OnTestSamplingRunnable, Runnable {
		@Autowired
		private IConfig config;
		@Autowired
		private IScheduledTaskService scheduledTaskService;
		private static final int DEFAULT_JMX_PORT = 5000;
		private static final String PLUGIN_NAME = "jvm_monitor";
		private static final String JMX_MONITOR_PORT = "plugin.jvm_monitor.port";
		private final Map<JVMMonitorClient, BufferedWriter> clientMap = new ConcurrentHashMap<JVMMonitorClient, BufferedWriter>();

		public JVMMonitorExtension() {
		}

		public void run() {
			for (JVMMonitorClient each : this.clientMap.keySet())
				each.update();
		}

		@Override
		public void startSampling(final ISingleConsole singleConsole, PerfTest perfTest,
			IPerfTestService perfTestService) {
			final int jmxPort = getJmxPort(singleConsole.getGrinderProperties());
			List<String> targetHostIP = perfTest.getTargetHostIP();
			Integer samplingInterval = perfTest.getSamplingInterval();
			for (final String target : targetHostIP) {
				this.scheduledTaskService.runAsync(new Runnable() {
					public void run() {
						JVMMonitor.LOGGER.info("Start JVM monitoring for IP:{}", target);
						JVMMonitorClient client = new JVMMonitorClient(target, jmxPort);
						client.init();
						if (client.isConnected()) {
							File testReportDir = singleConsole.getReportPath();
							File dataFile = null;
							try {
								File pluginDir = new File(testReportDir, PLUGIN_NAME);
								if (!pluginDir.exists()) {
									Preconditions.checkTrue(pluginDir.mkdir(),
										"Report directory should be created.");
								}
								dataFile = new File(pluginDir, target + ".data");
								FileWriter fileWriter = new FileWriter(dataFile, false);
								BufferedWriter bw = new BufferedWriter(fileWriter);

								bw.write("Heap Size,Used Heap Size,PermGen Size,Used PermGen Size,Minor GC Count,Full GC Count,Thread Count");
								bw.newLine();
								bw.flush();
								JVMMonitorExtension.this.clientMap.put(client, bw);
							} catch (IOException e) {
								JVMMonitor.LOGGER.error("Error to write to file:{}, Error:{}",
									dataFile.getPath(), e.getMessage());
							}
						}
					}
				});
			}
			assignScheduledTask(samplingInterval);
		}

		protected void assignScheduledTask(Integer samplingInterval) {
			this.scheduledTaskService.addFixedDelayedScheduledTask(this,
				samplingInterval.intValue() * 1000);
		}

		public int getJmxPort(GrinderProperties grinderProperties) {
			int port = -1;
			if (grinderProperties != null) {
				port = grinderProperties.getInt(JMX_MONITOR_PORT, -1);
			}
			if (port == -1) {
				port = this.config.getControllerProperties().getPropertyInt(JMX_MONITOR_PORT,
					DEFAULT_JMX_PORT);
			}
			return port;
		}

		@Override
		public void sampling(ISingleConsole singleConsole, PerfTest perfTest,
			IPerfTestService perfTestService, ImmutableStatisticsSet intervalStatistics,
			ImmutableStatisticsSet cumulativeStatistics) {
			for (Map.Entry each : this.clientMap.entrySet())
				try {
					JVMInfo currentInfo = ((JVMMonitorClient) each.getKey()).getJVMInfo();
					if (currentInfo != null) {
						BufferedWriter bw = (BufferedWriter) each.getValue();
						bw.write(currentInfo.toMonitorDataLine());
						bw.newLine();
					}
				} catch (IOException e) {
					LOGGER.error("Error while saving file :" + e.getMessage());
				}
		}

		@Override
		public void endSampling(ISingleConsole singleConsole, PerfTest perfTest,
			IPerfTestService perfTestService) {
			try {
				this.scheduledTaskService.removeScheduledJob(this);
			} catch (Exception e) {
			}
			for (Map.Entry each : this.clientMap.entrySet()) {
				IOUtils.closeQuietly((Closeable) each.getKey());
				IOUtils.closeQuietly((Writer) each.getValue());
			}
			this.clientMap.clear();
		}

	}
}
