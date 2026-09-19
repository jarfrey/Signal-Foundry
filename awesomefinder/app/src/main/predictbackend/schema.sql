PRAGMA foreign_keys = ON;

-- raws
CREATE TABLE IF NOT EXISTS company (
    company_id INTEGER PRIMARY KEY,
    slug TEXT NOT NULL UNIQUE,
    display_name TEXT,
    ticker TEXT,
    cik TEXT,
    industry TEXT,
    employee_est INTEGER,
    created_at TEXT NOT NULL

);

CREATE TABLE IF NOT EXISTS company_source (
  source        TEXT NOT NULL,
  handle        TEXT NOT NULL,
  company_id    INTEGER NOT NULL REFERENCES company(company_id),
  is_active     INTEGER NOT NULL DEFAULT 1,
  last_ok_at    TEXT,
  last_error    TEXT,
  PRIMARY KEY (source, handle)
);

CREATE TABLE IF NOT EXISTS company_office (
  company_id    INTEGER NOT NULL REFERENCES company(company_id) ON DELETE CASCADE,
  office        TEXT NOT NULL,
  PRIMARY KEY (company_id, office)
);

CREATE TABLE IF NOT EXISTS scrape_run (
  run_id         INTEGER PRIMARY KEY,
  source         TEXT NOT NULL,
  handle         TEXT NOT NULL,
  started_at     TEXT NOT NULL,
  finished_at    TEXT,
  status         TEXT NOT NULL,
  postings_seen  INTEGER,
  postings_new   INTEGER,
  error_msg      TEXT
);

CREATE TABLE IF NOT EXISTS posting (
  posting_id      TEXT PRIMARY KEY,
  source          TEXT NOT NULL,
  handle          TEXT NOT NULL,
  external_id     TEXT NOT NULL,
  title           TEXT,
  location_raw    TEXT,
  department_raw  TEXT, 
  description     TEXT,
  url             TEXT,
  posted_at       TEXT,
  updated_at      TEXT,
  raw_json        TEXT,
  first_seen_at   TEXT NOT NULL,
  last_seen_at    TEXT NOT NULL,
  closed_at       TEXT,
  is_open         INTEGER NOT NULL DEFAULT 1,
  content_hash    TEXT,
  company_id      INTEGER NOT NULL REFERENCES company(company_id),
  first_run_id    INTEGER REFERENCES scrape_run(run_id),
  posted_month    TEXT GENERATED ALWAYS AS (substr(posted_at, 1, 7)) STORED,
  UNIQUE (source, handle, external_id)
);

CREATE INDEX IF NOT EXISTS idx_posting_company_posted ON posting(company_id, posted_at);
CREATE INDEX IF NOT EXISTS idx_posting_open_company ON posting(is_open, company_id);
CREATE INDEX IF NOT EXISTS idx_posting_content_hash ON posting(content_hash);
CREATE INDEX IF NOT EXISTS idx_posting_posted_month ON posting(posted_month);





-- enriched postings (jobs, job types and niches and skills maybe)






-- other signals (markets & articles)





-- candidate data (resume parsing and skills for matching)





-- signals (for later)