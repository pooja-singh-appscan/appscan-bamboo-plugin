/**
 * (c) Copyright HCL Technologies Ltd. 2020.
 * LICENSE: Apache License, Version 2.0 https://www.apache.org/licenses/LICENSE-2.0
 */

package com.hcl.appscan.bamboo.plugin.impl;

import com.atlassian.bamboo.build.artifact.ArtifactManager;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.core.util.FileUtils;
import com.hcl.appscan.bamboo.plugin.auth.BambooAuthenticationProvider;
import com.hcl.appscan.bamboo.plugin.util.ScanProgress;
import com.hcl.appscan.sdk.CoreConstants;
import com.hcl.appscan.sdk.error.InvalidTargetException;
import com.hcl.appscan.sdk.error.ScannerException;
import com.hcl.appscan.sdk.logging.IProgress;
import com.hcl.appscan.sdk.results.NonCompliantIssuesResultProvider;
import com.hcl.appscan.sdk.scan.IScan;
import com.hcl.appscan.sdk.scanners.dynamic.DASTConstants;
import com.hcl.appscan.sdk.scanners.dynamic.DASTScanFactory;

import java.io.File;
import java.util.Map;

public class ASoCDASTScanner extends AbstractASoCScanner {
	private ProcessService processService;
	private BambooAuthenticationProvider authenticationProvider;
	private String jobId;

	public ASoCDASTScanner(LogHelper logger, ArtifactManager artifactManager, ProcessService processService) {
		super(logger, artifactManager);
		this.processService = processService;
	}

	@Override
	public String getScannerType() {
		return DASTConstants.DAST;
	}

	@Override
	public void scheduleScan(TaskContext taskContext) throws TaskException {
		logger.info("scan.schedule.dynamic");
		IProgress progress = new ScanProgress(logger);
		authenticationProvider = new BambooAuthenticationProvider(credential);
		DASTScanFactory scanFactory = new DASTScanFactory();

		Map<String, String> scanProperties = getScanProperties(taskContext);
		IScan scan = scanFactory.create(scanProperties, progress, authenticationProvider);
		try {
			scan.run();
			jobId = scan.getScanId();
			logger.info("scan.schedule.success", jobId);
			String homepageUrl = authenticationProvider.getServer() + "/serviceui/main/myapps/portfolio";
			logger.info("asoc.homepage.url", homepageUrl);

			provider = new NonCompliantIssuesResultProvider(scan.getScanId(), scan.getType(), scan.getServiceProvider(), progress);
			provider.setReportFormat(scan.getReportFormat());
			resultsRetriever = new ResultsRetriever(provider);
		} catch (ScannerException e) {
			logger.error("err.scan.schedule", e.getLocalizedMessage());
			throw new TaskException(e.getLocalizedMessage(), e.getCause());
		} catch (InvalidTargetException e) {
			logger.error("err.scan.schedule", e.getLocalizedMessage());
			throw new TaskException(e.getLocalizedMessage(), e.getCause());
		}
	}

	@Override
	protected Map<String, String> getScanProperties(TaskContext taskContext) throws TaskException {
		Map<String, String> properties = super.getScanProperties(taskContext);
		ConfigurationMap configurationMap = taskContext.getConfigurationMap();
		addEntryMap(properties, CoreConstants.TARGET, configurationMap.get(CoreConstants.TARGET));
		addEntryMap(properties, CFG_SEL_TEST_OPTIMIZE, configurationMap.get(CFG_SEL_TEST_OPTIMIZE));
		addEntryMap(properties, CFG_LOGIN_USER, configurationMap.get(CFG_LOGIN_USER));
		addEntryMap(properties, CFG_LOGIN_PASSWORD, configurationMap.get(CFG_LOGIN_PASSWORD));
		addEntryMap(properties, CFG_THIRD_CREDENTIAL, configurationMap.get(CFG_THIRD_CREDENTIAL));
		addEntryMap(properties, CFG_SEL_PRESENCE, configurationMap.get(CFG_SEL_PRESENCE));
		String scanFile = configurationMap.get(CFG_SCAN_FILE);
		addEntryMap(properties, CFG_SCAN_FILE, scanFile);
		String scanType = (scanFile != null && !scanFile.trim().isEmpty()) ? SCAN_OPTION_CUSTOM : configurationMap.get(CFG_SEL_SCAN_TYPE);
		addEntryMap(properties, CFG_SEL_SCAN_TYPE, scanType);
		return properties;
	}

	@Override
	public File initWorkingDir(TaskContext taskContext) throws TaskException {
		File workingDir = taskContext.getWorkingDirectory();
		File dirToScan = new File(workingDir, SA_DIR);

		if (dirToScan.exists())
			FileUtils.deleteDir(dirToScan);

		try {
			dirToScan.mkdirs();
		} catch (Exception e) {
			logger.error("err.working.dir.creation", e.getLocalizedMessage());
		}

		return dirToScan;
	}
}
