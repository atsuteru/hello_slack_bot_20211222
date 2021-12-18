# hello_slack_bot_20211222

## Steps to run a test using vscode

1.Create `Heroku Redis`

After adding the `Heroku Redis` to your` Heloku App`, check the `REDIS_URL` in the next step:

Execute on vscode Terminal:
`heroku config`

2.Create the following file:

`.vscode\settings.json`
```
{
    "java.configuration.updateBuildConfiguration": "automatic",
    "java.test.config": [
        {
            "name": "debugCinfiguration",
            "workingDirectory": "${workspaceFolder}",
            "env": {
                "TEST_URL": "http://localhost:58080/",
                "SLACK_URL": "http://localhost:58081/",
                "REDIS_URL": "{your REDIS_URL}",
                "BUSINESS_CARD_GEN_URL": "http://localhost:58082/"
            }
        }
    ]
}
```
