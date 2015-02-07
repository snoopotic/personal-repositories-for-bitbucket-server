package org.networkedassets.atlassian.stash.personalstash.web;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.soy.renderer.SoyException;
import com.atlassian.soy.renderer.SoyTemplateRenderer;

public abstract class SoyTemplateServlet extends HttpServlet {

	private static final long serialVersionUID = -7773632602555052731L;

	@Autowired
	private SoyTemplateRenderer soyTemplateRenderer;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp,
			String templatesResource, String templateName,
			Map<String, Object> templateParams) throws ServletException,
			IOException {
		try {
			render(resp, templatesResource, templateName, templateParams);
		} catch (Exception e) {

		}
	}

	protected void render(HttpServletResponse resp, String templatesResource,
			String templateName, Map<String, Object> data) throws IOException,
			ServletException {
		resp.setContentType("text/html;charset=UTF-8");
		try {
			soyTemplateRenderer.render(resp.getWriter(), templatesResource,
					templateName, data);
		} catch (SoyException e) {
			Throwable cause = e.getCause();
			if (cause instanceof IOException) {
				throw (IOException) cause;
			}
			throw new ServletException(e);
		}
	}

}