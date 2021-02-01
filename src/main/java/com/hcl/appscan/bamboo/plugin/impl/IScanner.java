/**
 * (c) Copyright HCL Technologies Ltd. 2020, 2021.
 * LICENSE: Apache License, Version 2.0 https://www.apache.org/licenses/LICENSE-2.0
 */

package com.hcl.appscan.bamboo.plugin.impl;

import com.atlassian.bamboo.credentials.CredentialsData;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.hcl.appscan.bamboo.plugin.auth.AppScanAPICredentials;
import com.hcl.appscan.sdk.error.InvalidTargetException;
import com.hcl.appscan.sdk.error.ScannerException;

import java.io.File;
import java.util.Map;

public interface IScanner extends IScannerConstants, IArtifactPublisher {
	public String SA_DIR = ".sa";
	public String SDK_VERSION_KEY = "atlassian.sdk.version";

	public void setCredential(AppScanAPICredentials credentials);

	public void setWorkingDir(File workingDir);

	public String getScannerType();

	public void scheduleScan(TaskContext taskContext) throws InvalidTargetException, ScannerException, ArtifactsUnavailableException;

	public File initWorkingDir(TaskContext taskContext) throws TaskException, ArtifactsUnavailableException;

	public void waitAndDownloadResult(TaskContext taskContext) throws TaskException, InterruptedException, TaskFailedException;

	public void waitForReady(TaskContext taskContext) throws TaskException, InterruptedException, TaskFailedException;

	public void downloadResult(TaskContext taskContext) throws TaskException;

	public void cleanUpWorkingDir(TaskContext taskContext) throws Exception;

	public Map<String, String> getFailSeverityLevelConfig(TaskContext taskContext);

	public long getTotalCount();

	public long getHighCount();

	public long getMediumCount();

	public long getLowCount();
}
