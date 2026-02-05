package org.kairosdb.core.oauth;

import com.google.inject.Inject;
import org.kairosdb.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * OAuth 1.0 Filter for authenticating API requests.
 * Implements HMAC-SHA1 signature verification per RFC 5849.
 */
public class OAuthFilter implements Filter
{
	public static final Logger logger = LoggerFactory.getLogger(OAuthFilter.class);
	private static final String HMAC_SHA1 = "HmacSHA1";

	private ConsumerTokenStore m_tokenStore;

	@Inject
	public OAuthFilter(ConsumerTokenStore tokenStore)
	{
		m_tokenStore = tokenStore;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException
	{
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
	{
		HttpServletRequest httpRequest = (HttpServletRequest)servletRequest;
		HttpServletResponse httpResponse = (HttpServletResponse)servletResponse;

		//Skip oauth for local connections
		if (!"127.0.0.1".equals(servletRequest.getRemoteAddr()))
		{
			// Get OAuth parameters from request header
			String authHeader = httpRequest.getHeader("Authorization");
			
			if (authHeader == null || !authHeader.startsWith("OAuth "))
			{
				logger.warn("Missing OAuth headers");
				httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing OAuth headers");
				return;
			}

			// Parse OAuth parameters from Authorization header
			Map<String, String> oauthParams = parseOAuthHeader(authHeader);
			String consumerKey = oauthParams.get("oauth_consumer_key");
			String timestampStr = oauthParams.get("oauth_timestamp");
			String signature = oauthParams.get("oauth_signature");
			String signatureMethod = oauthParams.get("oauth_signature_method");
			String nonce = oauthParams.get("oauth_nonce");

			if (consumerKey == null || timestampStr == null || signature == null)
			{
				logger.warn("Missing required OAuth parameters");
				httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing OAuth parameters");
				return;
			}

			// Validate signature method
			if (signatureMethod != null && !signatureMethod.equals("HMAC-SHA1"))
			{
				logger.warn("Unsupported signature method: " + signatureMethod);
				httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unsupported signature method");
				return;
			}

			// Get the consumer secret
			String consumerSecret = m_tokenStore.getToken(consumerKey);
			if (consumerSecret == null)
			{
				logger.warn("Unknown consumer key: " + consumerKey);
				httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unknown consumer");
				return;
			}

			// Check that the timestamp has not expired
			long msgTime = Util.parseLong(timestampStr) * 1000L; //Message time is in seconds
			long currentTime = System.currentTimeMillis();

			//if the message is older than 5 min it is no good
			if (Math.abs(msgTime - currentTime) > 300000)
			{
				logger.warn("OAuth message time out, msg time: "+msgTime+" current time: "+currentTime);
				httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Message expired");
				return;
			}

			// Verify the OAuth signature
			try
			{
				if (!verifySignature(httpRequest, oauthParams, consumerSecret, signature))
				{
					logger.warn("OAuth signature verification failed");
					httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
					return;
				}
			}
			catch (Exception e)
			{
				logger.error("Error verifying OAuth signature", e);
				httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Signature verification error");
				return;
			}
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	private boolean verifySignature(HttpServletRequest request, Map<String, String> oauthParams, 
			String consumerSecret, String providedSignature) 
			throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException
	{
		String baseString = buildBaseString(request, oauthParams);
		
		// Create the signing key (consumer_secret&token_secret)
		// For OAuth 1.0 without tokens, token_secret is empty
		String signingKey = percentEncode(consumerSecret) + "&";

		String expectedSignature = calculateHmacSha1(baseString, signingKey);
		
		// The signature is already decoded in parseOAuthHeader, so compare directly
		boolean valid = expectedSignature.equals(providedSignature);
		
		if (!valid)
		{
			logger.warn("Signature mismatch. Expected: {}, Provided: {}", expectedSignature, providedSignature);
			logger.debug("Base string: {}", baseString);
		}
		
		return valid;
	}

	private String buildBaseString(HttpServletRequest request, Map<String, String> oauthParams) 
			throws UnsupportedEncodingException
	{
		String method = request.getMethod().toUpperCase();
		
		// Base URL (scheme://host:port/path, without query string)
		String baseUrl = buildBaseUrl(request);
		
		// Normalized parameters (sorted, encoded)
		String normalizedParams = normalizeParameters(request, oauthParams);

		return method + "&" + percentEncode(baseUrl) + "&" + percentEncode(normalizedParams);
	}

	private String buildBaseUrl(HttpServletRequest request)
	{
		StringBuilder url = new StringBuilder();
		String scheme = request.getScheme().toLowerCase();
		int port = request.getServerPort();
		
		url.append(scheme);
		url.append("://");
		url.append(request.getServerName().toLowerCase());
		
		// Only include port if non-standard
		if ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443))
		{
			url.append(":");
			url.append(port);
		}
		
		url.append(request.getRequestURI());
		
		return url.toString();
	}

	private String normalizeParameters(HttpServletRequest request, Map<String, String> oauthParams) 
			throws UnsupportedEncodingException
	{
		// Collect all parameters (OAuth + request params, excluding oauth_signature)
		TreeMap<String, String> params = new TreeMap<>();
		
		// Add OAuth parameters (except signature)
		for (Map.Entry<String, String> entry : oauthParams.entrySet())
		{
			if (!"oauth_signature".equals(entry.getKey()))
			{
				params.put(entry.getKey(), entry.getValue());
			}
		}
		
		// Add request parameters
		Map<String, String[]> requestParams = request.getParameterMap();
		for (Map.Entry<String, String[]> entry : requestParams.entrySet())
		{
			for (String value : entry.getValue())
			{
				params.put(entry.getKey(), value);
			}
		}

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, String> entry : params.entrySet())
		{
			if (!first)
			{
				sb.append("&");
			}
			first = false;
			sb.append(percentEncode(entry.getKey()));
			sb.append("=");
			sb.append(percentEncode(entry.getValue()));
		}
		
		return sb.toString();
	}

	private String calculateHmacSha1(String data, String key) 
			throws NoSuchAlgorithmException, InvalidKeyException
	{
		SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA1);
		Mac mac = Mac.getInstance(HMAC_SHA1);
		mac.init(signingKey);
		byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(rawHmac);
	}

	private String percentEncode(String value) throws UnsupportedEncodingException
	{
		if (value == null)
		{
			return "";
		}
		return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
				.replace("+", "%20")
				.replace("*", "%2A")
				.replace("%7E", "~");
	}

	private Map<String, String> parseOAuthHeader(String authHeader)
	{
		Map<String, String> params = new HashMap<>();
		
		// Remove "OAuth " prefix
		String paramString = authHeader.substring(6);
		
		// Parse key="value" pairs
		String[] pairs = paramString.split(",\\s*");
		for (String pair : pairs)
		{
			int eqIdx = pair.indexOf('=');
			if (eqIdx > 0)
			{
				String key = pair.substring(0, eqIdx).trim();
				String value = pair.substring(eqIdx + 1).trim();
				// Remove quotes
				if (value.startsWith("\"") && value.endsWith("\""))
				{
					value = value.substring(1, value.length() - 1);
				}
				// URL decode the value
				try
				{
					value = URLDecoder.decode(value, StandardCharsets.UTF_8.name());
				}
				catch (Exception e)
				{
					// Keep original value if decoding fails
				}
				params.put(key, value);
			}
		}
		
		return params;
	}

	@Override
	public void destroy()
	{
	}
}
