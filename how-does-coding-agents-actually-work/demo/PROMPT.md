# Demo prompt

Paste this into both Claude Code and pi:

```
Fix the bugs in app.py and add tests. Run the tests to make sure they pass.
```

Then in pi only, follow up with:

```
review this
```

## Expected bugs found

1. **Off-by-one**: `range(1, len(users))` skips the first user — should be `range(len(users))` or use direct iteration
2. **Regex too restrictive**: `[a-zA-Z0-9]+` doesn't allow `+`, `.`, `-` in local part, or `.` in domain
3. **Truthy check**: `re.match()` returns a Match object or None — works but `bool()` or `is not None` is cleaner

## Expected skill trigger

The "review this" prompt should auto-load the code-review skill and give a structured review of the diff.
