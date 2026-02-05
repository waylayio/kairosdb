package org.kairosdb.core.oauth;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import org.mockito.stubbing.Answer;

public class OAuthFilterTest
{
	private static final String CONSUMER_KEY = "test-consumer";
	private static final String CONSUMER_SECRET = "test-secret";
	
	@Mock
	private ConsumerTokenStore mockTokenStore;
	
	@Mock
	private HttpServletRequest mockRequest;
	
	@Mock
	private HttpServletResponse mockResponse;
	
	@Mock
	private FilterChain mockFilterChain;
	
	private OAuthFilter filter;
	
	@Before
	public void setup()
	{
		openMocks(this);
		filter = new OAuthFilter(mockTokenStore);

		when(mockRequest.getRemoteAddr()).thenReturn("192.168.1.1");
		when(mockRequest.getMethod()).thenReturn("GET");
		when(mockRequest.getScheme()).thenReturn("http");
		when(mockRequest.getServerName()).thenReturn("localhost");
		when(mockRequest.getServerPort()).thenReturn(8080);
		when(mockRequest.getRequestURI()).thenReturn("/api/v1/datapoints");
		when(mockRequest.getParameterMap()).thenReturn(new HashMap<>());
	}
	
	@Test
	public void test_localRequest_skipsOAuth() throws Exception
	{
		when(mockRequest.getRemoteAddr()).thenReturn("127.0.0.1");
		
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
		verify(mockResponse, never()).sendError(anyInt(), anyString());
	}
	
	@Test
	public void test_missingAuthHeader_returns401() throws Exception
	{
		when(mockRequest.getHeader("Authorization")).thenReturn(null);
		
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockResponse).sendError(eq(401), anyString());
		verify(mockFilterChain, never()).doFilter(any(), any());
	}
	
	@Test
	public void test_invalidAuthHeader_returns401() throws Exception
	{
		when(mockRequest.getHeader("Authorization")).thenReturn("Basic abc123");
		
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockResponse).sendError(eq(401), anyString());
		verify(mockFilterChain, never()).doFilter(any(), any());
	}
	
	@Test
	public void test_unknownConsumer_returns401() throws Exception
	{
		String authHeader = buildOAuthHeader(CONSUMER_KEY, CONSUMER_SECRET);
		when(mockRequest.getHeader("Authorization")).thenReturn(authHeader);
		when(mockTokenStore.getToken(CONSUMER_KEY)).thenReturn(null);
		
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockResponse).sendError(eq(401), contains("Unknown consumer"));
		verify(mockFilterChain, never()).doFilter(any(), any());
	}
	
	@Test
	public void test_expiredTimestamp_returns401() throws Exception
	{
		// Use timestamp from 10 minutes ago
		long expiredTimestamp = (System.currentTimeMillis() / 1000) - 600;
		String authHeader = buildOAuthHeaderWithTimestamp(CONSUMER_KEY, CONSUMER_SECRET, expiredTimestamp);
		when(mockRequest.getHeader("Authorization")).thenReturn(authHeader);
		when(mockTokenStore.getToken(CONSUMER_KEY)).thenReturn(CONSUMER_SECRET);
		
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockResponse).sendError(eq(401), contains("expired"));
		verify(mockFilterChain, never()).doFilter(any(), any());
	}
	
	@Test
	public void test_validOAuthRequest_passesThrough() throws Exception
	{
		String authHeader = buildOAuthHeader(CONSUMER_KEY, CONSUMER_SECRET);
		when(mockRequest.getHeader("Authorization")).thenReturn(authHeader);
		when(mockTokenStore.getToken(CONSUMER_KEY)).thenReturn(CONSUMER_SECRET);
		
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockFilterChain).doFilter(mockRequest, mockResponse);
		verify(mockResponse, never()).sendError(anyInt(), anyString());
	}
	
	@Test
	public void test_invalidSignature_returns401() throws Exception
	{
		String authHeader = buildOAuthHeader(CONSUMER_KEY, "wrong-secret");
		when(mockRequest.getHeader("Authorization")).thenReturn(authHeader);
		when(mockTokenStore.getToken(CONSUMER_KEY)).thenReturn(CONSUMER_SECRET);
		
		filter.doFilter(mockRequest, mockResponse, mockFilterChain);
		
		verify(mockResponse).sendError(eq(401), contains("Invalid signature"));
		verify(mockFilterChain, never()).doFilter(any(), any());
	}
	
	private String buildOAuthHeader(String consumerKey, String consumerSecret) throws Exception
	{
		long timestamp = System.currentTimeMillis() / 1000;
		return buildOAuthHeaderWithTimestamp(consumerKey, consumerSecret, timestamp, "fixednonce12345");
	}
	
	private String buildOAuthHeaderWithTimestamp(String consumerKey, String consumerSecret, long timestamp) throws Exception
	{
		return buildOAuthHeaderWithTimestamp(consumerKey, consumerSecret, timestamp, "fixednonce12345");
	}
	
	private String buildOAuthHeaderWithTimestamp(String consumerKey, String consumerSecret, long timestamp, String nonce) throws Exception
	{

		TreeMap<String, String> params = new TreeMap<>();
		params.put("oauth_consumer_key", consumerKey);
		params.put("oauth_signature_method", "HMAC-SHA1");
		params.put("oauth_timestamp", String.valueOf(timestamp));
		params.put("oauth_nonce", nonce);
		params.put("oauth_version", "1.0");

		String method = "GET";
		String baseUrl = "http://localhost:8080/api/v1/datapoints";
		String normalizedParams = normalizeParams(params);
		String baseString = method + "&" + percentEncode(baseUrl) + "&" + percentEncode(normalizedParams);

		String signingKey = percentEncode(consumerSecret) + "&";
		String signature = calculateHmacSha1(baseString, signingKey);

		StringBuilder header = new StringBuilder("OAuth ");
		boolean first = true;
		for (Map.Entry<String, String> entry : params.entrySet())
		{
			if (!first) header.append(", ");
			first = false;
			header.append(entry.getKey()).append("=\"").append(percentEncode(entry.getValue())).append("\"");
		}
		header.append(", oauth_signature=\"").append(percentEncode(signature)).append("\"");
		
		return header.toString();
	}
	
	private String normalizeParams(TreeMap<String, String> params) throws UnsupportedEncodingException
	{
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, String> entry : params.entrySet())
		{
			if (!first) sb.append("&");
			first = false;
			sb.append(percentEncode(entry.getKey())).append("=").append(percentEncode(entry.getValue()));
		}
		return sb.toString();
	}
	
	private String calculateHmacSha1(String data, String key) throws Exception
	{
		SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(signingKey);
		byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(rawHmac);
	}
	
	private String percentEncode(String value) throws UnsupportedEncodingException
	{
		if (value == null) return "";
		return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
				.replace("+", "%20")
				.replace("*", "%2A")
				.replace("%7E", "~");
	}
}
