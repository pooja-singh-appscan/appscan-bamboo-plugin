/**
 * (c) Copyright HCL Technologies Ltd. 2020.
 * LICENSE: Apache License, Version 2.0 https://www.apache.org/licenses/LICENSE-2.0
 */

package com.hcl.appscan.bamboo.plugin.util;

import com.atlassian.bamboo.credentials.CredentialsData;
import com.atlassian.bamboo.task.TaskContext;
import com.hcl.appscan.bamboo.plugin.impl.IScannerConstants;
import com.hcl.appscan.sdk.scanners.dynamic.DASTConstants;
import com.hcl.appscan.sdk.scanners.sast.SASTConstants;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Utility {
	public static Map<String, String> getTestTypes() {
		Map<String, String> scanTypes = new HashMap<String, String>();
		scanTypes.put(SASTConstants.STATIC_ANALYZER, SASTConstants.SAST);
		scanTypes.put(DASTConstants.DYNAMIC_ANALYZER, DASTConstants.DAST);
		return scanTypes;
	}

	public static Map<String, String> getScanTypes() {
		Map<String, String> scanOptions = new HashMap<String, String>();
		scanOptions.put(IScannerConstants.SCAN_OPTION_STAGING, IScannerConstants.SCAN_OPTION_STAGING);
		scanOptions.put(IScannerConstants.SCAN_OPTION_PRODUCTION, IScannerConstants.SCAN_OPTION_PRODUCTION);
		return scanOptions;
	}

	public static Map<String, String> getTestOptimizations() {
		Map<String, String> testOptimizations = new LinkedHashMap<String, String>();
		testOptimizations.put(IScannerConstants.OPTIMIZATION_FAST, IScannerConstants.OPTIMIZATION_FAST);
		testOptimizations.put(IScannerConstants.OPTIMIZATION_FASTER, IScannerConstants.OPTIMIZATION_FASTER);
		testOptimizations.put(IScannerConstants.OPTIMIZATION_FASTEST, IScannerConstants.OPTIMIZATION_FASTEST);
		testOptimizations.put(IScannerConstants.NO_OPTIMIZATION, IScannerConstants.NO_OPTIMIZATION);
		return testOptimizations;
	}

	public static Map<String, String> getFailBuildTypes() {
		Map<String, String> failBuildTypes = new LinkedHashMap<String, String>();
		failBuildTypes.put(IScannerConstants.FAIL_NON_COMPLIANCE, "Fail build for non-compliance with application policies");
		failBuildTypes.put(IScannerConstants.FAIL_SEVERITY_LEVEL, "Fail build on Severity");
		return failBuildTypes;
	}

	public static String resolvePath(String path, TaskContext taskContext) {
		if (path != null && !path.trim().isEmpty() && !(new File(path).isAbsolute()) && taskContext != null
				&& taskContext.getWorkingDirectory().exists()) {
			return new File(taskContext.getWorkingDirectory(), path).getAbsolutePath();
		}
		return path;
	}

	public static String getUserName(CredentialsData credential) {
		return (credential != null ? credential.getConfiguration().get("username") : null); //$NON-NLS-1$
	}

	public static String getPlainTextPassword(CredentialsData credential) {
		if (credential == null) return null;
		String password = credential.getConfiguration().get("password"); //$NON-NLS-1$
		try {
			password = Decrypt.decrypt(password);
		} catch (Exception e) {
			// Ignore any Exception Occurred
		}
		return password;
	}
}
