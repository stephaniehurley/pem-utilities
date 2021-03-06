/**
 * Copyright 2019 Syncsort Inc. All Rights Reserved.
 * 
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.pem.utilities.sfg2pem.imp;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.pem.utilities.Configuration;
import com.ibm.pem.utilities.sfg2pem.ApiInvocationException;
import com.ibm.pem.utilities.sfg2pem.Constants;
import com.ibm.pem.utilities.sfg2pem.ImportException;
import com.ibm.pem.utilities.sfg2pem.ValidationException;
import com.ibm.pem.utilities.sfg2pem.imp.plugins.resources.SftpResourceHelper;
import com.ibm.pem.utilities.util.ApiResponse;

public class ImportHelper {

	private static final Logger LOG = LoggerFactory.getLogger(ImportHelper.class);

	private static final String LOGMSG_RUNNING_API_GET = "Running API: GET %s";

	static ApiResponse getPEMPartner(Configuration config, String partnerKey) throws ApiInvocationException {
		String url = config.getPrRestURL() + "partners/" + partnerKey + "/";
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.HEADER_ACCEPT, Constants.MEDIA_TYPE_APPLICATION_XML);

		if (LOG.isInfoEnabled()) {
			LOG.info(String.format(LOGMSG_RUNNING_API_GET, url));
		}
		ApiResponse apiOutput;
		try {
			apiOutput = config.getResourceFactory().createHttpClientInstance().doGet(url, headers, config.getUserName(),
					config.getPassword(), config, config.getPrHostName());
		} catch (KeyManagementException | NoSuchAlgorithmException | CertificateException | KeyStoreException
				| HttpException | IOException | URISyntaxException | ValidationException e) {
			throw new ApiInvocationException(e);
		}
		return apiOutput;
	}

	static ApiResponse getProdSFGPartner(Configuration config, String sfgPartnerKey) throws ApiInvocationException {
		String url = config.getSfgProdRestURL() + Constants.SFG_PARTNER_REST_URI + sfgPartnerKey;
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.HEADER_ACCEPT, Constants.MEDIA_TYPE_APPLICATION_XML);

		if (LOG.isInfoEnabled()) {
			LOG.info(String.format(LOGMSG_RUNNING_API_GET, url));
		}
		ApiResponse apiOutput;
		try {
			apiOutput = config.getResourceFactory().createHttpClientInstance().doGet(url, headers,
					config.getSfgProdUserName(), config.getSfgProdPassword(), config, config.getSfgProdHost());
		} catch (KeyManagementException | NoSuchAlgorithmException | CertificateException | KeyStoreException
				| HttpException | IOException | URISyntaxException | ValidationException e) {
			throw new ApiInvocationException(e);
		}
		return apiOutput;
	}

	// TODO merge this method with the one for prod-sfg.
	public static ApiResponse getTestSFGPartner(Configuration config, String sfgPartnerKey)
			throws ApiInvocationException {
		String url = config.getSfgTestRestURL() + Constants.SFG_PARTNER_REST_URI + sfgPartnerKey;
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.HEADER_ACCEPT, Constants.MEDIA_TYPE_APPLICATION_XML);
		if (LOG.isInfoEnabled()) {
			LOG.info(String.format(LOGMSG_RUNNING_API_GET, url));
		}
		ApiResponse apiOutput;
		try {
			apiOutput = config.getResourceFactory().createHttpClientInstance().doGet(url, headers,
					config.getSfgTestUserName(), config.getSfgTestPassword(), config, config.getSfgTestHost());
		} catch (KeyManagementException | NoSuchAlgorithmException | CertificateException | KeyStoreException
				| HttpException | IOException | URISyntaxException | ValidationException e) {
			throw new ApiInvocationException(e);
		}
		return apiOutput;
	}

	public static Document buildDomDoc(String text) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new StringReader(text)));
		return doc;
	}

	public static void printApiResponse(ApiResponse apiOutput) {
		if (LOG.isInfoEnabled()) {
			LOG.info("API Status Code: " + apiOutput.getStatusCode() + ", Status Line: " + apiOutput.getStatusLine());
			LOG.info("API Response: " + apiOutput.getResponse());
		}
	}

	public static ApiResponse getResourceFromSFG(boolean getFromProductionSfg, Configuration config, String resourceUri,
			String resourceKey) throws ApiInvocationException, ImportException {
		String url = getFromProductionSfg ? config.getSfgProdRestURL() : config.getSfgTestRestURL();
		url += resourceUri + resourceKey;
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.HEADER_ACCEPT, Constants.MEDIA_TYPE_APPLICATION_XML);
		try {
			String userName = getFromProductionSfg ? config.getSfgProdUserName() : config.getSfgTestUserName();
			char[] password = getFromProductionSfg ? config.getSfgProdPassword() : config.getSfgTestPassword();
			String host = getFromProductionSfg ? config.getSfgProdHost() : config.getSfgTestHost();
			if (LOG.isInfoEnabled()) {
				LOG.info(String.format(LOGMSG_RUNNING_API_GET, url));
			}
			ApiResponse apiOutput = config.getResourceFactory().createHttpClientInstance().doGet(url, headers, userName,
					password, config, host);
			if (LOG.isInfoEnabled()) {
				LOG.info("Response:\n" + apiOutput.getStatusCode() + apiOutput.getResponse());
			}
			if (!apiOutput.getStatusCode().equals("200")) {
				String message = apiOutput.getResponse();
				boolean isResponsePresent = true;
				if (message == null || message.isEmpty()) {
					isResponsePresent = false;
					message = apiOutput.getStatusLine();
				}
				throw new ImportException(SftpResourceHelper.getErrorDescription(message, isResponsePresent));
			}
			return apiOutput;
		} catch (KeyManagementException | NoSuchAlgorithmException | CertificateException | KeyStoreException
				| HttpException | IOException | URISyntaxException | ParserConfigurationException | SAXException
				| ValidationException e) {
			throw new ApiInvocationException(e);
		}
	}

	public static List<ApiResponse> getResourceListFromSFG(boolean getFromProductionSfg, Configuration config,
			String resourceUri) throws ApiInvocationException, ImportException {
		List<ApiResponse> apiResponses = new ArrayList<>();

		String url = getFromProductionSfg ? config.getSfgProdRestURL() : config.getSfgTestRestURL();
		url += resourceUri;
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(Constants.HEADER_ACCEPT, Constants.MEDIA_TYPE_APPLICATION_JSON);
		try {
			String userName = getFromProductionSfg ? config.getSfgProdUserName() : config.getSfgTestUserName();
			char[] password = getFromProductionSfg ? config.getSfgProdPassword() : config.getSfgTestPassword();
			String host = getFromProductionSfg ? config.getSfgProdHost() : config.getSfgTestHost();
			if (LOG.isInfoEnabled()) {
				LOG.info(String.format(LOGMSG_RUNNING_API_GET, url));
			}
			ApiResponse apiOutput = config.getResourceFactory().createHttpClientInstance().doGet(url, headers, userName,
					password, config, host);
			if (LOG.isInfoEnabled()) {
				LOG.info("Response: " + apiOutput.getStatusCode() + "\n" + apiOutput.getResponse());
			}
			if (!apiOutput.getStatusCode().equals("200")) {
				String message = apiOutput.getResponse();
				boolean isResponsePresent = true;
				if (message == null || message.isEmpty()) {
					isResponsePresent = false;
					message = apiOutput.getStatusLine();
				}
				try {
					message = SftpResourceHelper.getErrorDescription(message, isResponsePresent);
				} catch (Exception e) {
					if (LOG.isErrorEnabled()) {
						LOG.error("", e);
					}
				}
				throw new ImportException(message);
			}
			apiResponses.add(apiOutput);

			int totalCount = getTotalCountOfRecords(apiOutput.getResponseHeaders());
			int countCalls = 1;
			int maxResultsPerCall = 1000;
			int remainingRecords = totalCount - maxResultsPerCall;
			String url2 = url;
			while (remainingRecords > 0) {
				String rangeStr = "_range="
						+ ((countCalls * maxResultsPerCall) + "-" + ((countCalls + 1) * maxResultsPerCall - 1));
				if (!url.contains("?")) {
					url2 = url + "?" + rangeStr;
				} else {
					url2 = url + "&" + rangeStr;
				}
				if (LOG.isInfoEnabled()) {
					LOG.info("Calling " + url2);
					apiOutput = config.getResourceFactory().createHttpClientInstance().doGet(url2, headers, userName,
							password, config, host);
				}
				if (LOG.isInfoEnabled()) {
					LOG.info("Done Calling " + url2);
				}
				if (!apiOutput.getStatusCode().equals("200")) {
					String message = apiOutput.getResponse();
					boolean isResponsePresent = true;
					if (message == null || message.isEmpty()) {
						isResponsePresent = false;
						message = apiOutput.getStatusLine();
					}
					try {
						message = SftpResourceHelper.getErrorDescription(message, isResponsePresent);
					} catch (Exception e) {
						if (LOG.isErrorEnabled()) {
							LOG.error("", e);
						}
					}
					throw new ImportException(message);
				}
				apiResponses.add(apiOutput);
				remainingRecords = remainingRecords - maxResultsPerCall;
				countCalls++;
			}
			return apiResponses;
		} catch (KeyManagementException | NoSuchAlgorithmException | CertificateException | KeyStoreException
				| HttpException | IOException | URISyntaxException | ValidationException e) {
			throw new ApiInvocationException(e);
		}
	}

	private static int getTotalCountOfRecords(Header[] responseHeaders) {
		int totalCount = 0;
		if (responseHeaders != null) {
			for (Header header : responseHeaders) {
				if (header.getName().equals("content-range")) {
					String headerValue = header.getValue();
					String totalCountStr = headerValue.substring(headerValue.indexOf('/') + 1);
					if (totalCountStr != null) {
						totalCount = Integer.valueOf(totalCountStr);
					}
				}
			}
		}
		return totalCount;
	}

}
