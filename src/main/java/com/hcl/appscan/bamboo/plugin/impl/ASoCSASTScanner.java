/**
 * (c) Copyright HCL Technologies Ltd. 2020.
 * LICENSE: Apache License, Version 2.0 https://www.apache.org/licenses/LICENSE-2.0
 */

package com.hcl.appscan.bamboo.plugin.impl;

import com.atlassian.bamboo.build.artifact.ArtifactHandlingUtils;
import com.atlassian.bamboo.build.artifact.ArtifactManager;
import com.atlassian.bamboo.plan.artifact.ArtifactDefinitionContext;
import com.atlassian.bamboo.process.ProcessService;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.core.util.FileUtils;
import com.hcl.appscan.bamboo.plugin.auth.BambooAuthenticationProvider;
import com.hcl.appscan.bamboo.plugin.util.ScanProgress;
import com.hcl.appscan.bamboo.plugin.util.Utility;
import com.hcl.appscan.sdk.CoreConstants;
import com.hcl.appscan.sdk.error.InvalidTargetException;
import com.hcl.appscan.sdk.error.ScannerException;
import com.hcl.appscan.sdk.logging.IProgress;
import com.hcl.appscan.sdk.results.NonCompliantIssuesResultProvider;
import com.hcl.appscan.sdk.scanners.sast.SASTConstants;
import com.hcl.appscan.sdk.scanners.sast.SASTScan;
import com.hcl.appscan.sdk.scanners.sast.SASTScanFactory;
import com.hcl.appscan.sdk.utils.SystemUtil;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class ASoCSASTScanner extends AbstractASoCScanner {
	private ProcessService processService;
	private BambooAuthenticationProvider authenticationProvider;
	private String jobId;

	public ASoCSASTScanner(LogHelper logger, ArtifactManager artifactManager, ProcessService processService) {
		super(logger, artifactManager);
		this.processService = processService;
	}

	@Override
	public String getScannerType() {
		return SASTConstants.SAST;
	}

	private void setInstallDir() {
		if (SystemUtil.isWindows() && System.getProperty("user.home").toLowerCase().indexOf("system32") >= 0) {
			System.setProperty(CoreConstants.SACLIENT_INSTALL_DIR, BAMBOO_APPSCAN_INSTALL_DIR.getPath());
		}
	}

	@Override
	public void scheduleScan(TaskContext taskContext) throws TaskException {
		logger.info("scan.schedule.static");
		IProgress progress = new ScanProgress(logger);
		authenticationProvider = new BambooAuthenticationProvider(credential);
		SASTScanFactory scanFactory = new SASTScanFactory();

		Map<String, String> scanProperties = getScanProperties(taskContext);
		SASTScan scan = (SASTScan) scanFactory.create(scanProperties, progress, authenticationProvider);
		try {
			setInstallDir();
			scan.run();
			jobId = scan.getScanId();

			// Publish generated IRX File to current Build
			publishArtifact(taskContext, logger.getText("irx.file"), workingDir, scan.getIrx().getName());
			logger.info("scan.schedule.success", jobId);

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
		String target = taskContext.getConfigurationMap().get(CUSTOM_TARGET);
		if (target == null || target.trim().isEmpty()) {
			Collection<ArtifactDefinitionContext> artifacts = taskContext.getBuildContext().getArtifactContext().getDefinitionContexts();
			try {
				for (ArtifactDefinitionContext artifact : artifacts) {
					FileSet fileSet = ArtifactHandlingUtils.createFileSet(taskContext.getWorkingDirectory(), artifact, true, null);
					Iterator<Resource> iterator = fileSet.iterator();
					while (iterator.hasNext()) {
						Resource resource = iterator.next();
						target = new File(workingDir, resource.getName()).getAbsolutePath();
						break;
					}
					if (target != null && !target.trim().isEmpty()) break;
				}
			} catch (IOException e) {
			}
		} else {
			addEntryMap(properties, SASTConstants.SAVE_LOCATION, workingDir.getAbsolutePath());
		}
		target = Utility.resolvePath(target, taskContext);
		if (!new File(target).exists()) throw new TaskException(logger.getText("err.custom.target.path"));

		addEntryMap(properties, CoreConstants.TARGET, target);
		if (taskContext.getConfigurationMap().getAsBoolean(OPEN_SOURCE_ONLY))
			addEntryMap(properties, CoreConstants.OPEN_SOURCE_ONLY, "");

		return properties;
	}

	@Override
	public File initWorkingDir(TaskContext taskContext) throws TaskException {
		File workingDir = taskContext.getWorkingDirectory();
		File dirToScan = new File(workingDir, SA_DIR);

		if (dirToScan.exists())
			FileUtils.deleteDir(dirToScan);

		dirToScan.mkdirs();

		Collection<ArtifactDefinitionContext> artifacts = taskContext.getBuildContext().getArtifactContext().getDefinitionContexts();

		if (artifacts.isEmpty())
			throw new TaskException(logger.getText("err.no.artifacts")); //$NON-NLS-1$

		try {
			for (ArtifactDefinitionContext artifact : artifacts) {
				logger.info("copy.artifact", artifact.getName(), dirToScan); //$NON-NLS-1$
				FileSet fileSet = ArtifactHandlingUtils.createFileSet(workingDir, artifact, true, null);
				ArtifactHandlingUtils.copyFileSet(fileSet, dirToScan);
			}
			return dirToScan;
		} catch (IOException e) {
			throw new TaskException(e.getLocalizedMessage(), e);
		}
	}
}
