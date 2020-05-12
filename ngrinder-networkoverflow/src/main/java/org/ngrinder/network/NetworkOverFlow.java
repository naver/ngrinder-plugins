package org.ngrinder.network;

import net.grinder.common.processidentity.AgentIdentity;
import net.grinder.statistics.ImmutableStatisticsSet;
import net.grinder.statistics.StatisticsIndexMap;
import net.grinder.statistics.StatisticsIndexMap.LongIndex;
import net.grinder.util.Pair;
import net.grinder.util.UnitUtils;

import org.apache.commons.lang3.StringUtils;
import org.ngrinder.extension.OnTestSamplingRunnable;
import org.ngrinder.model.AgentInfo;
import org.ngrinder.model.PerfTest;
import org.ngrinder.model.Status;
import org.ngrinder.service.IAgentManagerService;
import org.ngrinder.service.IConfig;
import org.ngrinder.service.IPerfTestService;
import org.ngrinder.service.ISingleConsole;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Network overflow plugin.
 * This plugin blocks test running which causes the network overflow by the large test.
 *
 * @since 3.3
 */
public class NetworkOverFlow extends Plugin {
	private static final Logger LOGGER = LoggerFactory.getLogger(NetworkOverFlow.class);

	public NetworkOverFlow(PluginWrapper wrapper) {
		super(wrapper);
	}

	@Extension
	public static class NetworkOverFlowExtension implements OnTestSamplingRunnable {
		private long limit;
		private static final String PROP_NETWORK_OVERFLOW_PERTEST_LIMIT = "plugin.networkoverflow.pertest.limit";
		private static final int PROP_NETWORK_OVERFLOW_PERTEST_LIMIT_DEFAULT = 128;

		private static final int RETRY_DELAY = 300;
		private static final int RETRY_LIMIT = 3;

		@Autowired
		private IConfig config;

		@Autowired
		private IAgentManagerService agentManagerService;

		public void startSampling(ISingleConsole singleConsole, PerfTest perfTest,
			IPerfTestService perfTestService) {
			LOGGER.info("Test Id: {}", perfTest.getId());
			LOGGER.info("Test Name: {}, Test Time: {}", perfTest.getTestName(), perfTest.getCreatedDate());
			List<AgentIdentity> allAttachedAgents = singleConsole.getAllAttachedAgents();
			int consolePort = singleConsole.getConsolePort();
			LOGGER.info("getLocalAgent count: {}, consolePort: {}", getLocalAgents().size(), consolePort);

			int agentCount = 0;
			int userSpecificAgentCount = 0;

			int retryCount = 0;
			while (retryCount++ < RETRY_LIMIT) {
				Pair<Integer, Integer> counts = getAgentCount(consolePort);
				agentCount = counts.getFirst();
				userSpecificAgentCount = counts.getSecond();

				if (agentCount == allAttachedAgents.size()) {
					break;
				} else {
					LOGGER.info("Retry to calculate network limit. Agents are not fully ready for test. Retry count : " + retryCount);
					if (retryCount < RETRY_LIMIT) {
						sleep(RETRY_DELAY);
					}
				}
			}

			long configuredLimit = getLimit();
			int totalAgentSize = allAttachedAgents.size();
			int sharedAgent = (totalAgentSize - userSpecificAgentCount);

			LOGGER.info("Plugin Info: totalAgentSize: {}, userSpecificAgentCount: {}", totalAgentSize, userSpecificAgentCount);
			LOGGER.info("Plugin Info: configuredLimit: {}, sharedAgent: {}", configuredLimit, sharedAgent);

			limit = sharedAgent == 0 ? Long.MAX_VALUE : (long) (configuredLimit / (((float) sharedAgent) / totalAgentSize));
		}

		private Pair<Integer, Integer> getAgentCount(int consolePort) {
			int agentCount = 0;
			int userSpecificAgentCount = 0;

			for (AgentInfo each : getLocalAgents()) {
				LOGGER.info("Agent name: " + each.getName() + ", Agent region: " + each.getRegion() + ", Agent port: " + each.getPort());
				if (each.getPort() == consolePort) {
					agentCount++;
					if (StringUtils.contains(each.getRegion(), "owned")) {
						LOGGER.info("userSpecific agent name: {}, userSpecific agent region: {}", each.getName(), each.getRegion());
						userSpecificAgentCount++;
					}
				}
			}

			return Pair.of(agentCount, userSpecificAgentCount);
		}

		private void sleep(int millis) {
			try {
				MILLISECONDS.sleep(millis);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		int getLimit() {
			return this.config.getControllerProperties().getPropertyInt(
				PROP_NETWORK_OVERFLOW_PERTEST_LIMIT, PROP_NETWORK_OVERFLOW_PERTEST_LIMIT_DEFAULT) * 1024 * 1024;
		}

		List<AgentInfo> getLocalAgents() {
			return agentManagerService.getLocalAgents();
		}

		public void sampling(ISingleConsole singleConsole, PerfTest perfTest,
			IPerfTestService perfTestService, ImmutableStatisticsSet intervalStatistics,
			ImmutableStatisticsSet cumulativeStatistics) {
			LongIndex longIndex = singleConsole.getStatisticsIndexMap().getLongIndex(
				StatisticsIndexMap.HTTP_PLUGIN_RESPONSE_LENGTH_KEY);
			long byteSize = intervalStatistics.getValue(longIndex);
			if (byteSize > this.limit) {
				if (perfTest.getStatus() != Status.ABNORMAL_TESTING) {
					String message = String.format(
						"Too much traffic on this test. Stop by force.\n"
							+ "- LIMIT : %s - SENT :%s",
						UnitUtils.byteCountToDisplaySize(this.limit),
						UnitUtils.byteCountToDisplaySize(byteSize));
					LOGGER.info(message);
					LOGGER.info("Stop the test {} by force", perfTest.getTestIdentifier());
					perfTestService.markStatusAndProgress(perfTest, Status.ABNORMAL_TESTING,
						message);
				}
			}

		}

		public void endSampling(ISingleConsole singleConsole, PerfTest perfTest,
			IPerfTestService perfTestService) {
		}
	}

}
