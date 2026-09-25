# Modules

Core is always installed. Every other module is one backend starter plus one frontend package, and an app adds it
by listing both. [Build your app](../guides/build-your-app.md) shows how.

| Module | What it adds | Backend | Frontend |
|---|---|---|---|
| [core](core.md) | objects, fields, records, relationships, identity, audit, admin | `chawpi-spring-boot-starter` | `@chawpi/core`, `@chawpi/ui` |
| [views](views.md) | saved list configurations | `chawpi-spring-boot-starter-views` | `@chawpi/views` |
| [forms](forms.md) | field arrangements in sections | `chawpi-spring-boot-starter-forms` | `@chawpi/forms` |
| [pages](pages.md) | metadata-defined pages and their builder | `chawpi-spring-boot-starter-pages` | `@chawpi/pages` |
| [workflow](workflow.md) | states and transitions on records | `chawpi-spring-boot-starter-workflow` | `@chawpi/workflow` |
| [automation](automation.md) | rules that react to record changes | `chawpi-spring-boot-starter-automation` | `@chawpi/automation` |
| [documents](documents.md) | document templates, issuing, printing | `chawpi-spring-boot-starter-documents` | `@chawpi/documents` |
| [gis](gis.md) | geometry fields, maps, GeoServer layers | `chawpi-spring-boot-starter-gis` | `@chawpi/gis` |
| [agent](agent.md) | the AI assistant | `chawpi-spring-boot-starter-agent` | `@chawpi/agent` |
| [testing](testing.md) | test fixtures for your app | `chawpi-test` | `@chawpi/testing` |
