package org.networkedassets.atlassian.stash.personalstash.web;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.networkedassets.atlassian.stash.personalstash.license.LicenseManager;
import org.networkedassets.atlassian.stash.personalstash.repositories.RepositoriesPreScanningScheduler;
import org.networkedassets.atlassian.stash.personalstash.state.PluginState;
import org.networkedassets.atlassian.stash.personalstash.state.PluginStateManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.atlassian.stash.exception.AuthorisationException;
import com.atlassian.stash.nav.NavBuilder;
import com.atlassian.stash.user.Permission;
import com.atlassian.stash.user.PermissionValidationService;

public class AdministrationPanelServlet extends SoyTemplateServlet {

	private static final long serialVersionUID = 790189345704146559L;

	private String templateResources = "org.networkedassets.atlassian.stash.personalstash:templates-soy";
	private String templateKey = "org.networkedassets.personalstash.adminPage";

	private String licenseErrorTemplateResources = "org.networkedassets.atlassian.stash.personalstash:license-servlet-resources";
	private String licenseErrorTemplateKey = "org.networkedassets.personalstash.license.errorPage";

	@Autowired
	private LicenseManager licenseManager;
	@Autowired
	private NavBuilder navBuilder;
	@Autowired
	private PluginStateManager pluginStateManager;
	@Autowired
	private RepositoriesPreScanningScheduler repositoriesPreScanningScheduler;
	@Autowired
	private PermissionValidationService permissionValidationService;

	@Override
	protected String getTemplateResources() {
		return templateResources;
	}

	@Override
	protected String getTemplateKey() {
		return templateKey;
	}

	@Override
	protected HashMap<String, Object> getTemplateParams() {
		HashMap<String, Object> params = super.getTemplateParams();
		String baseApiPath = navBuilder.buildAbsolute()
				+ "/rest/personalstash/1.0";
		params.put("baseApiPath", baseApiPath);
		return params;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			permissionValidationService.validateForGlobal(Permission.ADMIN);
			if (licenseManager.isLicenseValid()) {
				if (pluginStateManager.getState() == PluginState.SCAN_NEEDED) {
					repositoriesPreScanningScheduler.scheduleScan();
				}
			}
			super.doGet(req, resp);
		} catch (AuthorisationException e) {
			resp.sendRedirect(navBuilder.login().buildAbsolute());
		}
	}

	@Override
	protected void render(HttpServletResponse resp, String templateResources,
			String templateKey, HashMap<String, Object> templateParams)
			throws IOException, ServletException {
		if (licenseManager.isLicenseValid()) {
			super.render(resp, getTemplateResources(), getTemplateKey(),
					getTemplateParams());
			return;
		}

		renderLicenseError(resp);
	}

	private void renderLicenseError(HttpServletResponse resp)
			throws IOException, ServletException {
		HashMap<String, Object> params = super.getTemplateParams();
		params.put("status", licenseManager.getLicenseStatus());
		super.render(resp, licenseErrorTemplateResources,
				licenseErrorTemplateKey, params);
	}
}