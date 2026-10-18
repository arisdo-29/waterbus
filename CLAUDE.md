# CLAUDE.md

Toàn bộ quy định cho agent nằm trong AGENTS.md (import bên dưới). Đọc và làm theo file đó.

@AGENTS.md

## Nhắc lại luật quan trọng nhất

- Được tự commit, nhưng commit và Pull Request **không được chứa tên agent**: không có dòng
  `Co-Authored-By` của AI, không có dòng "Generated with ...", không có link session.
  File `.claude/settings.json` đã tắt phần ghi công tự động; dù vậy vẫn phải tự kiểm tra message.
- Không sửa `git config user.name`/`user.email`, không push lên `main`/`develop`, không `--no-verify`.
