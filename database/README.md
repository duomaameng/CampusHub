# Database setup

This directory stores the local development database scripts for CampusHub.

## Files

- `01-schema.sql`
  - Creates the `campus_hub` database
  - Creates the core tables based on the P3 design

- `02-seed.sql`
  - Inserts admin and student test accounts
  - Inserts sample tasks, applications, orders, notifications, reviews, reports, and announcements
  - Use this for first-time local seed data

- `03-reset-dev-data.sql`
  - Deletes current development seed data
  - Use this before re-running `02-seed.sql` when you want a clean test dataset

## Default test accounts

The seed script inserts 4 accounts. The current shared password is:

`Password123!`

Accounts:

- `admin@smail.nju.edu.cn`
- `cailiyang@smail.nju.edu.cn`
- `wangzikuan@smail.nju.edu.cn`
- `wangshengsheng@smail.nju.edu.cn`

## Manual execution order

1. Run `01-schema.sql`
2. Run `02-seed.sql`

If you need to reset development data:

1. Run `03-reset-dev-data.sql`
2. Run `02-seed.sql` again

## PowerShell helper script

You can use:

- Initialize schema and seed data:
  - `.\scripts\init-db.ps1 -Mode init -Username root -Password your_password`
- Reset development seed data:
  - `.\scripts\init-db.ps1 -Mode reset-seed -Username root -Password your_password`

Usually you do not need to pass `-MySqlExePath` if MySQL is installed in a common location.
