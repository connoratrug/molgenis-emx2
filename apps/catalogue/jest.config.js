module.exports = {
    preset: 'jest-playwright-preset',
    testEnvironmentOptions: {
      'jest-playwright': {
        'launchOptions': {
            'headless': true
        }
      },
    },
  }