// Needs to be higher than the default Playwright timeout
jest.setTimeout(40 * 1000)

describe("Example.com", () => {
  it("Could go to the catalog page", async () => {
    await page.goto("http://localhost:9090/");
    // Click text=Sign in
    await page.click('text=Sign in');
    // Click input
    await page.click('input');
    // Fill input
    await page.fill('input', 'admin');
    // Click [placeholder="Enter password"]
    await page.click('[placeholder="Enter password"]');
    // Fill [placeholder="Enter password"]
    await page.fill('[placeholder="Enter password"]', process.env.EMX2_ADMIN_PW);
    // Click div[role="document"] button:has-text("Sign in")
    await page.click('div[role="document"] button:has-text("Sign in")');
    
    await expect(page).toEqualText('h2', 'Data collections and data users')
  })

  it("Could go to the catalog page", async () => {
    await page.goto("http://localhost:9090/#/lifecycle");
    
    await expect(page).toEqualText('h1', 'Cohort catalogue')
  })

})