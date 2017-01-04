package com.jivesoftware.os.upena.deployable.okta;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jonathan.colt on 11/14/16.
 */
public class OktaFormAuthenticationFilter extends FormAuthenticationFilter {

    private static final Logger log = LoggerFactory.getLogger(FormAuthenticationFilter.class);

    protected boolean executeLogin(ServletRequest request, ServletResponse response) throws Exception {

        Cookie[] cookies = WebUtils.toHttp(request).getCookies();
        String mfaUserName = null;
        String mfaToken = null;
        String mfaPassCode = WebUtils.getCleanParam(request, "passCode");
        String mfaRelay = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("mfaUserName")) {
                    mfaUserName = cookie.getValue();
                }
                if (cookie.getName().equals("mfaToken")) {
                    mfaToken = cookie.getValue();
                }
                if (cookie.getName().equals("mfaRelay")) {
                    mfaRelay =cookie.getValue();
                }
            }
        }

        AuthenticationToken token = new OktaUsernamePasswordToken(
            mfaPassCode,
            mfaToken,
            mfaRelay,
            mfaUserName != null ? mfaUserName : getUsername(request),
            getPassword(request),
            isRememberMe(request),
            getHost(request));


        if (token == null) {
            String msg = "createToken method implementation returned null. A valid non-null AuthenticationToken " +
                "must be created in order to execute a login attempt.";
            throw new IllegalStateException(msg);
        }
        try {
            Subject subject = getSubject(request, response);
            subject.login(token);
            subject.getSession().stop();
            subject.getSession(true);
            return onLoginSuccess(token, subject, request, response);
        } catch (AuthenticationException e) {
            return onLoginFailure(token, e, request, response);
        }
    }

    protected void issueSuccessRedirect(ServletRequest request, ServletResponse response) throws Exception {
        WebUtils.issueRedirect(request, response, getSuccessUrl(), null, false);
    }

    protected boolean onLoginFailure(AuthenticationToken token,
        AuthenticationException e,
        ServletRequest request,
        ServletResponse response) {

        if (log.isDebugEnabled()) {
            log.debug( "Authentication exception", e );
        }
        if (e instanceof OktaMFARequiredException) {
            OktaMFARequiredException oe = (OktaMFARequiredException)e;
            HttpServletResponse httpServletResponse = WebUtils.toHttp(response);
            httpServletResponse.setStatus(303);
            httpServletResponse.setHeader("Location", httpServletResponse.encodeRedirectURL("/ui/auth/okta/mfa"));

            httpServletResponse.addCookie(mfaCookie("mfaToken", oe.getToken()));
            httpServletResponse.addCookie(mfaCookie("mfaRelay", oe.getRelay()));
            httpServletResponse.addCookie(mfaCookie("mfaUserName", oe.getUserName()));

            return false;
        } else {
            setFailureAttribute(request, e);
        }
        return true;
    }

    private Cookie mfaCookie(String key, String value) {
        Cookie mfaC = new Cookie(key, value);
        mfaC.setPath("/ui/auth/okta/mfa");
        mfaC.setMaxAge(300);
        mfaC.setVersion(1);
        return mfaC;
    }
}
