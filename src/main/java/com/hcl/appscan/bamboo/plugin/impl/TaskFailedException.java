/**
 * (c) Copyright HCL Technologies Ltd. 2020.
 * LICENSE: Apache License, Version 2.0 https://www.apache.org/licenses/LICENSE-2.0
 */

package com.hcl.appscan.bamboo.plugin.impl;

public class TaskFailedException extends Exception {
	public TaskFailedException(String message) {
		super(message);
	}
}
