On slides/elements when setting a background the title/prompt is not clearly legible - need to figure out a way to handle this

When toggling an option as correct the background flickers

Thumbnail image forces the image to abide by ratio, should have it centered, overflow and clip

Backend

I fixed localdate time errors by doing this, need to decide
if the time can just be set on create or on update and if not need to pass in correct value
Instant.parse("2026-05-01T12:00:00Z")
