package org.gulla.service.gulla.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.gulla.service.gulla.model.JobPosting;
import org.gulla.service.gulla.model.ResumeProfile;
import org.springframework.stereotype.Service;

@Service
public class LinkedInEasyApplyAutomationService {

    private static final double DEFAULT_TIMEOUT_MS = 15_000;

    public void apply(JobPosting job, ResumeProfile resume, String email, String password, boolean headless) {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless))) {
            Page page = browser.newPage();
            page.setDefaultTimeout(DEFAULT_TIMEOUT_MS);

            login(page, email, password);
            openEasyApply(page, job.getLinkedinJobUrl());
            completeAndSubmit(page, resume);
        }
    }

    private void login(Page page, String email, String password) {
        try {
            page.navigate("https://www.linkedin.com/login");
            page.locator("#username").waitFor();
            page.locator("#username").fill(email);
            page.locator("#password").fill(password);
            page.locator("button[type='submit']").click();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("LinkedIn automation failed during login step.", ex);
        }
    }

    private void openEasyApply(Page page, String jobUrl) {
        try {
            page.navigate(jobUrl);
            page.locator("button.jobs-apply-button").first().waitFor();
            page.locator("button.jobs-apply-button").first().click();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("LinkedIn automation failed while opening the Easy Apply modal.", ex);
        }
    }

    private void completeAndSubmit(Page page, ResumeProfile resume) {
        try {
            page.getByLabel("Full name").first().waitFor();
            page.getByLabel("Full name").first().fill(resume.getCandidateName());
            page.getByLabel("Work experience").first().fill(String.valueOf(resume.getYearsExperience()));
            page.getByLabel("Summary").first().fill(resume.getSummary());
            page.locator("button[aria-label='Submit application']").first().waitFor();
            page.locator("button[aria-label='Submit application']").first().click();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("LinkedIn automation failed during form completion or submission.", ex);
        }
    }
}
