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

    public void apply(JobPosting job, ResumeProfile resume, String email, String password, boolean headless) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
            Page page = browser.newPage();

            page.navigate("https://www.linkedin.com/login");
            page.locator("#username").fill(email);
            page.locator("#password").fill(password);
            page.locator("button[type='submit']").click();

            page.navigate(job.getLinkedinJobUrl());
            page.locator("button.jobs-apply-button").first().click();

            page.getByLabel("Full name").first().fill(resume.getCandidateName());
            page.getByLabel("Work experience").first().fill(String.valueOf(resume.getYearsExperience()));
            page.getByLabel("Summary").first().fill(resume.getSummary());
            page.locator("button[aria-label='Submit application']").first().click();

            browser.close();
        }
    }
}
