/**
 * (c) Copyright HCL Technologies Ltd. 2020, 2021.
 * LICENSE: Apache License, Version 2.0 https://www.apache.org/licenses/LICENSE-2.0
 */

package com.hcl.appscan.bamboo.plugin.impl;

import com.hcl.appscan.sdk.CoreConstants;
import com.hcl.appscan.sdk.results.IResultsProvider;

public class ResultsRetriever {
	private IResultsProvider provider;
	private String status;
	private String message;
	private int retryFailedCount;
	private int retryInterval;

	public ResultsRetriever(IResultsProvider provider) {
		this(provider, IScannerConstants.FAILED_RETRY_COUNT, IScannerConstants.MIN_RETRY_INTERVAL);
	}

	public ResultsRetriever(IResultsProvider provider, int retryFailedCount, int retryInterval) {
		this.provider = provider;
		this.status = "";
		this.message = "";
		this.retryInterval = Math.max(retryInterval, IScannerConstants.MIN_RETRY_INTERVAL);
		this.retryFailedCount = Math.max(retryFailedCount, IScannerConstants.FAILED_RETRY_COUNT);
	}

	public void setRetryFailedCount(int retryFailedCount) {
		this.retryFailedCount = retryFailedCount;
	}

	public void setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
	}

	public void waitForResults() throws InterruptedException {
		retryInterval = Math.max(retryInterval, IScannerConstants.MIN_RETRY_INTERVAL);
		retryFailedCount = Math.max(retryFailedCount, IScannerConstants.FAILED_RETRY_COUNT);

		int failedCount = 0;
		while (failedCount < retryFailedCount) {
			status = provider.getStatus();
			message = provider.getMessage();

			if (CoreConstants.FAILED.equalsIgnoreCase(status)) 
				break;
			else if (status == null) {
				failedCount++;
			} else if (provider.hasResults()) {
				return;
			} else failedCount = 0;

			try {
				Thread.sleep(retryInterval * 1000L);
			} catch (InterruptedException e) {
				throw e;
			}
		}

		status = CoreConstants.FAILED;
		message = com.hcl.appscan.sdk.Messages.getMessage(CoreConstants.ERROR_GETTING_DETAILS, " Consecutive failed retry count: " + failedCount);
	}

	public boolean hasFailed() {
		return (CoreConstants.FAILED.equalsIgnoreCase(status));
	}

	public String getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}
}
