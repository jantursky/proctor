# Security policy

## Supported versions

Only the latest release of `io.github.jantursky:proctor` gets fixes.

## Reporting a vulnerability

Please don't open a public issue. Use GitHub's [private vulnerability reporting](https://github.com/jantursky/proctor/security/advisories/new), or email jan.tursky@gmail.com with a description and, if you can, a minimal reproduction.

You'll get an answer within a week. Once a fix is released, the advisory is published with credit, unless you'd rather stay anonymous.

## Things to know

- Datasets, prompts and model outputs stay on your machine and device. Proctor makes no network calls of its own and collects nothing. Result files are written to the app's files directory on the device and to the folder you give the report task.
- The only way data leaves is a judge you configure yourself. `llmJudge` sends the input, the output and the reference answer to whatever `Judge` you pass in; if that is a cloud model, those strings go to that service. The same holds for an `EmbeddingProvider` that calls out.
- Result files contain your prompts and the model's replies in plain text. Treat them like the data they came from, and keep real user data out of eval datasets.
- The HTML report is one static page with no scripts and no network requests. Prompts and outputs are HTML-escaped before they are written into it.
- The Android backend needs no permission of its own. The ML Kit library declares the permission to bind AICore.
