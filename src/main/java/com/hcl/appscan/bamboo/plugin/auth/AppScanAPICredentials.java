package com.hcl.appscan.bamboo.plugin.auth;

public class AppScanAPICredentials {
	private String keyId;
	private String password;

	public AppScanAPICredentials(String keyId, String password) {
		this.keyId = keyId;
		this.password = password;
	}

	public String getKeyId() {
		return keyId;
	}

	public String getPassword() {
		return password;
	}

	@Override
	public String toString() {
		return "AppScanAPICredentials{" +
				"keyId='" + keyId + '\'' +
				'}';
	}
}
