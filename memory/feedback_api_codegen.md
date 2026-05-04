---
name: Never create custom API files
description: All RTK Query hooks must come from the auto-generated BrainFlexApi.ts, never from hand-written injectEndpoints files
type: feedback
---

Never create custom API files (e.g. profileImageApi.ts) that call emptySplitApi.injectEndpoints(). All API hooks are auto-generated from the OpenAPI spec into BrainFlexApi.ts.

**Why:** The user regenerates BrainFlexApi.ts from the backend OpenAPI schema. Hand-rolled API files duplicate and conflict with the generated code.

**How to apply:** If a new backend endpoint is needed, implement it in Java, restart the backend, and run `npx @rtk-query/codegen-openapi openapi-config.cts` to regenerate BrainFlexApi.ts. Use the generated hooks directly. Never write your own injectEndpoints calls.
