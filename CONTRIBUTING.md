# Quy trình Git/GitHub của nhóm (4 BE)

Mục tiêu: 4 người code song song mà gần như không bao giờ phải gỡ conflict.
Ba thứ làm được điều đó: **mỗi người sở hữu file riêng**, **mỗi việc 1 nhánh + Pull Request nhỏ**,
và **GitHub chặn mọi thứ đi thẳng vào `main`/`develop`**.

---

## 1. Nhánh

```
main      ← bản ổn định để nộp/demo. Chỉ nhận PR từ develop (Leader merge cuối mỗi sprint).
develop   ← nhánh tích hợp hằng ngày. Mọi người tạo nhánh từ đây và PR về đây.
feature/<be>-<viec>   ← nhánh làm việc của từng người, xoá sau khi merge.
```

Ví dụ tên nhánh: `feature/be1-station-crud`, `feature/be2-trip-search`, `feature/be3-login`,
`feature/be4-create-booking`, `fix/be4-total-amount`.

## 2. Leader: tạo repo lần đầu (làm 1 lần)

1. Trên GitHub: **New repository** → tên `waterbus` → **không** tích "Add README/.gitignore"
   (repo phải trống). Chọn Public hoặc Private (xem lưu ý ở bước 5).
2. Trên máy Leader, trong thư mục project đã giải nén:
   ```bash
   git init -b main
   git config core.hooksPath .githooks
   git add .
   git update-index --chmod=+x mvnw .githooks/commit-msg   # để máy macOS/Linux chạy được
   git commit -m "chore: khoi tao khung project waterbus"
   git remote add origin https://github.com/<tai-khoan>/waterbus.git
   git push -u origin main
   git switch -c develop
   git push -u origin develop
   ```
3. **Settings → General → Default branch** → đổi sang `develop`
   (để PR mặc định đi vào develop, clone về là ở develop).
4. **Settings → General → Pull Requests**:
   - Bỏ tích *Allow merge commits* và *Allow rebase merging*, chỉ giữ **Allow squash merging**
     → mỗi PR thành 1 commit gọn trên develop.
   - Tích **Always suggest updating pull request branches**.
   - Tích **Automatically delete head branches**.
5. **Settings → Rules → Rulesets → New branch ruleset** (hoặc *Branches → Add branch protection rule*):
   - Ruleset name: `protect-main-develop`, Enforcement: **Active**.
   - Target branches: *Add target* → *Include by pattern* → `main`, rồi thêm `develop`.
   - Tích: **Restrict deletions**, **Block force pushes**, **Require linear history**.
   - Tích **Require a pull request before merging** → *Required approvals*: `1`,
     tích *Dismiss stale pull request approvals when new commits are pushed*.
   - Tích **Require status checks to pass** → *Add checks*: `build` và `no-agent-name`
     (2 check này chỉ hiện ra sau khi CI đã chạy ít nhất 1 lần - lần push ở bước 2 đã chạy rồi).
     Tích *Require branches to be up to date before merging*.

   > Lưu ý: branch protection/ruleset miễn phí cho repo **Public**. Repo **Private** cần tài khoản
   > GitHub Pro - sinh viên lấy miễn phí qua **GitHub Student Developer Pack** (education.github.com).
   > Nếu không bật được, cả nhóm vẫn làm đúng quy trình bên dưới, chỉ là GitHub không tự chặn.
6. **Settings → Collaborators → Add people** → mời 3 bạn (quyền Write). Các bạn vào email bấm *Accept*.
7. (Tuỳ chọn) Điền username thật vào `.github/CODEOWNERS` → GitHub tự gắn đúng người review.

## 3. Thành viên: lấy code lần đầu

```bash
git clone https://github.com/<tai-khoan>/waterbus.git
cd waterbus
git config core.hooksPath .githooks        # BẮT BUỘC: bật hook chặn commit có tên AI agent
git config user.name  "Ten That Cua Ban"   # tên hiện trên commit
git config user.email "email-github@cua-ban"
```
Sau đó làm theo [docs/SETUP.md](docs/SETUP.md) để chạy được app.

## 4. Vòng làm việc mỗi ngày

```bash
# 1) Bắt đầu việc mới: luôn đi từ develop mới nhất
git switch develop
git pull
git switch -c feature/be2-trip-search

# 2) Code + commit nhỏ, thường xuyên
git add .
git commit -m "feat(trip): tim chuyen theo ben va ngay"

# 3) Trước khi đẩy lên: lấy code mới của nhóm vào nhánh mình, chạy lại app/test
git pull origin develop
./mvnw -q test            # Windows: mvnw.cmd -q test

# 4) Đẩy nhánh lên và tạo Pull Request vào develop
git push -u origin feature/be2-trip-search
```
Trên GitHub bấm **Compare & pull request** → base: `develop` → điền template → **Create**.
1 bạn khác review → **Approve** → chủ PR bấm **Squash and merge** → xong, nhánh tự xoá.

Trong IntelliJ: *Git → Branches → New Branch*, *Git → Pull*, *Commit (Ctrl+K)*, *Push (Ctrl+Shift+K)*.

## 5. Luật chống conflict

1. **Chỉ sửa file thuộc module của mình** (bảng ở [AGENTS.md](AGENTS.md#3-chia-module-và-quyền-sửa-file)).
   Mỗi module có Controller, Service, ServiceImpl, DTO, thư mục view riêng → 4 người gần như không
   đụng cùng 1 file.
2. **File chung chỉ Leader sửa**: `pom.xml`, `application.properties`, `templates/fragments/layout.html`,
   `HomeController`, `entity/*`, `database/WaterbusDB_SQLServer.sql`, `.github/*`.
   Cần đổi → nhắn nhóm → tạo PR riêng thật nhỏ `chore(shared): ...` → merge sớm → mọi người `git pull origin develop`.
3. **Menu đã có sẵn link của cả 4 module** trong `layout.html`, URL prefix đã chia sẵn → không ai phải sửa layout.
4. **Đổi database**: không sửa file script chính. Tạo file mới `database/changes/2026-10-20_be2_them-cot-x.sql`
   (tên file khác nhau nên không bao giờ conflict) + sửa entity trong cùng PR. Leader gộp vào script chính định kỳ.
5. **Gọi module khác qua Service interface.** Các method ghi "HỢP ĐỒNG" đã có sẵn chữ ký
   (VD `TripService.getAvailableSeats`, `PromotionService.calculateDiscount`) → BE4 code song song được
   dù BE2/BE3 chưa làm xong. Muốn đổi chữ ký → hỏi chủ module.
6. **PR nhỏ, merge sớm**: 1 PR = 1 chức năng, tốt nhất < 300 dòng, không để nhánh sống quá 2-3 ngày.
7. **`git pull origin develop` mỗi sáng** và trước khi push.
8. **Không format lại cả file** của người khác. IntelliJ: khi Commit, nếu bật *Reformat code* thì
   chọn *Only VCS changed text*. `.editorconfig` đã thống nhất thụt lề 4 space, `.gitattributes` thống nhất xuống dòng LF.
9. **Không commit**: `.idea/`, `target/`, `application-local.properties` (đã có trong `.gitignore`).
10. **Không force push, không push thẳng `develop`/`main`.**

## 6. Lỡ bị conflict thì sao

```bash
git pull origin develop      # Git báo CONFLICT ở file X
```
IntelliJ: *Git → Resolve Conflicts…* → chọn file → **Merge** → cột trái là code của bạn,
phải là code develop, giữa là kết quả → bấm mũi tên để lấy từng phần → **Apply**.
Sau đó:
```bash
git add .
git commit -m "chore: merge develop"
./mvnw -q test
git push
```
Không chắc phần nào đúng → gọi chủ file đó cùng xem, đừng xoá code của người khác.

## 7. Commit message

Theo Conventional Commits: `<kieu>(<module>): <mo ta ngan>`

| Kiểu | Khi nào |
|---|---|
| `feat` | thêm chức năng |
| `fix` | sửa lỗi |
| `refactor` | sửa code không đổi chức năng |
| `docs` | tài liệu |
| `test` | thêm/sửa test |
| `chore` | cấu hình, thư viện, merge |
| `style` | giao diện/CSS |

**Không được có tên AI agent trong commit/PR** (kể cả dòng `Co-Authored-By` hay "Generated with ...").
Hook `.githooks/commit-msg` chặn ngay trên máy, và job `no-agent-name` trên GitHub chặn PR.
Chi tiết cho agent: [AGENTS.md](AGENTS.md).
