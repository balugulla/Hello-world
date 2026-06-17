package org.gulla.service.gulla.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.gulla.service.gulla.config.EasyApplyConfigProperties;
import org.gulla.service.gulla.exception.AutomationException;
import org.gulla.service.gulla.model.JobPosting;
import org.gulla.service.gulla.model.ResumeProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LinkedInEasyApplyAutomationService {

    private static final Logger logger = LoggerFactory.getLogger(LinkedInEasyApplyAutomationService.class);
    private static final String FULL_NAME_LABEL = "Full name";
    private static final String WORK_EXPERIENCE_LABEL = "Work experience";
    private static final String SUMMARY_LABEL = "Summary";

    private final EasyApplyConfigProperties config;

    public LinkedInEasyApplyAutomationService(EasyApplyConfigProperties config) {
        this.config = config;
    }

    public void apply(JobPosting job, ResumeProfile resume, String email, String password, boolean headless) {
        logger.info("Starting LinkedIn automation for job: {}", job.getLinkedinJobUrl());
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless))) {
            Page page = browser.newPage();
            page.setDefaultTimeout(config.getAutomationTimeoutMs());

            login(page, email, password);
            openEasyApply(page, job.getLinkedinJobUrl());
            completeAndSubmit(page, resume);
            
            logger.info("LinkedIn automation completed successfully for job: {}", job.getLinkedinJobUrl());
        } catch (Exception ex) {
            logger.error("LinkedIn automation failed for job: {}", job.getLinkedinJobUrl(), ex);
            throw ex;
        }
    }

    private void login(Page page, String email, String password) {
        try {
            logger.debug("Attempting LinkedIn login");
            page.navigate("https://www.linkedin.com/login");
            page.locator("#username").waitFor();
            page.locator("#username").fill(email);
            page.locator("#password").fill("***"); // Never log actual password
            page.locator("button[type='submit']").click();
            logger.debug("Login completed");
        } catch (RuntimeException ex) {
            throw new AutomationException("login", "LinkedIn automation failed during login step.", ex);
        }
    }

    private void openEasyApply(Page page, String jobUrl) {
        try {
            logger.debug("Opening Easy Apply for job: {}", jobUrl);
            page.navigate(jobUrl);
            page.locator("button.jobs-apply-button").first().waitFor();
            page.locator("button.jobs-apply-button").first().click();
            logger.debug("Easy Apply modal opened");
        } catch (RuntimeException ex) {
            throw new AutomationException("open_easy_apply", "LinkedIn automation failed while opening the Easy Apply modal.", ex);
        }
    }

    private void completeAndSubmit(Page page, ResumeProfile resume) {
        try {
            logger.debug("Filling application form for candidate: {}", resume.getCandidateName());
            page.getByLabel(FULL_NAME_LABEL).first().waitFor();
            page.getByLabel(FULL_NAME_LABEL).first().fill(resume.getCandidateName());
            page.getByLabel(WORK_EXPERIENCE_LABEL).first().fill(String.valueOf(resume.getYearsExperience()));
            page.getByLabel(SUMMARY_LABEL).first().fill(resume.getSummary());
            page.locator("button[aria-label='Submit application']").first().waitFor();
            page.locator("button[aria-label='Submit application']").first().click();
            logger.debug("Application submitted successfully");
        } catch (RuntimeException ex) {
            throw new AutomationException("form_completion", "LinkedIn automation failed during form completion or submission.", ex);
        }
    }
}
