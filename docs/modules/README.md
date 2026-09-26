# Modules

Core is always installed. Every other module is one backend starter plus one frontend package, and an app adds it
by listing both. [Build your app](../guides/build-your-app.md) shows how.

| Module | What it adds | Backend | Frontend |
|---|---|---|---|
| [core](core.md) | objects, fields, records, relationships, identity, audit, admin | `chawpi-spring-boot-starter` | `@hneyra/core`, `@hneyra/ui` |
| [views](views.md) | saved list configurations | `chawpi-spring-boot-starter-views` | `@hneyra/views` |
| [forms](forms.md) | field arrangements in sections | `chawpi-spring-boot-starter-forms` | `@hneyra/forms` |
| [pages](pages.md) | metadata-defined pages and their builder | `chawpi-spring-boot-starter-pages` | `@hneyra/pages` |
| [workflow](workflow.md) | states and transitions on records | `chawpi-spring-boot-starter-workflow` | `@hneyra/workflow` |
| [automation](automation.md) | rules that react to record changes | `chawpi-spring-boot-starter-automation` | `@hneyra/automation` |
| [documents](documents.md) | document templates, issuing, printing | `chawpi-spring-boot-starter-documents` | `@hneyra/documents` |
| [gis](gis.md) | geometry fields, maps, GeoServer layers | `chawpi-spring-boot-starter-gis` | `@hneyra/gis` |
| [agent](agent.md) | the AI assistant | `chawpi-spring-boot-starter-agent` | `@hneyra/agent` |
| [testing](testing.md) | test fixtures for your app | `chawpi-test` | `@hneyra/testing` |
